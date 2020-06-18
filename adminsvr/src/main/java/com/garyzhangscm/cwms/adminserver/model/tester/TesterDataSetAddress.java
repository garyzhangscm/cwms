package com.garyzhangscm.cwms.adminserver.model.tester;

public class TesterDataSetAddress {

    private static TesterDataSetAddress testerDataSetAddress = new TesterDataSetAddress(
            "Jack", "Tester",
            "U.S", "CA", "LA",
            "Hollywood", "",
            "6925 Hollywood Blvd", "", "90028"
    );

    private String contactorFirstname;
    private String contactorLastname;

    private String addressCountry;
    private String addressState;
    private String addressCounty;
    private String addressCity;
    private String addressDistrict;
    private String addressLine1;
    private String addressLine2;
    private String addressPostcode;

    private TesterDataSetAddress(String contactorFirstname,
                                 String contactorLastname,String addressCountry,String addressState,
                                 String addressCounty,String addressCity,String addressDistrict,
                                 String addressLine1,String addressLine2,String addressPostcode) {

        this.contactorFirstname = contactorFirstname;
        this.contactorLastname = contactorLastname;
        this.addressCountry = addressCountry;
        this.addressState = addressState;
        this.addressCounty = addressCounty;
        this.addressCity = addressCity;
        this.addressDistrict = addressDistrict;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressPostcode = addressPostcode;
    }

    public static TesterDataSetAddress getInstance() {
        return testerDataSetAddress;
    }



    public String getContactorFirstname() {
        return contactorFirstname;
    }

    public String getContactorLastname() {
        return contactorLastname;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public String getAddressState() {
        return addressState;
    }

    public String getAddressCounty() {
        return addressCounty;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }
}
