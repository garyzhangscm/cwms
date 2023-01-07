package com.garyzhangscm.cwms.integration.model;

import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CustomZonedDateTimeDeserializer extends InstantDeserializer<ZonedDateTime> {
    public CustomZonedDateTimeDeserializer() {
        // most parameters are the same used by InstantDeserializer
        super(ZonedDateTime.class,
                DateTimeFormatter.ISO_ZONED_DATE_TIME,
                ZonedDateTime::from,
                // when zone id is "UTC", use the ZoneOffset.UTC constant instead of the zoneId object
                a -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId.getId().equals("UTC") ? ZoneOffset.UTC : a.zoneId),
                // when zone id is "UTC", use the ZoneOffset.UTC constant instead of the zoneId object
                a -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId.getId().equals("UTC") ? ZoneOffset.UTC : a.zoneId),
                // the same is equals to InstantDeserializer
                ZonedDateTime::withZoneSameInstant, false);
    }
}
