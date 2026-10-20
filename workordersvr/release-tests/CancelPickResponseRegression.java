import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.RestTemplateProxy;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.model.Pick;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.service.ProductionLineAssignmentService;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.lang.reflect.Field;
import java.util.List;

/** Exercises the actual client and JSON decoding; no network or business writes. */
public class CancelPickResponseRegression {
    static final ObjectMapper mapper = new ObjectMapper();
    static class FakeOutbound extends RestTemplate {
        String json;
        int requests;
        @Override public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
                HttpEntity<?> entity, Class<T> responseType, Object... vars) {
            requests++;
            check(url.equals("http://apigateway:5555/api/outbound/picks/101"), "wrong endpoint");
            check(method == HttpMethod.DELETE && entity == null, "wrong request");
            try { return ResponseEntity.ok(mapper.readValue(json, responseType)); }
            catch (Exception error) { throw new RuntimeException(error); }
        }
    }
    static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static Pick pick(long id, long destination, long quantity, long picked) {
        Pick pick = new Pick(); pick.setId(id); pick.setDestinationLocationId(destination);
        pick.setQuantity(quantity); pick.setPickedQuantity(picked); return pick;
    }
    public static void main(String[] args) throws Exception {
        FakeOutbound http = new FakeOutbound();
        RestTemplateProxy proxy = new RestTemplateProxy();
        set(proxy, "restTemplate", http); set(proxy, "objectMapper", mapper);
        OutboundServiceRestemplateClient client = new OutboundServiceRestemplateClient();
        set(client, "restTemplateProxy", proxy);
        // Cancellation can return no replacement picks, one pick, or multiple picks.
        for (String data : List.of("[]", "[{\"id\":101,\"quantity\":40,\"pickedQuantity\":0}]",
                "[{\"id\":101},{\"id\":102}]")) {
            http.json = "{\"result\":0,\"message\":\"\",\"data\":" + data + "}";
            int before = http.requests;
            Object result = client.cancelPick(101L);
            check(result instanceof List<?>, "response must remain a list");
            List<?> picks = (List<?>) result;
            check(picks.size() == mapper.readTree(data).size(), "pick count changed");
            if (!picks.isEmpty()) check(((Pick)picks.get(0)).getId() == 101L, "pick ID lost");
            if (picks.size() == 1) check(((Pick)picks.get(0)).getQuantity() == 40L, "quantity changed");
            check(http.requests == before + 1, "cancellation unexpectedly retried");
        }
        http.json = "{\"result\":1,\"message\":\"Synthetic cancellation rejected\",\"data\":null}";
        int before = http.requests;
        try { client.cancelPick(101L); throw new AssertionError("backend failure was swallowed"); }
        catch (GenericException expected) { /* caller must retain its uncertain-result handling */ }
        check(http.requests == before + 1, "failed cancellation unexpectedly retried");
        // Exercise the real assignment caller too: changing the return descriptor
        // requires that caller to be rebuilt along with the client.
        OutboundServiceRestemplateClient assignmentClient = new OutboundServiceRestemplateClient() {
            @Override public List<Pick> getWorkOrderPicks(WorkOrder order) {
                return List.of(pick(101, 208, 40, 0), pick(102, 208, 40, 40), pick(103, 209, 40, 0));
            }
        };
        Field proxyField = OutboundServiceRestemplateClient.class.getDeclaredField("restTemplateProxy");
        proxyField.setAccessible(true); proxyField.set(assignmentClient, proxy);
        ProductionLineAssignmentService assignment = new ProductionLineAssignmentService();
        set(assignment, "outboundServiceRestemplateClient", assignmentClient);
        ProductionLine line = new ProductionLine(); line.setInboundStageLocationId(208L);
        var cancel = ProductionLineAssignmentService.class.getDeclaredMethod("cancelPicks", WorkOrder.class, ProductionLine.class);
        cancel.setAccessible(true); http.json = "{\"result\":0,\"message\":\"\",\"data\":[]}";
        before = http.requests;
        cancel.invoke(assignment, new WorkOrder(), line);
        check(http.requests == before + 1, "assignment must cancel only unfinished Picks for this line");
        System.out.println("PASS: empty/single/multiple arrays, IDs/quantities, failure propagation, no retries, and real assignment caller/filtering.");
    }
}
