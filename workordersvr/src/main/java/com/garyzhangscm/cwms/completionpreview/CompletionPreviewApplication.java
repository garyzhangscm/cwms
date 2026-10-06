package com.garyzhangscm.cwms.completionpreview;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.garyzhangscm.cwms.workorder.AuditorAwareImpl;
import com.garyzhangscm.cwms.workorder.clients.KafkaReceiver;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.service.WorkOrderCompleteTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Test-only completion endpoint: no scheduler, Kafka consumers, discovery registration or schema changes. */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages="com.garyzhangscm.cwms.workorder", excludeFilters={
    @ComponentScan.Filter(type=FilterType.ANNOTATION,classes={RestController.class,RestControllerAdvice.class}),
    @ComponentScan.Filter(type=FilterType.REGEX,pattern={"com\\.garyzhangscm\\.cwms\\.workorder\\.WorkOrderServerApplication"}),
    @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,classes=KafkaReceiver.class)
})
@EntityScan("com.garyzhangscm.cwms.workorder.model")
@EnableJpaRepositories({"com.garyzhangscm.cwms.workorder.repository", "com.garyzhangscm.cwms.workorder.deletion"})
@EnableJpaAuditing(auditorAwareRef="auditorAware")
public class CompletionPreviewApplication {
    public static void main(String[] args) { SpringApplication.run(CompletionPreviewApplication.class,args); }
    @Bean public AuditorAware<String> auditorAware(){ return new AuditorAwareImpl(); }
    @Bean @Primary public ObjectMapper getObjMapper(){ return new ObjectMapper()
        .registerModule(new ParameterNamesModule()).registerModule(new Jdk8Module()).registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false); }
    @Bean HttpClient completionHttpClient(){ return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(); }
    @RestController
    public static class Endpoint {
        private final WorkOrderCompleteTransactionService service;
        private final HttpClient client;
        private final String gateway;
        private final ObjectMapper mapper=new ObjectMapper();
        public Endpoint(WorkOrderCompleteTransactionService service,HttpClient client,
            @org.springframework.beans.factory.annotation.Value("${completion.preview.gateway:http://apigateway:5555}") String gateway){
            this.service=service;this.client=client;this.gateway=gateway;
        }
        @GetMapping("/healthz") public Map<String,Object> health(){return Map.of("result",0,"data","ready");}
        @PostMapping("/work-order-complete-transactions") public Map<String,Object> complete(
            @RequestParam Long warehouseId,@RequestParam(required=false) Long locationId,
            @RequestBody WorkOrderCompleteTransaction transaction,HttpServletRequest request){
            if(transaction.getWorkOrder()==null || transaction.getWorkOrder().getId()==null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Work order is required.");
            authorize(warehouseId,transaction.getWorkOrder().getId(),request);
            if(!Objects.equals(warehouseId,transaction.getWorkOrder().getWarehouseId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Work order warehouse does not match.");
            return Map.of("result",0,"data",service.startNewTransaction(warehouseId,transaction,locationId));
        }
        private void authorize(Long warehouseId, Long id, HttpServletRequest request) {
            String authorization = request.getHeader("Authorization");
            String company = request.getHeader("companyId");
            if (company == null) company = request.getParameter("companyId");
            if (authorization == null || !authorization.startsWith("Bearer ") || company == null || !company.matches("[0-9]+"))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please sign in before completing work orders.");
            {
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

        @ExceptionHandler(GenericException.class) public Map<String,Object> rejected(GenericException error){
            return Map.of("result",error.getExceptionCode().getCode(),"message",error.getMessage(),"data",Map.of());
        }
    }
}
