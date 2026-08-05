package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ProfilePictureTooLargeException extends ApplicationException {

    public ProfilePictureTooLargeException(long actualSize, long maxSize) {
        super(
            "Profile picture exceeds the maximum size of " + (maxSize / (1024 * 1024)) + "MB",
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.PROFILE_PICTURE_TOO_LARGE,
            Map.of("maxSizeBytes", maxSize, "actualSizeBytes", actualSize)
        );
    }
}
