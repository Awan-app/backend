package com.ezdo.exception;

public class OnboardingAlreadyCompletedException extends ApplicationException {

    public OnboardingAlreadyCompletedException() {
        super("User has already completed onboarding.", 409, ErrorCodes.ONBOARDING_ALREADY_COMPLETED);
    }
}
