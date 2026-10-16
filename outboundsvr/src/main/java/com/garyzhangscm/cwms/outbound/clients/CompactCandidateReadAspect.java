package com.garyzhangscm.cwms.outbound.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.allocationwire.CandidateWireCodec;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.model.Inventory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/** Additive hook: preserve the running client, query builder and FIFO implementation. */
@Aspect
@Component
public class CompactCandidateReadAspect {
    private static final Logger log = LoggerFactory.getLogger(CompactCandidateReadAspect.class);
    @Value("${allocation.candidates.compact-enabled:false}") private boolean enabled;
    @Autowired @Lazy private RestTemplateProxy proxy;
    @Autowired @Qualifier("getObjMapper") private ObjectMapper mapper;

    @Around("execution(* com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy.exchangeList(..))")
    public Object read(ProceedingJoinPoint call) throws Throwable {
        Object[] args = call.getArgs();
        if (!enabled || args.length != 4 || args[0] != Inventory.class || args[2] != HttpMethod.GET || args[3] != null) {
            return call.proceed();
        }
        String uri = (String) args[1];
        UriComponents original = UriComponentsBuilder.fromUriString(uri).build(true);
        // Intercept only the allocation candidate request, leaving all other reads alone.
        if (!"/api/inventory/inventories/pickable".equals(original.getPath())
                || !"false".equals(original.getQueryParams().getFirst("includeDetails"))
                || !"500".equals(original.getQueryParams().getFirst("lpnLimit"))) {
            return call.proceed();
        }
        String target = UriComponentsBuilder.fromUriString(uri)
                .replacePath("/api/inventory/inventories/pickable/compact-v1").build(true).toUriString();
        long started = System.nanoTime();
        try {
            JsonNode response = proxy.getRestTemplate().exchange(target, HttpMethod.GET, null, JsonNode.class).getBody();
            if (response == null || !response.path("result").isIntegralNumber()
                    || response.path("result").bigIntegerValue().signum() != 0) {
                throw OrderOperationException.raiseException("Compact inventory response failed validation; allocation stopped.");
            }
            List<Inventory> inventories = new CandidateWireCodec(mapper, Inventory.class).decode(response.get("data"), Inventory.class);
            log.info("Compact allocation candidates read: rows={}, elapsedMs={}", inventories.size(), (System.nanoTime()-started)/1000000);
            return inventories;
        } catch (HttpClientErrorException.NotFound unavailable) {
            log.warn("Compact allocation candidates endpoint unavailable; using legacy read endpoint.");
            return call.proceed();
        } catch (java.io.IOException malformed) {
            throw OrderOperationException.raiseException("Compact inventory data could not be verified; allocation stopped.");
        }
    }
}
