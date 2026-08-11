package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class EmailDeliveryException extends ApplicationException {

    public EmailDeliveryException() {
        super(
            "We could not send the email, please try again",
            HttpStatus.BAD_GATEWAY.value(),
            ErrorCodes.EMAIL_SEND_FAILED
        );
    }

    public EmailDeliveryException(Throwable cause) {
        this();
        initCause(cause);
    }
}
