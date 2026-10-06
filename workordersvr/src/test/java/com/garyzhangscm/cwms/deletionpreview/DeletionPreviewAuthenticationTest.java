package com.garyzhangscm.cwms.deletionpreview;
import com.garyzhangscm.cwms.workorder.service.WorkOrderDeletionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked","rawtypes"})
class DeletionPreviewAuthenticationTest {
    WorkOrderDeletionService service;
    HttpClient client;
    HttpServletRequest request;
    HttpResponse<String> response;
    DeletionPreviewApplication.Endpoint endpoint;
    @BeforeEach void setup() throws Exception {
        service=mock(WorkOrderDeletionService.class);client=mock(HttpClient.class);request=mock(HttpServletRequest.class);response=mock(HttpResponse.class);
        endpoint=new DeletionPreviewApplication.Endpoint(service,client,"http://gateway.test");
        when(request.getHeader("Authorization")).thenReturn("Bearer test-token");
        when(request.getHeader("companyId")).thenReturn("1");
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"result\":0,\"data\":{\"warehouseId\":1}}");
    }
    @Test void noLoginNeverReachesDeletion() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertThrows(ResponseStatusException.class,()->endpoint.delete(1L,"1",request));
        verifyNoInteractions(service,client);
    }
    @Test void gatewayDenialAndWarehouseMismatchNeverReachDeletion() {
        when(response.statusCode()).thenReturn(403);
        assertThrows(ResponseStatusException.class,()->endpoint.delete(1L,"1",request));
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"result\":0,\"data\":{\"warehouseId\":2}}");
        assertThrows(ResponseStatusException.class,()->endpoint.delete(1L,"1",request));
        verifyNoInteractions(service);
    }
    @Test void authenticationServiceFailureFailsClosed() throws Exception {
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("offline"));
        assertThrows(ResponseStatusException.class,()->endpoint.delete(1L,"1",request));
        verifyNoInteractions(service);
    }
    @Test void readonlyCheckCannotDeleteAnything() throws Exception {
        assertEquals(0,endpoint.check(1L,"1",request).get("result"));
        verify(service).check(1L,"1");verify(service,never()).delete(anyLong(),anyString());
        verify(client).send(argThat((HttpRequest r)->r.method().equals("GET")),any(HttpResponse.BodyHandler.class));
    }
    @Test void authorizedDeleteUsesTheGuardOnlyAfterReadAuthentication() throws Exception {
        assertEquals(0,endpoint.delete(1L,"1",request).get("result"));
        var order=inOrder(client,service);
        order.verify(client).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        order.verify(service).delete(1L,"1");
    }
}
