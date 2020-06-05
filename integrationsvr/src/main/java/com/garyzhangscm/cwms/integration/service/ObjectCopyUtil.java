package com.garyzhangscm.cwms.integration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class ObjectCopyUtil {


    private static final Logger logger = LoggerFactory.getLogger(ObjectCopyUtil.class);

    public static <T, S> void copyValue(T copyFromObject, S copyToObject, String[] fieldNames) {
        Arrays.stream(fieldNames).forEach(fieldname -> {
            // 1st, check if we have this field defined in the from and to object
            Class<T> copyFromClass = (Class<T>) copyFromObject.getClass();
            Class<S> copyToClass = (Class<S>) copyToObject.getClass();

            try {

                Field copyFromField  = copyFromClass.getDeclaredField(fieldname);
                Field copyToField  = copyToClass.getDeclaredField(fieldname);

                // make them public
                copyFromField.setAccessible(true);
                copyToField.setAccessible(true);

                // copy the value
                Object value = copyFromField.get(copyFromObject);
                copyToField.set(copyToObject, value);

            }
            catch (NoSuchFieldException ex) {
                // Ignore and continue with next field
                logger.debug("NoSuchFieldException when copy with field: {}\n exception stack:\n{}",
                        fieldname, ex.getMessage());
            }
            catch (IllegalAccessException ex) {
                logger.debug("IllegalAccessException when copy with field: {}\n exception stack:\n{}",
                        fieldname, ex.getMessage());
            }


        });
    }
}

