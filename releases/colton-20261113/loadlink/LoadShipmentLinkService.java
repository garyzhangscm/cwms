package com.garyzhangscm.cwms.outbound.loadlink;

import java.util.*;
import java.sql.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Additive Load linkage service. No JPA orphan-removal collection mutations. */
public final class LoadShipmentLinkService {
  private final JdbcTemplate db;
  private final TransactionTemplate tx;
  private final java.util.function.BiConsumer<Long,Long> readiness;
  public LoadShipmentLinkService(DataSource ds) { this(ds,null); }
  public LoadShipmentLinkService(DataSource ds,java.util.function.BiConsumer<Long,Long> readiness) {
    this(ds,readiness,new DataSourceTransactionManager(ds));
  }
  public LoadShipmentLinkService(DataSource ds,java.util.function.BiConsumer<Long,Long> readiness,org.springframework.transaction.PlatformTransactionManager manager) {
    this.readiness=readiness;
    db=new JdbcTemplate(ds); tx=new TransactionTemplate(manager);
  }
  public void replace(long warehouse,long load,List<Long> expected,List<Long> wanted,String actor) {
    if(actor==null||actor.trim().isEmpty()) throw new IllegalArgumentException("User identity is required.");
    if(wanted==null||expected==null||wanted.size()>500||new HashSet<>(wanted).size()!=wanted.size()||wanted.stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("Shipment IDs must be a unique list (maximum 500).");
    tx.execute(status->{
      List<Map<String,Object>> loads=db.queryForList("SELECT * FROM trailer_appointment WHERE trailer_appointment_id=? AND warehouse_id=? FOR UPDATE",load,warehouse);
      require(loads.size()==1,"Load not found in this warehouse.");
      Map<String,Object> l=loads.get(0);
      require("PLANNED".equals(l.get("status")),"Only a planned Load can change shipments.");
      require("SHIPPING".equals(l.get("type")),"This is not an outbound Load.");
      List<Long> current=db.queryForList("SELECT s.shipment_id FROM shipment s JOIN stop t ON s.stop_id=t.stop_id WHERE t.trailer_appointment_id=? AND t.warehouse_id=? ORDER BY s.shipment_id",Long.class,load,warehouse);
      require(new TreeSet<>(current).equals(new TreeSet<>(expected)),"Load shipment links changed. Reload before saving.");
      TreeSet<Long> lockIds=new TreeSet<>(current);lockIds.addAll(wanted);
      for(Long sid:lockIds) {
        List<Map<String,Object>> ships=db.queryForList("SELECT * FROM shipment WHERE shipment_id=? AND warehouse_id=? FOR UPDATE",sid,warehouse);
        require(ships.size()==1,"Shipment not found in this warehouse.");
        Map<String,Object> s=ships.get(0);Object stop=s.get("stop_id");
        if(stop!=null) {
          List<Long> owners=db.queryForList("SELECT trailer_appointment_id FROM stop WHERE stop_id=? AND warehouse_id=?",Long.class,stop,warehouse);
          require(owners.size()==1&&owners.get(0)!=null&&owners.get(0)==load,"Shipment belongs to another Load or an existing Stop. Remove it there first.");
        }
        if(readiness!=null) { readiness.accept(warehouse,sid); } else {
        require("STAGED".equals(s.get("status")),"Shipment must be staged, with Pick and Deposit complete, before changing its Load.");
        List<Map<String,Object>> lines=db.queryForList("SELECT quantity,open_quantity,inprocess_quantity,loaded_quantity,shipped_quantity FROM shipment_line WHERE shipment_id=? AND warehouse_id=? FOR UPDATE",sid,warehouse);
        require(!lines.isEmpty(),"Shipment has no lines.");
        for(Map<String,Object> line:lines) require(n(line,"quantity")>0&&n(line,"open_quantity")==0&&n(line,"inprocess_quantity")==n(line,"quantity")&&n(line,"loaded_quantity")==0&&n(line,"shipped_quantity")==0,"Shipment quantities are not ready, or loading has started.");
        require(s.get("stage_location_id")!=null,"Shipment has no Shipping Stage.");
        }
      }
      List<Long> stops=db.queryForList("SELECT stop_id FROM stop WHERE trailer_appointment_id=? AND warehouse_id=? ORDER BY stop_sequence,stop_id FOR UPDATE",Long.class,load,warehouse);
      // Never collapse legacy multi-stop loads or move their sibling shipments implicitly.
      require(stops.size()<=1,"This Load has multiple existing Stops. Use the legacy workflow.");
      Long stop=stops.isEmpty()?null:stops.get(0);
      if(stop!=null) {
        String st=db.queryForObject("SELECT status FROM stop WHERE stop_id=?",String.class,stop);
        require("PLANNED".equals(st),"Stop processing has started.");
      }
      if(stop==null&&!wanted.isEmpty()) {
        GeneratedKeyHolder key=new GeneratedKeyHolder();
        db.update(c->{PreparedStatement p=c.prepareStatement("INSERT INTO stop(warehouse_id,trailer_appointment_id,stop_sequence,number,status,created_time,created_by,last_modified_time,last_modified_by) VALUES(?,?,1,?,'PLANNED',CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?)",Statement.RETURN_GENERATED_KEYS);p.setLong(1,warehouse);p.setLong(2,load);p.setString(3,"AUTO-"+load);p.setString(4,actor);p.setString(5,actor);return p;},key);
        stop=key.getKey().longValue();
      }
      for(Long sid:current) if(!wanted.contains(sid)) db.update("UPDATE shipment SET stop_id=NULL,load_number=NULL,last_modified_time=CURRENT_TIMESTAMP,last_modified_by=? WHERE shipment_id=? AND warehouse_id=?",actor,sid,warehouse);
      for(Long sid:wanted) if(!current.contains(sid)) db.update("UPDATE shipment SET stop_id=?,load_number=?,last_modified_time=CURRENT_TIMESTAMP,last_modified_by=? WHERE shipment_id=? AND warehouse_id=?",stop,l.get("number"),actor,sid,warehouse);
      db.update("INSERT INTO load_shipment_link_audit(warehouse_id,load_id,actor,before_ids,after_ids,created_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)",warehouse,load,actor,current.toString(),wanted.toString());
      return null;
    });
  }
  static long n(Map<String,Object> m,String k){return ((Number)m.get(k)).longValue();}
  static void require(boolean ok,String msg){if(!ok)throw new IllegalArgumentException(msg);}
}
