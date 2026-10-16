package com.garyzhangscm.cwms.outbound.reportfix;

import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Keep report failures readable without changing allocation or other report endpoints. */
@Aspect
@Component
public class ManualPickReportFailureAspect {
    private static final Logger LOG = LoggerFactory.getLogger(ManualPickReportFailureAspect.class);

    @Around("execution(public * com.garyzhangscm.cwms.outbound.service.OrderService.generateManualPickReportByOrder(..))")
    public Object generate(ProceedingJoinPoint call) throws Throwable {
        try {
            return call.proceed();
        } catch (RuntimeException failure) {
            LOG.error("Manual pick sheet generation failed", failure);
            // Do not return internal URLs, SQL, exception messages or stack traces to the browser.
            throw OrderOperationException.raiseException(
                    "Unable to generate the manual pick sheet. Please contact support to check the report data or report service.");
        }
    }
}
