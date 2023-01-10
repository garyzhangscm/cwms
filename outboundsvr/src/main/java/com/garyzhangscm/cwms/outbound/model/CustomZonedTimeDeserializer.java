package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class CustomZonedTimeDeserializer extends JsonDeserializer<ZonedDateTime> {
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