package com.garyzhangscm.cwms.outbound.loadlink;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
/** Called inside the warehouse coordinator transaction. No inventory or shipment writes. */
public class DockAssignmentStore {
 private final JdbcTemplate db;
 public DockAssignmentStore(DataSource ds){db=new JdbcTemplate(ds);}
 private void check(boolean b,String m){if(!b)throw new IllegalArgumentException(m);}
 private long n(Map<String,Object> r,String k){Object v=r.get(k);return v==null?0:v instanceof Boolean?((Boolean)v?1:0):((Number)v).longValue();}
 private Map<String,Object> one(String sql,Object...a){List<Map<String,Object>> r=db.queryForList(sql,a);check(r.size()==1,"Record not found in this warehouse.");return r.get(0);}
 public List<Map<String,Object>> list(long w){return db.queryForList("SELECT load_id loadId,trailer_id trailerId,dock_id dockId,revision,updated_by updatedBy,updated_at updatedAt FROM load_dock_assignment WHERE warehouse_id=?",w);}
 public Map<String,Object> assign(long w,long loadId,long trailerId,Long dockId,long revision,String who){
  check(revision>=0&&trailerId>0&&(dockId==null||dockId>0),"Invalid Dock selection.");
  Map<String,Object> load=one("SELECT * FROM trailer_appointment WHERE warehouse_id=? AND trailer_appointment_id=? FOR UPDATE",w,loadId);
  check("SHIPPING".equals(load.get("type")),"Select an outbound Load.");
  check("PLANNED".equals(load.get("status")),"Only a planned Load can change its Dock assignment.");
  Map<String,Object> trailer=one("SELECT * FROM trailer WHERE warehouse_id=? AND trailer_id=? FOR UPDATE",w,trailerId);
  check(n(load,"trailer_id")==trailerId&&n(trailer,"trailer_appointment_id")==loadId&&Objects.equals(load.get("company_id"),trailer.get("company_id")),"The Trailer / Load link changed. Refresh before assigning.");
  List<Map<String,Object>> current=db.queryForList("SELECT * FROM load_dock_assignment WHERE warehouse_id=? AND load_id=? FOR UPDATE",w,loadId);
  long old=current.isEmpty()?0:n(current.get(0),"dock_id");
  long actual=current.isEmpty()?0:n(current.get(0),"revision");
  check(revision==actual,"Dock assignment changed. Refresh before trying again.");
  long target=dockId==null?0:dockId;
  String token="LOAD-DOCK:"+w+":"+loadId;
  TreeSet<Long> ids=new TreeSet<>();if(old>0)ids.add(old);if(target>0)ids.add(target);
  Map<Long,Map<String,Object>> doors=new HashMap<>();
  for(long id:ids)doors.put(id,one("SELECT l.*,g.name group_name,t.dock_locations FROM location l JOIN location_group g ON g.location_group_id=l.location_group_id AND g.warehouse_id=l.warehouse_id JOIN location_group_type t ON t.location_group_type_id=g.location_group_type_id WHERE l.warehouse_id=? AND l.location_id=? FOR UPDATE",w,id));
  if(old>0)check(token.equals(doors.get(old).get("reserved_code")),"Dock reservation changed outside this page. Review it before moving the Trailer.");
  if(target>0){
   Map<String,Object> door=doors.get(target);
   check(n(door,"enabled")==1&&n(door,"locked")==0,"This Dock is disabled or locked.");
   check("SHIP_DCK".equals(door.get("group_name"))&&n(door,"dock_locations")==1,"Select an outbound Dock in this warehouse.");
   Object reserved=door.get("reserved_code");
   check(reserved==null||reserved.toString().isBlank()||(old==target&&token.equals(reserved)),"This Dock is already reserved.");
   check(db.queryForObject("SELECT COUNT(*) FROM load_dock_assignment WHERE warehouse_id=? AND dock_id=? AND load_id<>?",Long.class,w,target,loadId)==0,"This Dock is occupied by another Load.");
   check(db.queryForObject("SELECT COUNT(*) FROM load_dock_assignment WHERE warehouse_id=? AND trailer_id=? AND load_id<>?",Long.class,w,trailerId,loadId)==0,"This Trailer is already assigned to another Load.");
   if(old!=target)check(db.queryForObject("SELECT COUNT(*) FROM inventory WHERE warehouse_id=? AND location_id=? AND quantity>0",Long.class,w,target)==0,"This Dock contains inventory. Review the existing work before assigning.");
  }
  if(old==target)return Map.of("loadId",loadId,"revision",actual);
  if(old>0)check(db.update("UPDATE location SET reserved_code='',last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND location_id=? AND reserved_code=?",who,w,old,token)==1,"Dock reservation changed.");
  if(target>0)db.update("UPDATE location SET reserved_code=?,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND location_id=?",token,who,w,target);
  if(current.isEmpty())db.update("INSERT INTO load_dock_assignment(warehouse_id,load_id,trailer_id,dock_id,revision,updated_by) VALUES(?,?,?,?,?,?)",w,loadId,target==0?null:trailerId,dockId,actual+1,who);
  else db.update("UPDATE load_dock_assignment SET trailer_id=?,dock_id=?,revision=?,updated_by=?,updated_at=CURRENT_TIMESTAMP(3) WHERE warehouse_id=? AND load_id=?",target==0?null:trailerId,dockId,actual+1,who,w,loadId);
  db.update("INSERT INTO load_dock_assignment_audit(warehouse_id,load_id,trailer_id,previous_dock_id,dock_id,revision,actor) VALUES(?,?,?,?,?,?,?)",w,loadId,trailerId,old==0?null:old,dockId,actual+1,who);
  return Map.of("loadId",loadId,"revision",actual+1);
 }
}
