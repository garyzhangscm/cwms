package com.garyzhangscm.cwms.resources.model;
 

public class ShippingTrailerAppointmentFileUpload extends FileUploadType {

    public ShippingTrailerAppointmentFileUpload(){
        super("shipping-trailer-appointment", "Shipping Trailer Appointment",
                "outbound/trailer-appointments/shipping/upload",
                "resource/assets/file-templates/shipping-trailer-appointments.csv",
                "outbound/trailer-appointments/shipping/upload/progress",
                "outbound/trailer-appointments/shipping/upload/result");
        setupColumns();
    }

    private void setupColumns() {
        addColumn(new FileUploadTemplateColumn(
                "trailer", "Trailer",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                 "number", "Trailer Appointment Number",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "description", "Trailer Appointment Description",
                String.class, 2000, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "stopSequence", "Stop Sequence(Offloading sequence)",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "order", "Order Number",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "line", "Order Line Number",
                String.class, 20, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "carrier", "Carrier",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "serviceLevel", "Service Level",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "driverFirstName", "Driver's First Name",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "driverLastName", "Driver's Last Name",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "driverPhone", "Driver's Phone",
                String.class, 100, false
        ));


    }




}
