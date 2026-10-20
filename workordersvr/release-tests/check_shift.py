"""Compile the candidate's actual validation methods with dependency-only stubs.

No Spring context, service calls, database access or business writes.
"""
from pathlib import Path
import subprocess
import tempfile
import os

root = Path(__file__).resolve().parent
java_bin = Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else None
source = (root.parent / 'src/main/java/com/garyzhangscm/cwms/workorder/service/WorkOrderConfigurationService.java').read_text()
methods = source[source.index('    private void validateShiftSchedule('):source.index('    private void loadProductionShiftSchedule(')]
java = '''import java.time.*;
import java.time.format.*;
import java.util.*;
public class ShiftValidationRegression {
  static class WorkOrderException extends RuntimeException {
    WorkOrderException(String message) { super(message); }
    static WorkOrderException raiseException(String message) { return new WorkOrderException(message); }
  }
  static class ProductionShiftSchedule {
    String start, end; Boolean next;
    ProductionShiftSchedule(String start, String end, Boolean next) { this.start=start; this.end=end; this.next=next; }
    String getShiftStartTime() { return start; }
    String getShiftEndTime() { return end; }
    Boolean getShiftEndNextDay() { return next; }
  }
  static class WorkOrderConfiguration {
    List<ProductionShiftSchedule> shifts;
    WorkOrderConfiguration(ProductionShiftSchedule... shifts) { this.shifts=Arrays.asList(shifts); }
    List<ProductionShiftSchedule> getProductionShiftSchedules() { return shifts; }
  }
''' + methods + '''
  static ProductionShiftSchedule s(String a,String b,Boolean next) { return new ProductionShiftSchedule(a,b,next); }
  static int checks;
  static void verify(boolean valid, ProductionShiftSchedule... shifts) {
    boolean passed=true;
    try { new ShiftValidationRegression().validateShiftSchedule(new WorkOrderConfiguration(shifts)); }
    catch (WorkOrderException error) { passed=false; }
    if (valid!=passed) throw new AssertionError("Unexpected validation result in case " + checks);
    checks++;
  }
  public static void main(String[] args) {
    // Screenshot: the two shifts touch exactly at 08:30, in either row order.
    verify(true,s("08:30:00","16:30:00",false),s("00:30:00","08:30:00",false));
    verify(true,s("00:30:00","08:30:00",false),s("08:30:00","16:30:00",false));
    // Full three-shift cycle, including the following midnight.
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
    verify(true,s("00:30:00","08:30:00",null));
    verify(true);
    // Repeat the exact-boundary case to guard against wall-clock nanos leaking in.
    for(int i=0;i<10000;i++) verify(true,s("08:30:00","16:30:00",false),s("00:30:00","08:30:00",false));
    System.out.println("PASS: " + checks + " checks of actual candidate validation methods; no database/network calls.");
  }
}
'''
with tempfile.TemporaryDirectory(prefix='shift-validator-') as folder:
    path = Path(folder) / 'ShiftValidationRegression.java'
    path.write_text(java)
    subprocess.run([str(java_bin / 'javac') if java_bin else 'javac', str(path)], check=True)
    subprocess.run([str(java_bin / 'java') if java_bin else 'java', '-cp', folder, 'ShiftValidationRegression'], check=True)
