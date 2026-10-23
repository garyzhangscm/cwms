import java.lang.reflect.*;import java.nio.file.*;import java.time.*;import java.util.*;import java.io.*;
/** Exercises actual candidate classes without Spring startup, database, HTTP or Kafka. */
public class UnifiedReadOnlyChecks {
 static Class<?> clazz(String n)throws Exception{return Class.forName(n);}
 static Object make(String n)throws Exception{return clazz(n).getConstructor().newInstance();}
 static void field(Object obj,String name,Object value)throws Exception {Class<?> c=obj.getClass();Field f=null;while(c!=null){try{f=c.getDeclaredField(name);break;}catch(NoSuchFieldException e){c=c.getSuperclass();}}if(f==null)throw new NoSuchFieldException(name);f.setAccessible(true);if(value instanceof Number){if(f.getType()==Integer.class||f.getType()==int.class)value=((Number)value).intValue();else if(f.getType()==Long.class||f.getType()==long.class)value=((Number)value).longValue();}f.set(obj,value);}
 static Object call(Object o,String method,Object...args)throws Exception {Method m=Arrays.stream(o.getClass().getMethods()).filter(v->v.getName().equals(method)&&v.getParameterCount()==args.length).findFirst().orElseThrow();return m.invoke(o,args);}
 static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void verify(String...names)throws Exception {for(String n:names)clazz(n).getDeclaredMethods();}
 static void outbound()throws Exception {
  String p="com.garyzhangscm.cwms.outbound.";verify(p+"service.OrderService",p+"model.OrderLine");
  Object a=make(p+"model.Inventory"),b=make(p+"model.Inventory"),loc=make(p+"model.Location");field(loc,"name","TEST-A");field(loc,"id",1L);
  for(Object v:Arrays.asList(a,b)){field(v,"location",loc);field(v,"locationId",1L);field(v,"quantity",10L);field(v,"inWarehouseDatetime",ZonedDateTime.parse("2026-10-01T00:00:00Z"));}
  Object line=make(p+"model.OrderLine");call(line,"setupManualPickableInventoryForDisplay",new ArrayList<>(Arrays.asList(a,b)));System.out.println("RESULT outbound rows="+call(line,"getManualPickableInventoryForDisplay"));
 }
 static void inventory()throws Exception {
  String p="com.garyzhangscm.cwms.inventory.";verify(p+"service.InventoryService",p+"service.InventoryActivityService",p+"service.QCRuleService");
  Object service=make(p+"service.InventoryService"),item=make(p+"model.Item");field(item,"name","TEST-ITEM");field(item,"description","TEST DESCRIPTION");
  Method legacy=clazz(p+"service.InventoryService").getDeclaredMethod("getLPNLabelContent",String.class,clazz(p+"model.Item"),Long.class);legacy.setAccessible(true);Map<?,?> content=(Map<?,?>)legacy.invoke(service,"TEST-LPN",item,27L);require("TEST-LPN".equals(content.get("lpn")),"Legacy LPN changed");System.out.println("RESULT inventory legacy="+new TreeMap(content));
  Class<?> lookup=clazz(p+"service.RfidItemLookupService");Method variants=lookup.getMethod("variants",String.class);Set<?> codes=(Set<?>)variants.invoke(null,"036000291452");require(codes.contains("036000291452"),"Valid UPC lost");require(codes.contains("00036000291452"),"GTIN padding lost");
  try{variants.invoke(null,"036000291453");throw new AssertionError("Invalid UPC accepted");}catch(InvocationTargetException e){require(e.getCause().getClass().getName().contains("ResponseStatusException"),"Wrong UPC rejection");}
  // Conditional registration evaluates real Spring relaxed property binding; no context refresh or business beans.
  Object context=make("org.springframework.context.annotation.AnnotationConfigApplicationContext");Object env=call(context,"getEnvironment");Object sources=call(env,"getPropertySources");Class<?> mapSource=clazz("org.springframework.core.env.MapPropertySource");Object source=mapSource.getConstructor(String.class,Map.class).newInstance("test-flags",Map.of("mes.rfid.lookup.enabled",System.getProperty("MES_RFID_LOOKUP_ENABLED","false")));sources.getClass().getMethod("addFirst",clazz("org.springframework.core.env.PropertySource")).invoke(sources,source);
  context.getClass().getMethod("register",Class[].class).invoke(context,(Object)new Class<?>[]{clazz(p+"controller.RfidItemLookupController")});boolean registered=(Boolean)call(context,"containsBeanDefinition","rfidItemLookupController");require(registered==Boolean.parseBoolean(System.getProperty("MES_RFID_LOOKUP_ENABLED","false")),"RFID endpoint flag ignored");System.out.println("RESULT RFID variants and conditional registration passed; enabled="+registered);
 }
 static void resource()throws Exception {
  String p="com.garyzhangscm.cwms.resources.";Object runner=make(p+"MyApplicationRunner");Path dir=Files.createTempDirectory("mes-report-config-test");field(runner,"reportTemplateFolder",dir.toString());field(runner,"fileService",make(p+"service.FileService"));
  Method copy=runner.getClass().getDeclaredMethod("copyReportFile",clazz("org.springframework.core.io.Resource"));copy.setAccessible(true);
  for(String file:List.of("order_manual_pick_sheet.jrxml","order_manual_pick_sheet_en_US.properties","order_manual_pick_sheet_zh_CN.properties")){
   Object input=clazz("org.springframework.core.io.ClassPathResource").getConstructor(String.class).newInstance("reports/meta/"+file);copy.invoke(runner,input);String folder=Boolean.parseBoolean(System.getProperty("MES_OUTBOUND_MANUAL_PICK_REPORT_V2","true"))?"reports/meta/":"reports/site-legacy/";
   try(InputStream stream=UnifiedReadOnlyChecks.class.getClassLoader().getResourceAsStream(folder+file)){String expected = new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().collect(java.util.stream.Collectors.joining(System.lineSeparator(),"",System.lineSeparator()));require(Files.readString(dir.resolve(file)).equals(expected),"Wrong site report template "+file);} }
  System.out.println("RESULT all three report resources selected exactly by configuration");
 }
 public static void main(String[] args)throws Exception {switch(args[0]){case "inventory":inventory();break;case "outbound":outbound();break;case "resource":resource();break;default:throw new IllegalArgumentException(args[0]);}}
}
