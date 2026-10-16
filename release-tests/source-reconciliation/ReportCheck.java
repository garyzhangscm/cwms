import com.fasterxml.jackson.databind.*;
import com.garyzhangscm.cwms.outbound.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.exception.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import com.garyzhangscm.cwms.outbound.reportfix.*;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;

public class ReportCheck {
    static int checks;
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); checks++; }
    static Inventory inventory(String lpn, Long locationId, String locationName, String date) {
        Inventory value = new Inventory(); value.setLpn(lpn); value.setQuantity(10L); value.setLocationId(locationId);
        if (locationName != null) { Location location = new Location(); location.setId(locationId); location.setName(locationName); value.setLocation(location); }
        value.setInWarehouseDatetime(date == null ? null : ZonedDateTime.parse(date)); return value;
    }
    static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    public static void main(String[] args) throws Exception {
        if (System.getSecurityManager() == null) throw new AssertionError("Offline socket restrictions required");
        boolean blocked = false;
        try { System.getSecurityManager().checkPermission(new java.net.SocketPermission("127.0.0.1:9", "connect,resolve")); }
        catch (SecurityException expected) { blocked = true; }
        check(blocked, "Network access must be blocked during report checks");
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("ROOT")).setLevel(ch.qos.logback.classic.Level.OFF);
        Inventory late = inventory("LATE", 1L, "A", "2025-02-03T00:00:00Z");
        Inventory early = inventory("EARLY", 1L, "A", "2024-01-02T00:00:00Z");
        Inventory unknown = inventory("UNDATED", 1L, "A", null);
        List<Inventory> input = Collections.unmodifiableList(Arrays.asList(late, unknown, early));
        OrderLine line = new OrderLine(); line.setItemId(1L); line.setInventoryStatusId(1L); line.setNumber("1");
        line.setupManualPickableInventoryForDisplay(input);
        List<String> rows = line.getManualPickableInventoryForDisplay();
        check(rows.equals(Arrays.asList("A: 01/02/2024 ~ 02/03/2025 (some dates unknown)", "    # EARLY, 10, 01/02/2024", "    # LATE, 10, 02/03/2025", "    # UNDATED, 10, Unknown")), "Mixed dates output");
        check(input.get(0) == late && unknown.getInWarehouseDatetime() == null && early.getQuantity() == 10L, "Input mutated");
        line.setupManualPickableInventoryForDisplay(Arrays.asList(late, early));
        check(line.getManualPickableInventoryForDisplay().equals(Arrays.asList("A: 01/02/2024 ~ 02/03/2025", "    # EARLY, 10, 01/02/2024", "    # LATE, 10, 02/03/2025")), "Known-date format changed");
        Inventory other = inventory("B", 2L, "B", "2023-01-01T00:00:00Z");
        Inventory absent = inventory("C", 3L, null, null);
        line.setupManualPickableInventoryForDisplay(Arrays.asList(absent, late, other, unknown, early));
        rows = line.getManualPickableInventoryForDisplay();
        check(rows.get(0).startsWith("B:") && rows.get(2).startsWith("A:") && rows.get(6).equals("Unknown location (ID 3): Unknown"), "Location groups or unknown dates sort incorrectly");
        Inventory equal = inventory("EQUAL", 1L, "A", "2024-01-02T00:00:00Z");
        line.setupManualPickableInventoryForDisplay(Arrays.asList(equal, early));
        check(line.getManualPickableInventoryForDisplay().get(1).contains("EQUAL"), "Equal-date order changed");
        line.setupManualPickableInventoryForDisplay(Arrays.asList(unknown, absent));
        check(line.getManualPickableInventoryForDisplay().get(0).equals("A: Unknown"), "All unknown dates failed");
        line.setupManualPickableInventoryForDisplay(Collections.emptyList());
        check(line.getManualPickableInventoryForDisplay().isEmpty(), "Empty inventory failed");
        line.setupManualPickableInventoryForDisplay(null);
        check(line.getManualPickableInventoryForDisplay().isEmpty(), "Absent inventory list failed");
        boolean rejected = false;
        try { line.setupManualPickableInventoryForDisplay(Arrays.asList(early, null)); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Invalid inventory silently omitted");

        AtomicReference<Report> report = new AtomicReference<>();
        AtomicReference<List<Inventory>> candidates = new AtomicReference<>(input);
        InventoryServiceRestemplateClient inventoryClient = mock(InventoryServiceRestemplateClient.class, invocation -> {
            if (invocation.getMethod().getName().equals("getPickableInventory")) return candidates.get();
            return RETURNS_DEFAULTS.answer(invocation);
        });
        ResourceServiceRestemplateClient resources = mock(ResourceServiceRestemplateClient.class, invocation -> {
            if (invocation.getMethod().getName().equals("generateReport")) { report.set(invocation.getArgument(2)); return new ReportHistory(); }
            return RETURNS_DEFAULTS.answer(invocation);
        });
        OrderService target = new OrderService();
        inject(target, "inventoryServiceRestemplateClient", inventoryClient);
        inject(target, "resourceServiceRestemplateClient", resources);
        inject(target, "warehouseLayoutServiceRestemplateClient", mock(WarehouseLayoutServiceRestemplateClient.class));
        Order order = new Order(); order.setNumber("SYNTHETIC-REPORT"); order.setWarehouseId(1L); order.setOrderLines(Arrays.asList(line));
        AspectJProxyFactory factory = new AspectJProxyFactory(target); factory.addAspect(new ManualPickReportFailureAspect());
        OrderService service = factory.getProxy();
        check(service.generateManualPickReportByOrder(order, "en-US") != null && report.get() != null, "Real report generation failed");
        verifyNoInteractions(inventoryClient);
        // Empty inventory must still pass an empty list to the existing report renderer.
        candidates.set(Collections.emptyList());
        check(service.generateManualPickReportByOrder(order, "en-US") != null, "Empty real report failed");
        verifyNoInteractions(inventoryClient);
        ResourceServiceRestemplateClient broken = mock(ResourceServiceRestemplateClient.class, invocation -> {
            if (invocation.getMethod().getName().equals("generateReport")) throw new IllegalStateException("private-internal-service-detail");
            return RETURNS_DEFAULTS.answer(invocation);
        });
        inject(target, "resourceServiceRestemplateClient", broken);
        RuntimeException failure = null;
        try { service.generateManualPickReportByOrder(order, "en-US"); } catch (RuntimeException expected) { failure = expected; }
        check(failure instanceof OrderOperationException, "Report failure not translated");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/orders/1/manual-pick-report");
        ResponseBodyWrapper wrapped = new BaseGlobalResponseBodyAdvice().defaultErrorHandler((GenericException)failure, request);
        ObjectMapper plain = new ObjectMapper();
        JsonNode json = plain.readTree(plain.writeValueAsString(wrapped));
        check(json.path("result").asInt() != 0 && json.path("message").asText().contains("Unable to generate the manual pick sheet"), "Readable error missing");
        check(!json.toString().contains("private-internal-service-detail"), "Internal details leaked");
        check(Instant.parse(json.path("data").path("timestamp").asText()) != null, "Timestamp not ISO text");
        ObjectMapper appMapper = new OutboundServerApplication().getObjMapper();
        check(appMapper.readTree(appMapper.writeValueAsString(wrapped)).path("data").path("timestamp").isTextual(), "Application mapper still fails");
        System.out.println("PASS " + checks + " checks: real report path, missing/known/equal dates, missing locations, empty candidates, no input changes, readable failure and timestamp serialization.");
    }
}
