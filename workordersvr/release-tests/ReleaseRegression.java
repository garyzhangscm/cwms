import java.time.*;
import java.lang.reflect.*;
import java.util.*;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.service.WorkOrderConfigurationService;

/** Instantiate the actual merged service; invoke pure validation only, no Spring/DB. */
public class ReleaseRegression {
    static Method validate, overlap;
    static WorkOrderConfigurationService service=new WorkOrderConfigurationService();
    static int checks;
    static ProductionShiftSchedule s(String start,String end,boolean next) {
        ProductionShiftSchedule row=new ProductionShiftSchedule();
        row.setShiftStartTime(start);row.setShiftEndTime(end);row.setShiftEndNextDay(next);return row;
    }
    static void verify(boolean expected,ProductionShiftSchedule... rows) throws Exception {
        WorkOrderConfiguration config=new WorkOrderConfiguration();
        config.setProductionShiftSchedules(Arrays.asList(rows));
        boolean passed=true;
        try { validate.invoke(service,config); }
        catch(InvocationTargetException error) {
            if (!error.getCause().getClass().getSimpleName().equals("WorkOrderException")) throw error;
            passed=false;
        }
        if(passed!=expected) throw new AssertionError("Validation mismatch in case "+checks);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        validate=service.getClass().getDeclaredMethod("validateShiftSchedule",WorkOrderConfiguration.class);validate.setAccessible(true);
        overlap=service.getClass().getDeclaredMethod("isTimeRangeOverLapping",LocalDateTime.class,LocalDateTime.class,LocalDateTime.class,LocalDateTime.class);overlap.setAccessible(true);
        LocalDate day=LocalDate.of(2000,1,1);
        boolean result=(boolean)overlap.invoke(service,day.atTime(8,30),day.atTime(16,30),day.atTime(0,30),day.atTime(8,30));
        if(args.length>0 && args[0].equals("baseline")) {
            if(!result) throw new AssertionError("Expected original boundary defect");
            System.out.println("PASS: original deployed service reproduces screenshot boundary defect.");return;
        }
        if(result) throw new AssertionError("Boundary defect persists");
        verify(true,s("08:30:00","16:30:00",false),s("00:30:00","08:30:00",false));
        verify(true,s("00:30:00","08:30:00",false),s("08:30:00","16:30:00",false));
        verify(true,s("08:30:00","16:30:00",false),s("16:30:00","00:30:00",true),s("00:30:00","08:30:00",false));
        verify(true,s("04:00:00","16:00:00",false),s("16:00:00","04:00:00",true));
        verify(false,s("08:30:00","16:30:00",false),s("00:30:00","08:30:01",false));
        verify(false,s("08:30:00","16:30:00",false),s("09:00:00","10:00:00",false));
        verify(false,s("08:30:00","16:30:00",false),s("08:30:00","16:30:00",false));
        verify(false,s("16:30:00","00:31:00",true),s("00:30:00","08:30:00",false));
        verify(false,s("00:30:00","08:30:00",false),s("16:30:00","00:31:00",true));
        verify(false,s("08:30:00","08:30:00",false));
        verify(false,s("16:30:00","00:30:00",false));
        verify(false,s("08:30:00","16:30:00",true));
        verify(true,s("08:30:00","08:30:00",true));
        verify(false,s("08:30:00","08:30:00",true),s("09:00:00","10:00:00",false));
        verify(true);
        for(int i=0;i<10000;i++) verify(true,s("08:30:00","16:30:00",false),s("00:30:00","08:30:00",false));
        System.out.println("PASS: "+checks+" checks of actual deployed-class candidate with actual models; no database/network.");
    }
}
