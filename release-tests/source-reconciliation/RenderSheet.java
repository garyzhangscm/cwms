import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.nio.file.*;
import java.util.*;

public class RenderSheet {
    static Map<String,?> line(String number,String item,String description,long quantity){
        Map<String,Object> row=new HashMap<>();row.put("number",number);row.put("item.name",item);row.put("item.description",description);
        // The report API deserializes untyped JSON numbers as Integer or Long depending on size.
        if(quantity<=Integer.MAX_VALUE)row.put("openQuantity",Integer.valueOf((int)quantity));else row.put("openQuantity",Long.valueOf(quantity));
        row.put("manualPickableInventoryForDisplay",Arrays.asList("SHOULD-NEVER-BE-PRINTED"));return row;
    }
    static void render(JasperReport template,Path output,List<Map<String,?>> lines)throws Exception {
        Map<String,Object> params=new HashMap<>();params.put("order_number","SAMPLE-ORDER-001");params.put("destination_location","STAGE-A");params.put(JRParameter.REPORT_LOCALE,Locale.US);
        JasperPrint print=JasperFillManager.fillReport(template,params,new JRMapCollectionDataSource(lines));
        JasperExportManager.exportReportToPdfFile(print,output.toString());
        try(PDDocument document=PDDocument.load(output.toFile())){
            String text=new PDFTextStripper().getText(document);
            if(text.contains("SHOULD-NEVER-BE-PRINTED") || text.contains("Source Location") || text.contains("Unknown"))throw new AssertionError("Candidate inventory leaked");
            for(String expected:Arrays.asList("Manual Pick Sheet","SAMPLE-ORDER-001","Shipping Stage","STAGE-A","Description","Qty to Pick"))if(!text.contains(expected))throw new AssertionError("Missing "+expected);
            if(!lines.isEmpty() && !text.contains("42CAN-P"))throw new AssertionError("Missing item");
            if(lines.size()==1 && document.getNumberOfPages()!=1)throw new AssertionError("Single-line report spans pages");
            Files.writeString(output.resolveSibling(output.getFileName()+".txt"),text);
            System.out.println("PASS rendered "+output.getFileName()+": "+document.getNumberOfPages()+" page(s), no candidate inventory.");
        }
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]),output=Path.of(args[1]);Files.createDirectories(output);
        String source=Files.readString(root.resolve("order_manual_pick_sheet.jrxml"));
        if(source.split("<jr:barbecue ",-1).length-1!=1 || !source.contains("<jr:codeExpression><![CDATA[$P{order_number}]]></jr:codeExpression>"))throw new AssertionError("Only the order-number barcode should remain");
        if(source.contains("<jr:codeExpression><![CDATA[$P{destination_location}]]></jr:codeExpression>"))throw new AssertionError("Stage barcode still present");
        JasperReport template=JasperCompileManager.compileReport(root.resolve("order_manual_pick_sheet.jrxml").toString());
        render(template,output.resolve("manual-pick-sheet-preview.pdf"),Arrays.asList(line("1","42CAN-P","42 CAN HARDLINER",40)));
        List<Map<String,?>> many=new ArrayList<>();
        for(int i=1;i<=38;i++)many.add(line(""+i,"42CAN-P",i==1?"Long product description with additional packaging instructions that must wrap fully within the description column without hiding the quantity or overflowing into adjacent rows.":"Sample product description",i));
        render(template,output.resolve("manual-pick-sheet-multipage.pdf"),many);
        render(template,output.resolve("manual-pick-sheet-empty.pdf"),Collections.emptyList());
    }
}
