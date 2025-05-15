package com.garyzhangscm.cwms.resources.model;


public class PrintingJob {

    private Long printingTime;

    private String printerName;

    private String printingJobName;

    private PrintingRequestResult result;

    public PrintingJob(){}


    public PrintingJob(Long printingTime, String printerName, String printingJobName, PrintingRequestResult result) {
        this.printingTime = printingTime;
        this.printerName = printerName;
        this.printingJobName = printingJobName;
        this.result = result;
    }

    public Long getPrintingTime() {
        return printingTime;
    }

    public void setPrintingTime(Long printingTime) {
        this.printingTime = printingTime;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getPrintingJobName() {
        return printingJobName;
    }

    public void setPrintingJobName(String printingJobName) {
        this.printingJobName = printingJobName;
    }

    public PrintingRequestResult getResult() {
        return result;
    }

    public void setResult(PrintingRequestResult result) {
        this.result = result;
    }
}
