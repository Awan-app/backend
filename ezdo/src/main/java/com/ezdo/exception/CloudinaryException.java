package com.ezdo.exception;

public class CloudinaryException extends ApplicationException {

    public CloudinaryException(String message) {
        super(message, 502, ErrorCodes.CLOUDINARY_UPLOAD_FAILED);
    }
}