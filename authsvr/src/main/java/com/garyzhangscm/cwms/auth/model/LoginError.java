package com.garyzhangscm.cwms.auth.model;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class to map the error returned from OAuth2 endpoint
 */
public class LoginError {

    @JsonProperty("error")
    String error;
    @JsonProperty("error_description")
    String errorDescription;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
