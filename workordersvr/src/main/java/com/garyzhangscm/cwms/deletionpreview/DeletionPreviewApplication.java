package com.garyzhangscm.cwms.deletionpreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.deletion.WorkOrderDeletionRepository;
import com.garyzhangscm.cwms.workorder.service.WorkOrderDeletionService;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Additive test entrypoint; the regular WorkOrder application does not scan this package. */
@SpringBootConfiguration
@EnableAutoConfiguration(excludeName = {
    "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
    "org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration"})
@EntityScan("com.garyzhangscm.cwms.workorder.model")
@EnableJpaRepositories(basePackageClasses = WorkOrderDeletionRepository.class)
@Import(WorkOrderDeletionService.class)
public class DeletionPreviewApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DeletionPreviewApplication.class);
        app.setDefaultProperties(Map.of("spring.jpa.hibernate.ddl-auto", "none", "spring.jpa.open-in-view", "false",
                "eureka.client.enabled", "false", "spring.cloud.discovery.enabled", "false"));
        app.run(args);
    }

    @Bean HttpClient deletionHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @RestController
    public static class Endpoint {
        private final WorkOrderDeletionService service;
        private final HttpClient client;
        private final String gateway;
        private final ObjectMapper mapper = new ObjectMapper();
        public Endpoint(WorkOrderDeletionService service, HttpClient client,
                @org.springframework.beans.factory.annotation.Value("${deletion.preview.gateway:http://apigateway:5555}") String gateway) {
            this.service = service; this.client = client; this.gateway = gateway;
        }
        @GetMapping("/healthz") public Map<String,Object> health() { return Map.of("result",0,"data","ready"); }
        @GetMapping("/work-orders/deletion-check")
        public Map<String,Object> check(@RequestParam Long warehouseId, @RequestParam String workOrderIds, HttpServletRequest request) {
            authorize(warehouseId, workOrderIds, request);
            service.check(warehouseId, workOrderIds);
            return Map.of("result",0,"message","Deletion checks passed. No data deleted.","data",Map.of());
        }
        @DeleteMapping("/work-orders")
        public Map<String,Object> delete(@RequestParam Long warehouseId, @RequestParam String workOrderIds, HttpServletRequest request) {
            authorize(warehouseId, workOrderIds, request);
            service.delete(warehouseId, workOrderIds);
            return Map.of("result",0,"message","Work order deleted.","data",Map.of());
        }
        private void authorize(Long warehouseId, String ids, HttpServletRequest request) {
            String authorization = request.getHeader("Authorization");
            String company = request.getHeader("companyId");
            if (company == null) company = request.getParameter("companyId");
            if (authorization == null || !authorization.startsWith("Bearer ") || company == null || !company.matches("[0-9]+"))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please sign in before deleting work orders.");
            for (Long id : WorkOrderDeletionService.parseIds(ids)) {
                try {
                    HttpRequest read = HttpRequest.newBuilder(URI.create(gateway + "/api/workorder/work-orders/" + id))
                            .timeout(Duration.ofSeconds(10)).header("Authorization", authorization).header("companyId", company).GET().build();
                    HttpResponse<String> response = client.send(read, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode()!=200) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Work order access could not be verified.");
                    var body = mapper.readTree(response.body());
                    if (!body.has("result") || body.path("result").asInt(-1)!=0
                            || body.path("data").path("warehouseId").asLong(-1)!=warehouseId)
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Work order access could not be verified.");
                } catch (ResponseStatusException e) { throw e; }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable."); }
                catch (Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Work order access could not be verified."); }
            }
        }
        @ExceptionHandler(WorkOrderException.class)
        public Map<String,Object> rejected(WorkOrderException error) {
            return Map.of("result",55000,"message",error.getData().getOrDefault("error_message", "Deletion not allowed."),"data",Map.of());
        }
    }
}
