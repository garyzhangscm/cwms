package com.garyzhangscm.cwms.outbound.transit;

import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.*;

/** Verify the bearer with Auth, then use that identity rather than a caller-supplied username. */
@Service
public class TransitRuleAccess {
    @Value("${FAY_TRANSIT_AUTH_URL:http://apigateway:5555/api/auth}") private String authUrl;
    @Autowired private WarehouseLayoutServiceRestemplateClient layout;
    @Autowired private ResourceServiceRestemplateClient resource;
    private RestOperations auth = new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(10)).build();
    public String requireAdmin(HttpServletRequest request, Long warehouseId) {
        String authorization=request.getHeader("Authorization");
        if (warehouseId == null || authorization==null || !authorization.startsWith("Bearer ") || authorization.substring(7).trim().isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"MES login required");
        Warehouse warehouse=layout.getWarehouseById(warehouseId);
        if (warehouse==null || warehouse.getCompanyId()==null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Invalid warehouse");
        String actor;
        try {
            HttpHeaders headers=new HttpHeaders();headers.set("Authorization",authorization);headers.set("companyId",warehouse.getCompanyId().toString());headers.set("warehouseId",warehouseId.toString());
            java.net.URI uri=UriComponentsBuilder.fromHttpUrl(authUrl+"/users/username-by-token").queryParam("companyId",warehouse.getCompanyId()).queryParam("token",authorization.substring(7)).build().encode().toUri();
            Map response=auth.exchange(uri,HttpMethod.GET,new HttpEntity<>(headers),Map.class).getBody();
            if (response==null || !Integer.valueOf(0).equals(response.get("result")) || !(response.get("data") instanceof String)) throw new IllegalStateException();
            actor=(String)response.get("data");
            if (actor.trim().isEmpty()) throw new IllegalStateException();
        } catch (RuntimeException error) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Could not verify MES login"); }
        User user=resource.getUserByUsername(warehouse.getCompanyId(),actor);
        if (user==null || !actor.equals(user.getUsername()) || !user.isEnabled() || user.isLocked() ||
                (!Objects.equals(user.getCompanyId(),warehouse.getCompanyId()) && !(Long.valueOf(-1).equals(user.getCompanyId()) && Boolean.TRUE.equals(user.getSystemAdmin())))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Company access required");
        boolean admin=Boolean.TRUE.equals(user.getAdmin()) || Boolean.TRUE.equals(user.getSystemAdmin()) || user.getRoles()!=null && user.getRoles().stream().anyMatch(r -> Boolean.TRUE.equals(r.getEnabled()) && "Admin".equals(r.getName()));
        if (!admin) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator access required for in-transit configuration");
        return actor;
    }
}
