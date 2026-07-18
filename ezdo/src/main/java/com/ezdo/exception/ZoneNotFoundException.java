package com.ezdo.exception;

import java.util.Map;
import java.util.UUID;

public class ZoneNotFoundException extends ApplicationException{

    public ZoneNotFoundException(UUID zoneId) {
        super(
                "Zone with id=" + zoneId + " not found",
                404,
                ErrorCodes.ZONE_NOT_FOUND,
                Map.of(
                        "zoneId", zoneId
                )
        );
    }
}
