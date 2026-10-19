package com.edinsonamaya.control_deudas.common.error;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas. Verifique el correo y la contraseña.");
    }
}
