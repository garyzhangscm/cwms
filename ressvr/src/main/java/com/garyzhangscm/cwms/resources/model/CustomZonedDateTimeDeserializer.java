package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
public class CustomZonedDateTimeDeserializer extends InstantDeserializer<ZonedDateTime> {
    public CustomZonedDateTimeDeserializer() {
        // most parameters are the same used by InstantDeserializer
        super(ZonedDateTime.class,
                // DateTimeFormatter.ISO_ZONED_DATE_TIME,
                // DateTimeFormatter.ISO_INSTANT,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                ZonedDateTime::from,
                // when zone id is "UTC", use the ZoneOffset.UTC constant instead of the zoneId object
                a -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId.getId().equals("UTC") ? ZoneOffset.UTC : a.zoneId),
                // when zone id is "UTC", use the ZoneOffset.UTC constant instead of the zoneId object
                a -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId.getId().equals("UTC") ? ZoneOffset.UTC : a.zoneId),
                // the same is equals to InstantDeserializer
                ZonedDateTime::withZoneSameInstant, false);
    }
}
**/
public class CustomZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {
    @Override
    public ZonedDateTime deserialize(JsonParser jsonParser,
                                     DeserializationContext deserializationContext)
            throws IOException {
        Instant instant = Instant.parse(jsonParser.getText());
        // LocalDateTime localDateTime = LocalDateTime.parse(
        //         jsonParser.getText(), DateTimeFormatter.ISO_INSTANT);

        return instant.atZone(ZoneOffset.UTC);
    }
}