package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class ProfilePictureEmptyException extends ApplicationException {

    public ProfilePictureEmptyException() {
        super(
            "Profile picture file must not be empty",
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.PROFILE_PICTURE_EMPTY
        );
    }
}
