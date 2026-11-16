package com.garyzhangscm.cwms.outbound.loadlink;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
/** All mutations run in the warehouse coordinator transaction. Never ships inventory. */
public class YardVisitStore {
 private final JdbcTemplate db;
 public YardVisitStore(DataSource ds){db=new JdbcTemplate(ds);}
 public static class Request {
  public long revision; public Long trailerId,loadId,dockId;
  public String number,ownership,purpose,carrier,driverName,tractorNumber,notes;
  public boolean physicallyDeparted;
 }
 private void check(boolean ok,String m){if(!ok)throw new IllegalArgumentException(m);}
 private long n(Map<String,Object> r,String k){Object v=r.get(k);return v==null?0:v instanceof Boolean?((Boolean)v?1:0):((Number)v).longValue();}
 private Map<String,Object> one(String sql,Object...a){List<Map<String,Object>> r=db.queryForList(sql,a);check(r.size()==1,"Record not found in this warehouse.");return r.get(0);}
 private long count(String sql,Object...a){return db.queryForObject(sql,Long.class,a);}
 private String text(String v,int max){String s=v==null?"":v.trim();check(s.length()<=max,"A field exceeds its allowed length.");return s;}
 public List<Map<String,Object>> list(long w,boolean history){List<Map<String,Object>> result=db.queryForList("SELECT v.*,t.number trailer_number,l.number load_number,l.status load_status FROM yard_visit v JOIN trailer t ON t.trailer_id=v.trailer_id AND t.warehouse_id=v.warehouse_id LEFT JOIN trailer_appointment l ON l.trailer_appointment_id=v.load_id AND l.warehouse_id=v.warehouse_id WHERE v.warehouse_id=? AND v.state "+(history?"=":"<>")+" 'DISPATCHED' ORDER BY v.visit_id DESC"+(history?" LIMIT 200":""),w);for(Map<String,Object> row:result)for(String key:List.of("checked_in_at","arrived_at","dispatched_at","updated_at")){Object value=row.get(key);if(value instanceof java.time.temporal.TemporalAccessor)row.put(key,value.toString());else if(value instanceof java.sql.Timestamp)row.put(key,((java.sql.Timestamp)value).toLocalDateTime().toString());}return result;}
 private Map<String,Object> visit(long w,long id){return one("SELECT * FROM yard_visit WHERE warehouse_id=? AND visit_id=? FOR UPDATE",w,id);}
 private String json(Map<String,Object> r){try{return new ObjectMapper().findAndRegisterModules().writeValueAsString(r);}catch(Exception e){throw new IllegalStateException("Unable to record Yard operation",e);}}
 private Map<String,Object> audit(long w,long id,String action,String who,Map<String,Object> before){Map<String,Object> after=visit(w,id);db.update("INSERT INTO yard_visit_audit(warehouse_id,visit_id,revision,action,before_json,after_json,actor) VALUES(?,?,?,?,?,?,?)",w,id,n(after,"revision"),action,before==null?null:json(before),json(after),who);return after;}
 public Map<String,Object> checkIn(long w,long company,Request r,String who){
  check(r!=null,"Check-in details are required.");
  check(Set.of("OWN","PARTNER","EXTERNAL").contains(text(r.ownership,24)),"Choose Trailer ownership.");
  check(Set.of("PICKUP","DELIVERY","EMPTY_DROP_OFF","EMPTY_COLLECTION").contains(text(r.purpose,32)),"Choose a visit purpose.");
  String number=text(r.number,50),carrier=text(r.carrier,200),driver=text(r.driverName,100),tractor=text(r.tractorNumber,100),notes=text(r.notes,2000);
  long tid;
  if(r.trailerId!=null){Map<String,Object> t=one("SELECT * FROM trailer WHERE warehouse_id=? AND trailer_id=? FOR UPDATE",w,r.trailerId);check(n(t,"company_id")==company,"Trailer belongs to another company.");tid=r.trailerId;}
  else {
   check(!"OWN".equals(r.ownership),"Select your registered fleet Trailer.");check(!number.isBlank(),"Trailer / container number is required.");
   List<Map<String,Object>> matches=db.queryForList("SELECT * FROM trailer WHERE warehouse_id=? AND LOWER(TRIM(number))=LOWER(?) FOR UPDATE",w,number);
   check(matches.size()<=1,"Duplicate Trailer records exist. Select the correct registered Trailer.");
   if(matches.isEmpty()){
    db.update("INSERT INTO trailer(warehouse_id,company_id,number,description,created_by,created_time,last_modified_by,last_modified_time) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP)",w,company,number,carrier,who,who);
    tid=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
   }else{tid=n(matches.get(0),"trailer_id");check(n(matches.get(0),"company_id")==company,"Trailer belongs to another company.");}
  }
  check(count("SELECT COUNT(*) FROM yard_visit WHERE warehouse_id=? AND active_trailer_id=?",w,tid)==0,"This Trailer is already checked in. Refresh the Yard.");
  check(count("SELECT COUNT(*) FROM load_dock_assignment WHERE warehouse_id=? AND trailer_id=? AND dock_id IS NOT NULL",w,tid)==0,"This Trailer has an existing Dock assignment. Release that assignment before checking in.");
  db.update("INSERT INTO yard_visit(warehouse_id,trailer_id,ownership,purpose,carrier,driver_name,tractor_number,notes,state,active_trailer_id,updated_by) VALUES(?,?,?,?,?,?,?,?,'WAITING',?,?)",w,tid,r.ownership,r.purpose,carrier,driver,tractor,notes,tid,who);
  long id=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);return audit(w,id,"CHECK_IN",who,null);
 }
 private Map<String,Object> load(long w,long id){Map<String,Object> l=one("SELECT * FROM trailer_appointment WHERE warehouse_id=? AND trailer_appointment_id=? FOR UPDATE",w,id);check("SHIPPING".equals(l.get("type")),"Select an outbound Load.");return l;}
 private void planned(Map<String,Object> l){check("PLANNED".equals(l.get("status")),"Only a planned Load can be reassigned.");}
 private void link(long w,Map<String,Object> v,Long target,String who){
  check(target==null||target>0,"Select a valid Load.");long tid=n(v,"trailer_id"),old=n(v,"load_id"),next=target==null?0:target;
  if(old==next)return;
  Map<String,Object> t=one("SELECT * FROM trailer WHERE warehouse_id=? AND trailer_id=? FOR UPDATE",w,tid);
  if(old>0){Map<String,Object> l=load(w,old);planned(l);check(n(l,"trailer_id")==tid&&n(t,"trailer_appointment_id")==old,"Trailer / Load link changed. Refresh before trying again.");}
  if(next>0){
   Map<String,Object> l=load(w,next);planned(l);check(Objects.equals(l.get("company_id"),t.get("company_id")),"Load belongs to another company.");
   check(count("SELECT COUNT(*) FROM yard_visit WHERE warehouse_id=? AND active_load_id=? AND visit_id<>?",w,next,n(v,"visit_id"))==0,"This Load belongs to another checked-in Trailer.");
   check(count("SELECT COUNT(*) FROM load_dock_assignment WHERE warehouse_id=? AND load_id=? AND dock_id IS NOT NULL",w,next)==0,"This Load has an existing Dock assignment.");
   long previous=n(l,"trailer_id");
   if(previous>0&&previous!=tid){
    check(count("SELECT COUNT(*) FROM yard_visit WHERE warehouse_id=? AND active_trailer_id=?",w,previous)==0,"This Load is assigned to another checked-in Trailer.");
    check(count("SELECT COUNT(*) FROM load_dock_assignment WHERE warehouse_id=? AND trailer_id=? AND dock_id IS NOT NULL",w,previous)==0,"The previous Trailer is assigned to a Dock.");
    db.update("UPDATE trailer SET trailer_appointment_id=NULL,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND trailer_id=? AND trailer_appointment_id=?",who,w,previous,next);
   }
  }
  long current=n(t,"trailer_appointment_id");
  if(current>0&&current!=old&&current!=next){Map<String,Object> l=load(w,current);check("COMPLETED".equals(l.get("status")),"This Trailer already has a different active Load. Resolve that Load first.");}
  if(old>0)db.update("UPDATE trailer_appointment SET trailer_id=NULL,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND trailer_appointment_id=? AND trailer_id=?",who,w,old,tid);
  if(next>0)db.update("UPDATE trailer_appointment SET trailer_id=?,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND trailer_appointment_id=?",tid,who,w,next);
  db.update("UPDATE trailer SET trailer_appointment_id=?,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND trailer_id=?",target,who,w,tid);
  db.update("UPDATE yard_visit SET load_id=?,active_load_id=? WHERE warehouse_id=? AND visit_id=?",target,target,w,n(v,"visit_id"));
 }
 private void dock(long w,Map<String,Object> v,Long target,String who){
  long old=n(v,"dock_id"),next=target==null?0:target,id=n(v,"visit_id");String token="YARD-VISIT:"+w+":"+id;
  if(old==next)return;
  TreeSet<Long> ids=new TreeSet<>();if(old>0)ids.add(old);if(next>0)ids.add(next);
  Map<Long,Map<String,Object>> doors=new HashMap<>();
  for(long d:ids)doors.put(d,one("SELECT l.*,g.name group_name,t.dock_locations FROM location l JOIN location_group g ON g.location_group_id=l.location_group_id AND g.warehouse_id=l.warehouse_id JOIN location_group_type t ON t.location_group_type_id=g.location_group_type_id WHERE l.warehouse_id=? AND l.location_id=? FOR UPDATE",w,d));
  if(old>0){check(token.equals(doors.get(old).get("reserved_code")),"Dock reservation changed. Review it before releasing.");check(count("SELECT COUNT(*) FROM inventory WHERE warehouse_id=? AND location_id=? AND quantity>0",w,old)==0,"The Dock still contains inventory. Resolve it before moving or dispatching.");}
  if(next>0){Map<String,Object> d=doors.get(next);check(n(d,"enabled")==1&&n(d,"locked")==0&&n(d,"dock_locations")==1&&"SHIP_DCK".equals(d.get("group_name")),"Select an enabled outbound Dock.");
   check(d.get("reserved_code")==null||d.get("reserved_code").toString().isBlank(),"This Dock is reserved.");
   check(count("SELECT COUNT(*) FROM yard_visit WHERE warehouse_id=? AND dock_id=?",w,next)==0&&count("SELECT COUNT(*) FROM load_dock_assignment WHERE warehouse_id=? AND dock_id=?",w,next)==0,"This Dock is occupied.");
   check(count("SELECT COUNT(*) FROM inventory WHERE warehouse_id=? AND location_id=? AND quantity>0",w,next)==0,"This Dock contains inventory.");
  }
  if(old>0)check(db.update("UPDATE location SET reserved_code='',last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND location_id=? AND reserved_code=?",who,w,old,token)==1,"Dock reservation changed.");
  if(next>0)db.update("UPDATE location SET reserved_code=?,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND location_id=?",token,who,w,next);
  db.update("UPDATE yard_visit SET dock_id=? WHERE warehouse_id=? AND visit_id=?",target,w,id);
 }
 public Map<String,Object> change(long w,long id,String action,Request r,String who){
  check(r!=null,"Refresh before trying again.");Map<String,Object> v=visit(w,id);
  check(n(v,"revision")==r.revision,"This visit changed. Refresh before trying again.");check(!"DISPATCHED".equals(v.get("state")),"This Trailer has already departed.");
  String state=String.valueOf(v.get("state"));
  switch(action){
   case "assign-load": link(w,v,r.loadId,who);break;
   case "assign-dock": check(r.dockId!=null&&r.dockId>0,"Select a Dock.");check(!"AT_DOCK".equals(state),"Return the Trailer to waiting before moving it to another Dock.");dock(w,v,r.dockId,who);state="ASSIGNED";break;
   case "arrive":check("ASSIGNED".equals(state)&&n(v,"dock_id")>0,"Assign a Dock before confirming arrival.");check(count("SELECT COUNT(*) FROM location WHERE warehouse_id=? AND location_id=? AND reserved_code=?",w,n(v,"dock_id"),"YARD-VISIT:"+w+":"+id)==1,"Dock reservation changed.");state="AT_DOCK";db.update("UPDATE yard_visit SET arrived_at=CURRENT_TIMESTAMP(3) WHERE visit_id=?",id);break;
   case "return":check(n(v,"dock_id")>0,"This Trailer is already waiting.");dock(w,v,null,who);state="WAITING";db.update("UPDATE yard_visit SET arrived_at=NULL WHERE visit_id=?",id);break;
   case "dispatch":
    check(r.physicallyDeparted,"Confirm the Trailer has physically departed.");
    Map<String,Object> t=one("SELECT * FROM trailer WHERE warehouse_id=? AND trailer_id=? FOR UPDATE",w,n(v,"trailer_id"));
    long lid=n(v,"load_id"),current=n(t,"trailer_appointment_id");
    check(current==0||current==lid,"Trailer has another Load. Assign or resolve it before dispatching.");
    if(lid>0){Map<String,Object> l=load(w,lid);check("COMPLETED".equals(l.get("status")),"Confirm Loaded on the Load page before dispatching this Trailer.");
     check(n(l,"trailer_id")==n(v,"trailer_id"),"Trailer / Load link changed.");
     check(count("SELECT COUNT(*) FROM stop t JOIN shipment s ON s.stop_id=t.stop_id AND s.warehouse_id=t.warehouse_id WHERE t.warehouse_id=? AND t.trailer_appointment_id=? AND s.status<>'DISPATCHED'",w,lid)==0,"Some Shipments have not shipped. Resolve the Load before dispatching.");
    }
    dock(w,v,null,who);state="DISPATCHED";
    db.update("UPDATE yard_visit SET active_trailer_id=NULL,active_load_id=NULL,dispatched_at=CURRENT_TIMESTAMP(3) WHERE visit_id=?",id);
    db.update("UPDATE trailer SET trailer_appointment_id=NULL,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND trailer_id=? AND trailer_appointment_id=?",who,w,n(v,"trailer_id"),lid);break;
   default:throw new IllegalArgumentException("Unknown Yard action.");
  }
  db.update("UPDATE yard_visit SET state=?,revision=revision+1,updated_by=?,updated_at=CURRENT_TIMESTAMP(3) WHERE warehouse_id=? AND visit_id=?",state,who,w,id);
  return audit(w,id,action.toUpperCase(Locale.ROOT),who,v);
 }
}
