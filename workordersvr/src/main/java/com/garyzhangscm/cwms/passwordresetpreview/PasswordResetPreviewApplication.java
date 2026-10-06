package com.garyzhangscm.cwms.passwordresetpreview;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Test-only authenticated facade; no database access or background jobs. */
@SpringBootConfiguration
@EnableAutoConfiguration(excludeName={
 "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
 "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
 "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
 "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
 "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
 "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
 "org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration"})
public class PasswordResetPreviewApplication {
    public static void main(String[] args){SpringApplication.run(PasswordResetPreviewApplication.class,args);}
    @Bean HttpClient resetHttpClient(){return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();}
    public static class ResetRequest {
        public String newPassword;
        public boolean changePasswordAtNextLogon=true;
    }
    @RestController
    public static class Endpoint {
        private final HttpClient client;
        private final String gateway;
        private final ObjectMapper mapper=new ObjectMapper();
        public Endpoint(HttpClient client,@org.springframework.beans.factory.annotation.Value("${password.reset.preview.gateway:http://apigateway:5555}") String gateway){this.client=client;this.gateway=gateway;}
        @GetMapping("/healthz") public Map<String,Object> health(){return Map.of("result",0,"data","ready");}
        @PostMapping("/users/{id}/password-reset") public Map<String,Object> reset(@PathVariable Long id,@RequestParam Long companyId,
                @RequestBody ResetRequest body,HttpServletRequest request){
            String authorization=request.getHeader("Authorization");
            if(authorization==null || !authorization.startsWith("Bearer "))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Please sign in.");
            String password=body.newPassword;
            if(password==null || password.isBlank() || password.length()<8 || password.length()>128 || password.startsWith("{"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The password must contain 8–128 characters and cannot start with {.");
            try {
                // Claims are only used after the same token is verified by the gateway below.
                JsonNode claims=mapper.readTree(Base64.getUrlDecoder().decode(authorization.substring(7).split("\\.")[1]));
                String username=claims.path("sub").asText("");
                long tokenCompany=claims.path("companyId").asLong(Long.MIN_VALUE);
                if(username.isBlank() || companyId==null || companyId<0 || (tokenCompany!=companyId && tokenCompany!=-1))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Company access denied.");
                JsonNode actors=get("/api/resource/users?companyId="+companyId+"&username="+URLEncoder.encode(username,StandardCharsets.UTF_8),authorization,companyId);
                if(!actors.isArray() || actors.size()!=1 || !actors.get(0).path("username").asText().equals(username)
                        || !actors.get(0).path("admin").asBoolean(false)
                        || (actors.get(0).path("companyId").asLong(Long.MIN_VALUE)!=companyId && !actors.get(0).path("systemAdmin").asBoolean(false)))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only an administrator can reset user passwords.");
                JsonNode target=get("/api/resource/users/"+id,authorization,companyId);
                if(target.path("id").asLong(-1)!=id || target.path("companyId").asLong(-1)!=companyId || target.path("systemAdmin").asBoolean(false))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This user cannot be reset from the selected company.");
                ObjectNode change=(ObjectNode)target.deepCopy();
                change.put("password",password);change.put("changePasswordAtNextLogon",body.changePasswordAtNextLogon);
                HttpRequest write=base("/api/resource/users/"+id,authorization,companyId)
                        .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(change))).build();
                checked(client.send(write,HttpResponse.BodyHandlers.ofString()));
                return Map.of("result",0,"data",Map.of());
            } catch(ResponseStatusException e){throw e;}
              catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Password reset service unavailable.");}
              catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Password reset could not be verified. Do not retry automatically.");}
        }
        private HttpRequest.Builder base(String path,String authorization,Long companyId){return HttpRequest.newBuilder(URI.create(gateway+path))
                .timeout(Duration.ofSeconds(15)).header("Authorization",authorization).header("companyId",companyId.toString());}
        private JsonNode get(String path,String authorization,Long companyId)throws Exception {
            return checked(client.send(base(path,authorization,companyId).GET().build(),HttpResponse.BodyHandlers.ofString())).path("data");
        }
        private JsonNode checked(HttpResponse<String> response)throws Exception {
            if(response.statusCode()!=200)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"User access or update was denied.");
            JsonNode data=mapper.readTree(response.body());
            if(data.path("result").asInt(-1)!=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User access or update was rejected.");
            return data;
        }
    }
}
