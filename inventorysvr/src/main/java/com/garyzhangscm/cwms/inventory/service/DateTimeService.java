package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Service
public class DateTimeService {

    private static final Logger logger = LoggerFactory.getLogger(DateTimeService.class);



    public ZonedDateTime getZonedDateTime(String dateString) {

        // YYYY-MM-DD
        Pattern pattern = Pattern.compile("^\\d{2,4}-\\d{1,2}-\\d{1,2}$");
        DateTimeFormatter formatter;
        String dateFormat;

        if(pattern.matcher(dateString).find()) {
            dateFormat = getDateFormat(dateString, "-", new String[]{"y", "M", "d"});
            formatter = DateTimeFormatter.ofPattern(dateFormat);
            try {
                LocalDate.parse(dateString, formatter).atStartOfDay().atZone(ZoneOffset.UTC);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                throw MissingInformationException.raiseException("Can't parse the date " + dateString);
            }
        }

        pattern = Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{2,4}$");
        // MM/DD/YYYY
        // or DD/MM/YYYY
        if(pattern.matcher(dateString).find()) {
            dateFormat = getDateFormat(dateString, "/", new String[]{"M", "d", "y"});

            formatter = DateTimeFormatter.ofPattern(dateFormat);
            try {
                return LocalDate.parse(dateString, formatter).atStartOfDay().atZone(ZoneOffset.UTC);
            }
            catch (Exception ex) {
                // can't parse by MM/DD/YYYY, try DD/MM/YYYY
                ex.printStackTrace();

            }

            dateFormat = getDateFormat(dateString, "/", new String[]{"d", "M", "y"});
            formatter = DateTimeFormatter.ofPattern(dateFormat);
            try {
                return LocalDate.parse(dateString, formatter).atStartOfDay().atZone(ZoneOffset.UTC);
            }
            catch (Exception ex) {
                // can't parse by MM/DD/YYYY, try DD/MM/YYYY
                ex.printStackTrace();
                throw MissingInformationException.raiseException("Can't parse the date " + dateString);

            }
        }
        try {
            return ZonedDateTime.parse(dateString);
        }
        catch (Exception ex) {
            // can't parse by MM/DD/YYYY, try DD/MM/YYYY
            ex.printStackTrace();
            throw MissingInformationException.raiseException("Can't parse the date " + dateString);

        }
    }

    /**
     * Parse the datestring and get the date format in the format of YYYYMMDD, or YYYY-MM-DD or any valid
     * date format so that we can use it to parse the string later on
     * @param dateString
     * @param splitter
     * @param placeHolder 3 element of string array
     * @return
     */
    private String getDateFormat(String dateString, String splitter, String[] placeHolder) {
        logger.debug("start to get date format from string {}, with splitter {}, place holder {}",
                dateString, splitter, placeHolder);
        String[] dateElements = dateString.split(splitter);
        // we are only support 3 element date format, which is Year, Month, Day but can be in any sequence
        if (dateElements.length != 3) {
            throw MissingInformationException.raiseException("Can't parse date " + dateString + ". It is not in the right format");
        }
        String dateFormat = placeHolder[0].repeat(dateElements[0].length()) + splitter +
                placeHolder[1].repeat(dateElements[1].length()) + splitter +
                placeHolder[2].repeat(dateElements[2].length());

        logger.debug("Get format {} out of date string {}",
                dateFormat, dateString);

        return dateFormat;


    }
}
