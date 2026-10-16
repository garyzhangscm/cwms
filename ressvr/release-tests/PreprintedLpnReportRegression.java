import com.garyzhangscm.cwms.resources.service.ReportService;
import com.garyzhangscm.cwms.resources.model.Report;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
public class PreprintedLpnReportRegression {
 public static void main(String[]args)throws Exception {
  String template=Files.readString(Path.of(args[0]));
  Report report=new Report();List<Map<String,Object>> rows=new ArrayList<>();
  for(int i=1;i<=20;i++)rows.add(Map.of("lpn",String.format("L%09d",i),"batch","LPN-TEST","sequence",i+" / 20"));report.setData(rows);
  Method process=ReportService.class.getDeclaredMethod("processLabel",String.class,Report.class);process.setAccessible(true);
  String labels=(String)process.invoke(new ReportService(),template,report);
  if(labels.split("\\^XA",-1).length!=21 || labels.contains("$lpn$") || labels.contains("DEFECTIVE"))throw new AssertionError("Wrong ordinary batch output");
  for(int i=1;i<=20;i++){String lpn=String.format("L%09d",i);if(labels.split(lpn,-1).length!=3)throw new AssertionError("LPN must appear once in barcode and once in human-readable text");}
  if(!labels.contains("ITEM NUMBER")||!labels.contains("QTY")||!labels.contains("DATE"))throw new AssertionError("Missing write-on spaces");
  System.out.println("PASS: actual Report renderer produces 20 unique ordinary Code128 labels with batch numbers and blank date/item/quantity fields; no stock or printer calls.");
 }
}
