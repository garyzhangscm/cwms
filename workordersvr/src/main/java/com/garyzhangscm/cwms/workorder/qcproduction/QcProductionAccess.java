package com.garyzhangscm.cwms.workorder.qcproduction;
import java.util.Map;
public interface QcProductionAccess {
 String actor(long warehouse);
 Map<String,Object> stock(long warehouse,String lpn);
}
