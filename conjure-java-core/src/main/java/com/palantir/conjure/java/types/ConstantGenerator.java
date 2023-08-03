/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.conjure.java.types;

import com.google.errorprone.annotations.Immutable;
import com.palantir.conjure.java.ConjureAnnotations;
import com.palantir.conjure.java.Options;
import com.palantir.conjure.java.lib.SafeLong;
import com.palantir.conjure.java.util.Packages;
import com.palantir.conjure.spec.ConstantDefinition;
import com.palantir.conjure.spec.TypeName;
import com.palantir.logsafe.Safe;
import com.palantir.tokens.auth.BearerToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

public final class ConstantGenerator {
    private ConstantGenerator() {}

    public static JavaFile generateConstantType(List<ConstantDefinition> typeDefs, Options options) {
        TypeName prefixedTypeName = Packages.getPrefixedName(typeDefs.get(0).getTypeName(), options.packagePrefix());
        ClassName thisClass = ClassName.get(prefixedTypeName.getPackage(), prefixedTypeName.getName());
        return JavaFile.builder(prefixedTypeName.getPackage(), createConstant(typeDefs, thisClass))
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private static TypeSpec createConstant(List<ConstantDefinition> typeDefs, ClassName thisClass) {
        return TypeSpec.classBuilder("constants")
                .addAnnotation(ConjureAnnotations.getConjureGeneratedAnnotation(ConstantGenerator.class))
                .addAnnotation(Safe.class)
                .addAnnotation(Immutable.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(typeDefs.stream()
                        .map(typeDef -> FieldSpec.builder(
                                        convertToClass(typeDef.getType().toString()),
                                        typeDef.getTypeName().getName())
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                                .initializer("$S", typeDef.getValue())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public static Class<?> convertToClass(String className) {
        switch (className) {
            case "STRING":
            case "RID":
                return String.class;
            case "DATETIME":
                return Date.class;
            case "INTEGER":
            case "BINARY":
                return Integer.class;
            case "DOUBLE":
                return Double.class;
            case "SAFELONG":
                return SafeLong.class;
            case "BOOLEAN":
                return Boolean.class;
            case "UUID":
                return UUID.class;
            case "BEARERTOKEN":
                return BearerToken.class;
            default:
                return Object.class;
        }
    }
}
