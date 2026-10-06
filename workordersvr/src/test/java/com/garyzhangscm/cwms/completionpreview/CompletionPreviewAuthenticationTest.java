package com.garyzhangscm.cwms.completionpreview;
import com.garyzhangscm.cwms.workorder.service.WorkOrderCompleteTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked","rawtypes"})
class CompletionPreviewAuthenticationTest {
    WorkOrderCompleteTransactionService service;
    HttpClient client;
    HttpServletRequest request;
    HttpResponse<String> response;
    CompletionPreviewApplication.Endpoint endpoint;
    com.garyzhangscm.cwms.workorder.model.WorkOrderCompleteTransaction transaction;
    @BeforeEach void setup() throws Exception {
        service=mock(WorkOrderCompleteTransactionService.class);client=mock(HttpClient.class);request=mock(HttpServletRequest.class);response=mock(HttpResponse.class);
        endpoint=new CompletionPreviewApplication.Endpoint(service,client,"http://gateway.test");
        transaction=new com.garyzhangscm.cwms.workorder.model.WorkOrderCompleteTransaction();
        var workOrder=new com.garyzhangscm.cwms.workorder.model.WorkOrder();workOrder.setId(259L);workOrder.setWarehouseId(1L);transaction.setWorkOrder(workOrder);
        when(service.startNewTransaction(eq(1L),eq(transaction),nullable(Long.class))).thenReturn(transaction);
        when(request.getHeader("Authorization")).thenReturn("Bearer test-token");
        when(request.getHeader("companyId")).thenReturn("1");
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"result\":0,\"data\":{\"warehouseId\":1}}");
    }
    @Test void noLoginNeverReachesDeletion() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertThrows(ResponseStatusException.class,()->endpoint.complete(1L,null,transaction,request));
        verifyNoInteractions(service,client);
    }
    @Test void gatewayDenialAndWarehouseMismatchNeverReachDeletion() {
        when(response.statusCode()).thenReturn(403);
        assertThrows(ResponseStatusException.class,()->endpoint.complete(1L,null,transaction,request));
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"result\":0,\"data\":{\"warehouseId\":2}}");
        assertThrows(ResponseStatusException.class,()->endpoint.complete(1L,null,transaction,request));
        verifyNoInteractions(service);
    }
    @Test void authenticationServiceFailureFailsClosed() throws Exception {
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("offline"));
        assertThrows(ResponseStatusException.class,()->endpoint.complete(1L,null,transaction,request));
        verifyNoInteractions(service);
    }
    @Test void validCompletionOnlyRunsAfterGatewayAuthorization() throws Exception {
        endpoint.complete(1L,null,transaction,request);
        var order=inOrder(client,service);
        order.verify(client).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        order.verify(service).startNewTransaction(1L,transaction,null);
    }
}
