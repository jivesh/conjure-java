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
import com.palantir.conjure.java.util.Packages;
import com.palantir.conjure.spec.ConstantDefinition;
import com.palantir.conjure.spec.TypeName;
import com.palantir.logsafe.Safe;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

public final class ConstantGenerator {

    private ConstantGenerator() {}

    public static JavaFile generateConstantType(List<ConstantDefinition> typeDefs, Options options) {
        TypeName prefixedTypeName =
                Packages.getPrefixedName(TypeName.of("constant", "package"), options.packagePrefix());
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
                                        typeDef.getType().getClass(),
                                        typeDef.getTypeName().getName())
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                                .initializer("$S", typeDef.getValue())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
