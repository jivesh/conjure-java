/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.conjure.java.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;
import com.palantir.conjure.java.api.errors.RemoteException;
import com.palantir.conjure.java.com.palantir.conjure.verification.server.EndpointName;
import com.palantir.conjure.java.serialization.ObjectMappers;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleParamServicesTest {

    @RegisterExtension
    public static final VerificationServerExtension server = new VerificationServerExtension();

    private static final Logger log = LoggerFactory.getLogger(SingleParamServicesTest.class);
    private static final ObjectMapper objectMapper = ObjectMappers.newClientObjectMapper();
    private static ImmutableMultimap<String, Object> servicesMaps = ImmutableMultimap.<String, Object>builder()
            .putAll(
                    "singlePathParamService",
                    VerificationClients.singlePathParamService(server),
                    VerificationClients.dialogueSinglePathParamService(server))
            .putAll(
                    "singleHeaderService",
                    VerificationClients.singleHeaderService(server),
                    VerificationClients.dialogueSingleHeaderService(server))
            .putAll(
                    "singleQueryParamService",
                    VerificationClients.singleQueryParamService(server),
                    VerificationClients.dialogueSingleQueryParamService(server))
            .build();

    static List<Arguments> testCases() {
        List<Arguments> arguments = new ArrayList<>();

        Cases.TEST_CASES.getSingleHeaderService().forEach((endpointName, singleHeaderTestCases) -> {
            int size = singleHeaderTestCases.size();
            IntStream.range(0, 2).forEach(serviceIndex -> IntStream.range(0, size)
                    .forEach(i -> arguments.add(Arguments.of(
                            "singleHeaderService", serviceIndex, endpointName, i, singleHeaderTestCases.get(i)))));
        });

        Cases.TEST_CASES.getSinglePathParamService().forEach((endpointName, singleHeaderTestCases) -> {
            int size = singleHeaderTestCases.size();
            IntStream.range(0, 2).forEach(serviceIndex -> IntStream.range(0, size)
                    .forEach(i -> arguments.add(Arguments.of(
                            "singlePathParamService", serviceIndex, endpointName, i, singleHeaderTestCases.get(i)))));
        });

        Cases.TEST_CASES.getSingleQueryParamService().forEach((endpointName, singleQueryTestCases) -> {
            int size = singleQueryTestCases.size();
            IntStream.range(0, 2).forEach(serviceIndex -> IntStream.range(0, size)
                    .forEach(i -> arguments.add(Arguments.of(
                            "singleQueryParamService", serviceIndex, endpointName, i, singleQueryTestCases.get(i)))));
        });

        return arguments;
    }

    @ParameterizedTest(name = "{0}-{1}/{2}({4})")
    @MethodSource("testCases")
    public void runTestCase(
            String serviceName, int serviceIndex, EndpointName endpointName, int index, String jsonString)
            throws Exception {
        Assumptions.assumeFalse(Cases.shouldIgnore(endpointName, jsonString));

        System.out.println(String.format("Invoking %s %s(%s)", serviceName, endpointName, jsonString));

        Object service = servicesMaps.get(serviceName).asList().get(serviceIndex);
        for (Method method : servicesMaps.get(serviceName).getClass().getMethods()) {
            String name = method.getName();

            if (method.getParameterCount() == 1) {
                // conjure-java generates `default` methods for optional query params, we don't want to call these
                continue;
            }

            if (endpointName.get().equals(name)) {
                try {
                    // HACKHACK, index parameter order is different for different services.
                    if ("singleHeaderService".equals(serviceName)) {
                        Type type = method.getGenericParameterTypes()[0];
                        Class<?> cls = ClassUtils.getClass(type.getTypeName());
                        method.invoke(service, objectMapper.readValue(jsonString, cls), index);
                    } else {
                        Type type = method.getGenericParameterTypes()[1];
                        Class<?> cls = ClassUtils.getClass(type.getTypeName());
                        method.invoke(service, index, objectMapper.readValue(jsonString, cls));
                    }

                    log.info("Successfully post param to endpoint {} and index {}", endpointName, index);
                } catch (RemoteException e) {
                    log.error("Caught exception with params: {}", e.getError().parameters(), e);
                    throw e;
                }
            }
        }
    }
}
