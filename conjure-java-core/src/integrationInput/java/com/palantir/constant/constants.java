package com.palantir.constant;

import com.google.errorprone.annotations.Immutable;
import com.palantir.conjure.java.lib.SafeLong;
import com.palantir.logsafe.Safe;
import javax.annotation.processing.Generated;

@Generated("com.palantir.conjure.java.types.ConstantGenerator")
@Safe
@Immutable
public final class constants {
    public static final Boolean ConstantBoolean = true;

    public static final Double ConstantDouble = 123.123;

    public static final Integer ConstantInteger = 123;

    public static final SafeLong ConstantSafeLong = SafeLong.of(123123123);

    public static final String ConstantString = "hello";
}