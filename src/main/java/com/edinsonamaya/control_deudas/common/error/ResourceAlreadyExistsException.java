package com.edinsonamaya.control_deudas.common.error;

public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String resource, String field, Object value) {
        super(String.format("Ya existe un %s con %s: '%s'.", resource, field, value));
    }
}
