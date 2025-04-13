package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.WarehouseConfiguration;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class WarehouseConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseConfigurationService.class);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    /**
     * Return the start and end date based on the UTC Time zone, from a date string from
     * @param warehouseId
     * @param date
     * @return
     */
    public Pair<ZonedDateTime, ZonedDateTime> getUTCDateTimeRangeFromWarehouseTimeZone(Long warehouseId, String date) {


        ZonedDateTime begin = getUTCDateBeginTimeFromWarehouseTimeZone(warehouseId, date);
        ZonedDateTime end = begin.plusDays(1).minusNanos(1);

        return Pair.of(begin, end);

    }

    public ZonedDateTime getUTCDateBeginTimeFromWarehouseTimeZone(Long warehouseId, String date) {

        WarehouseConfiguration warehouseConfiguration = warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(warehouseId);
        if (Objects.isNull(warehouseConfiguration) || Strings.isBlank(warehouseConfiguration.getTimeZone())) {
            // if there's no timezone defined for the warehouse, use the server's local
            // time zone

            logger.debug("No time zone defined for warehouse {}, calculate based on the server's time zone",
                    warehouseId);
            LocalDateTime begin = LocalDate.parse(date).atStartOfDay();

            return  begin.atZone(ZoneId.of("UTC"));
        }
        else {
            logger.debug("start to parse {} to UTC date time, based on current time zone {}",
                    date, warehouseConfiguration.getTimeZone());

            // the date should be in the format of YYYY-MM-DD
            LocalDate localDate = LocalDate.parse(date);

            ZonedDateTime begin = ZonedDateTime.of(localDate.getYear(), localDate.getMonthValue(),
                    localDate.getDayOfMonth(), 0, 0, 0, 0,
                    ZoneId.of(warehouseConfiguration.getTimeZone()));

            return begin.withZoneSameInstant(ZoneId.of("UTC"));

        }
    }

    public ZonedDateTime getUTCDateEndTimeFromWarehouseTimeZone(Long warehouseId, String date) {

        ZonedDateTime begin = getUTCDateBeginTimeFromWarehouseTimeZone(warehouseId, date);
        return begin.plusDays(1).minusNanos(1);
    }

    public ZonedDateTime getUTCDateTimeFromWarehouseTimeZone(Long warehouseId, String dateTime) {

        WarehouseConfiguration warehouseConfiguration = warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(warehouseId);
        if (Objects.isNull(warehouseConfiguration) || Strings.isBlank(warehouseConfiguration.getTimeZone())) {
            // if there's no timezone defined for the warehouse, use the server's local
            // time zone

            logger.debug("No time zone defined for warehouse {}, calculate based on the server's time zone",
                    warehouseId);
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime);

            return  localDateTime.atZone(ZoneId.of("UTC"));
        }
        else {
            logger.debug("start to parse {} to UTC date time, based on current time zone {}",
                    dateTime, warehouseConfiguration.getTimeZone());

            // the date should be in the format of YYYY-MM-DD
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime);

            ZonedDateTime begin = ZonedDateTime.of(localDateTime.getYear(), localDateTime.getMonthValue(),
                    localDateTime.getDayOfMonth(), localDateTime.getHour(), localDateTime.getMinute(),
                    localDateTime.getSecond(), localDateTime.getNano(),
                    ZoneId.of(warehouseConfiguration.getTimeZone()));

            return begin.withZoneSameInstant(ZoneId.of("UTC"));

        }
    }

}
