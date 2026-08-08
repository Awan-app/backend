package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

public class DailyGiftAlreadyClaimedException extends ApplicationException {

    public DailyGiftAlreadyClaimedException(LocalDate claimDate) {
        super(
            "Daily gift already claimed for this day.",
            HttpStatus.CONFLICT.value(),
            ErrorCodes.DAILY_GIFT_ALREADY_CLAIMED,
            Map.of("claimDate", claimDate)
        );
    }
}
