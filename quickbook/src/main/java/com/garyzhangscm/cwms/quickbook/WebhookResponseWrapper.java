package com.garyzhangscm.cwms.quickbook;

public class WebhookResponseWrapper {
    private String message;

    public WebhookResponseWrapper(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
