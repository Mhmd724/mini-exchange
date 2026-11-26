package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;

public record OwnerId(long value) {

    public OwnerId {
        Validation.requirePositive(value, "Owner id");
    }
}
