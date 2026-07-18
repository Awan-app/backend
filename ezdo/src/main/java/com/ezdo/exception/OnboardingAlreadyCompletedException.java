package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class OnboardingAlreadyCompletedException extends ApplicationException {

    public OnboardingAlreadyCompletedException() {
        super(
            "User has already completed onboarding.",
            HttpStatus.CONFLICT.value(),
            ErrorCodes.ONBOARDING_ALREADY_COMPLETED
        );
    }
}
