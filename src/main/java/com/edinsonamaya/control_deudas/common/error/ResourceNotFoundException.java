package com.edinsonamaya.control_deudas.common.error;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(String.format("%s con identificador '%s' no encontrado.", resource, identifier));
    }
}
