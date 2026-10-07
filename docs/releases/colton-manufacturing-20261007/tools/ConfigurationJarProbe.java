import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.WarehouseConfigurationRepository;
import com.garyzhangscm.cwms.layout.service.WarehouseConfigurationService;
import java.lang.reflect.*;
public class ConfigurationJarProbe {
    public static void main(String[] args)throws Exception{
        WarehouseConfiguration old=new WarehouseConfiguration();old.setId(5L);old.setManufacturingIssueRequireSourceLocation(false);old.setManufacturingIssueRequireAllocatedLpn(false);
        WarehouseConfigurationRepository repository=(WarehouseConfigurationRepository)Proxy.newProxyInstance(WarehouseConfigurationRepository.class.getClassLoader(),new Class[]{WarehouseConfigurationRepository.class},(p,m,a)->m.getName().equals("findByWarehouse")?old:a[0]);
        WarehouseConfigurationService service=new WarehouseConfigurationService();Field field=WarehouseConfigurationService.class.getDeclaredField("warehouseConfigurationRepository");field.setAccessible(true);field.set(service,repository);
        Warehouse warehouse=new Warehouse();warehouse.setId(1L);WarehouseConfiguration incoming=new WarehouseConfiguration();incoming.setWarehouse(warehouse);service.saveOrUpdate(incoming);
        if(!Long.valueOf(5).equals(incoming.getId()) || !Boolean.FALSE.equals(incoming.getManufacturingIssueRequireSourceLocation()) || !Boolean.FALSE.equals(incoming.getManufacturingIssueRequireAllocatedLpn()))throw new AssertionError("legacy save reset choices");
        incoming.setManufacturingIssueRequireSourceLocation(true);service.saveOrUpdate(incoming);if(!incoming.getManufacturingIssueRequireSourceLocation())throw new AssertionError("explicit choice lost");
        System.out.println("Actual release JAR: legacy client preservation and explicit configuration choice passed; no network or database used.");
    }
}
