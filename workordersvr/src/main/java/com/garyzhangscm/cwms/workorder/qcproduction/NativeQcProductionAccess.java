package com.garyzhangscm.cwms.workorder.qcproduction;
import static com.garyzhangscm.cwms.workorder.qcproduction.QcProductionStore.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.garyzhangscm.cwms.workorder.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
@Service
public class NativeQcProductionAccess implements QcProductionAccess {
 @Autowired HttpServletRequest request;
 String gateway="http://apigateway:5555";
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 private final ObjectMapper mapper=new ObjectMapper();
 @Autowired WarehouseLayoutServiceRestemplateClient layout;
 @Autowired InventoryServiceRestemplateClient inventory;
 public String actor(long warehouse){
  require(warehouse==1,"QC production preview is enabled only for warehouse 1");
  var wh=layout.getWarehouseById(warehouse);require(wh!=null,"Warehouse not found");
  var user=verifiedUser(wh.getCompanyId());require(user!=null&&user.isEnabled()&&!user.isLocked(),"Sign in with an active QC account");
  require((Objects.equals(user.getCompanyId(),wh.getCompanyId())||(Boolean.TRUE.equals(user.getSystemAdmin())&&Objects.equals(user.getCompanyId(),-1L))),"User belongs to another company");
  boolean admin=Boolean.TRUE.equals(user.getAdmin())||Boolean.TRUE.equals(user.getSystemAdmin());
  boolean qc=user.getRoles()!=null&&user.getRoles().stream().anyMatch(r->Boolean.TRUE.equals(r.getEnabled())&&"QC".equals(r.getName()));
  require(admin||qc,"QC role is required for production confirmation and withdrawal");return user.getUsername();
 }
 protected User verifiedUser(Long companyId){
  String auth=request.getHeader("Authorization");require(auth!=null&&auth.startsWith("Bearer "),"Sign in with a QC account");
  try{
   var claims=mapper.readTree(Base64.getUrlDecoder().decode(auth.substring(7).split("\\.")[1]));String name=claims.path("sub").asText("");long company=claims.path("companyId").asLong(Long.MIN_VALUE);
   require(!name.isBlank()&&(company==companyId||company==-1),"Company access denied");
   // Gateway verifies the ORIGINAL token. Never forward a caller-provided username header.
   var req=HttpRequest.newBuilder(URI.create(gateway+"/api/resource/users?companyId="+companyId+"&username="+URLEncoder.encode(name,StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(15)).header("Authorization",auth).header("companyId",companyId.toString()).GET().build();
   var response=http.send(req,HttpResponse.BodyHandlers.ofString());require(response.statusCode()==200,"Login could not be verified");var payload=mapper.readTree(response.body());require(payload.path("result").asInt(-1)==0,"Login could not be verified");var users=payload.path("data");require(users.isArray()&&users.size()==1&&name.equals(users.get(0).path("username").asText()),"QC account could not be verified");return mapper.treeToValue(users.get(0),User.class);
  }catch(IllegalArgumentException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalArgumentException("Login verification interrupted");}catch(Exception e){throw new IllegalArgumentException("Login could not be verified; sign in again");}
 }
 public Map<String,Object> stock(long warehouse,String lpn){
  var rows=inventory.findInventoryByLPN(warehouse,lpn);var orders=new HashSet<Long>();boolean found=false;
  for(var row:rows){if(!Objects.equals(lpn,row.getLpn())||!Objects.equals(warehouse,row.getWarehouseId())||row.getQuantity()==null||row.getQuantity()<=0)continue;found=true;if(row.getWorkOrderId()!=null)orders.add(row.getWorkOrderId());}
  return Map.of("exists",found,"orderIds",orders);
 }
}
