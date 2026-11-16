package com.garyzhangscm.cwms.inventory.controller;
import java.io.*;import java.lang.reflect.Type;import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.*;
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
/** Keep legacy model defaults for integrations, while requiring explicit maintenance input. */
@ControllerAdvice
public class ItemCreateInputAdvice extends RequestBodyAdviceAdapter {
 public boolean supports(MethodParameter p,Type type,Class<? extends HttpMessageConverter<?>> converter){return p.getDeclaringClass()==ItemController.class && p.getMethod()!=null && p.getMethod().getName().equals("addItem");}
 public HttpInputMessage beforeBodyRead(HttpInputMessage input,MethodParameter parameter,Type target,Class<? extends HttpMessageConverter<?>> converter)throws IOException {
  byte[] bytes=input.getBody().readAllBytes();JsonNode tree=new ObjectMapper().readTree(bytes);validate(tree);
  return new HttpInputMessage(){public InputStream getBody(){return new ByteArrayInputStream(bytes);} public HttpHeaders getHeaders(){return input.getHeaders();}};
 }
 public static void validate(JsonNode tree) {
  if(tree==null || !tree.isObject())fail("Item input is required");
  JsonNode cost=tree.get("unitCost"),days=tree.get("shelfLifeDays");
  if(cost==null || !cost.isNumber() || !Double.isFinite(cost.doubleValue()) || cost.doubleValue()<0)fail("Unit cost must be entered as a nonnegative number");
  if(days==null || !days.isIntegralNumber() || !days.canConvertToInt() || days.intValue()<0)fail("Shelf life days must be entered as a nonnegative whole number");
 }
 private static void fail(String message){throw RequestValidationFailException.raiseException(message);}
}
