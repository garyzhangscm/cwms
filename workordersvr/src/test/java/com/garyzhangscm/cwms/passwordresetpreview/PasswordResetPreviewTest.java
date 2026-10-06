package com.garyzhangscm.cwms.passwordresetpreview;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SuppressWarnings({"unchecked","rawtypes"})
class PasswordResetPreviewTest {
    HttpClient client;HttpServletRequest request;PasswordResetPreviewApplication.Endpoint endpoint;
    PasswordResetPreviewApplication.ResetRequest body;
    @BeforeEach void setup(){
        client=mock(HttpClient.class);request=mock(HttpServletRequest.class);
        endpoint=new PasswordResetPreviewApplication.Endpoint(client,"http://gateway.test");
        String claims=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"admin\",\"companyId\":1}".getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Bearer header."+claims+".signature");
        body=new PasswordResetPreviewApplication.ResetRequest();body.newPassword="test-only-password";
    }
    HttpResponse<String> response(int status,String data){return new HttpResponse<String>(){
        public int statusCode(){return status;} public String body(){return data;}
        public HttpRequest request(){return null;}
        public java.util.Optional<HttpResponse<String>> previousResponse(){return java.util.Optional.empty();}
        public HttpHeaders headers(){return HttpHeaders.of(java.util.Map.of(),(a,b)->true);}
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession(){return java.util.Optional.empty();}
        public java.net.URI uri(){return java.net.URI.create("http://gateway.test");}
        public HttpClient.Version version(){return HttpClient.Version.HTTP_1_1;}
    };}
    @Test void noLoginOrInvalidPasswordNeverWrites(){
        when(request.getHeader("Authorization")).thenReturn(null);
        assertThrows(ResponseStatusException.class,()->endpoint.reset(10L,1L,body,request));verifyNoInteractions(client);
    }
    @Test void rejectedTokenAndNonAdminNeverWrites()throws Exception {
        when(client.send(any(),any(HttpResponse.BodyHandler.class))).thenReturn(response(403,"{}"));
        assertThrows(ResponseStatusException.class,()->endpoint.reset(10L,1L,body,request));
        when(client.send(any(),any(HttpResponse.BodyHandler.class))).thenReturn(response(200,"{\"result\":0,\"data\":[{\"username\":\"admin\",\"admin\":false,\"companyId\":1}]}"));
        assertThrows(ResponseStatusException.class,()->endpoint.reset(10L,1L,body,request));
        verify(client,never()).send(argThat((HttpRequest r)->r.method().equals("POST")),any(HttpResponse.BodyHandler.class));
    }
    @Test void crossCompanyTargetNeverWrites()throws Exception {
        when(client.send(any(),any(HttpResponse.BodyHandler.class))).thenReturn(
            response(200,"{\"result\":0,\"data\":[{\"username\":\"admin\",\"admin\":true,\"companyId\":1}]}"),
            response(200,"{\"result\":0,\"data\":{\"id\":10,\"companyId\":2}}"));
        assertThrows(ResponseStatusException.class,()->endpoint.reset(10L,1L,body,request));
        verify(client,never()).send(argThat((HttpRequest r)->r.method().equals("POST")),any(HttpResponse.BodyHandler.class));
    }
    @Test void validResetUsesBodyAndPreservesUserFields()throws Exception {
        when(client.send(any(),any(HttpResponse.BodyHandler.class))).thenReturn(
            response(200,"{\"result\":0,\"data\":[{\"username\":\"admin\",\"admin\":true,\"companyId\":1}]}"),
            response(200,"{\"result\":0,\"data\":{\"id\":10,\"companyId\":1,\"username\":\"target\",\"enabled\":true,\"roles\":[]}}"),
            response(200,"{\"result\":0,\"data\":{}}"));
        assertEquals(0,endpoint.reset(10L,1L,body,request).get("result"));
        verify(client).send(argThat((HttpRequest r)->r.method().equals("POST") && !r.uri().toString().contains("password") && r.bodyPublisher().isPresent()),any(HttpResponse.BodyHandler.class));
    }
}
