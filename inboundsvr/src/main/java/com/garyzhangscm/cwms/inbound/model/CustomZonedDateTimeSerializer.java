package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CustomZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {

    private static final Logger logger = LoggerFactory.getLogger(CustomZonedDateTimeSerializer.class);
    @Override
    public void serialize(ZonedDateTime zonedDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        // logger.debug("start to serial zoned date time {}", zonedDateTime);

        String result = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
                .format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
        // logger.debug("==> result: {}", result);
        jsonGenerator.writeString(result);

    }
}