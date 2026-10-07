import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import com.garyzhangscm.cwms.outbound.service.ManufacturingIssuePolicyService;
import java.lang.reflect.Proxy;
import java.util.*;

public class PolicyJarProbe {
    static WarehouseConfiguration config=new WarehouseConfiguration();
    static List<Pick> originals=new ArrayList<>();
    static int queries=0,checks=0;
    public static class Client extends WarehouseLayoutServiceRestemplateClient {
        @Override public WarehouseConfiguration getManufacturingIssueConfiguration(Long id){return config;}
    }
    static Pick pick(Long source,String lpn){Pick p=new Pick();p.setSourceLocationId(source);p.setLpn(lpn);return p;}
    static void check(ManufacturingIssuePolicyService service,Long source,String lpn,boolean allowed){
        boolean result=true;try{service.validate(1L,2L,3L,4L,source,lpn);}catch(RuntimeException e){result=false;}
        if(result!=allowed)throw new AssertionError("unexpected policy result");checks++;
    }
    public static void main(String[] args){
        PickRepository repo=(PickRepository)Proxy.newProxyInstance(PickRepository.class.getClassLoader(),new Class[]{PickRepository.class},(p,m,a)->{
            if(!m.getName().equals("findOpenWorkOrderPicksForIssue") || !Arrays.equals(a,new Object[]{1L,2L,3L,4L})) throw new AssertionError("unexpected query");queries++;return originals;
        });
        ManufacturingIssuePolicyService service=new ManufacturingIssuePolicyService(new Client(),repo);
        originals.add(pick(10L,"L1"));
        check(service,10L,"L1",true);check(service,11L,"L1",false);check(service,10L,"L2",false);
        config.setManufacturingIssueRequireAllocatedLpn(false);check(service,10L,"L2",true);check(service,11L,"L1",false);
        config.setManufacturingIssueRequireSourceLocation(false);int before=queries;check(service,11L,"L2",true);if(queries!=before)throw new AssertionError("unnecessary allocation read");
        config.setManufacturingIssueRequireAllocatedLpn(true);check(service,11L,"L1",true);check(service,10L,"L2",false);
        config.setManufacturingIssueRequireSourceLocation(true);originals.add(pick(11L,"L2"));check(service,10L,"L2",false);
        originals.clear();originals.add(pick(10L,null));check(service,10L,"L2",true);check(service,11L,"L2",false);
        originals.clear();check(service,11L,"L2",true);
        System.out.println("Actual release JAR: "+checks+" policy checks passed; no network or database used.");
    }
}
