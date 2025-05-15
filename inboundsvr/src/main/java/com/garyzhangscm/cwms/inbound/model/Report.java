package com.garyzhangscm.cwms.inbound.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Report   {

    Collection<?> data;


    Map<String, Object> parameters = new HashMap<>();

    Map<String, Integer> parametersLength = new HashMap<>();


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Collection<?> getData() {
        return data;
    }

    public void setData(Collection<?> data) {
        this.data = data;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    public void addParameter(String name, Object value) {

        this.parameters.put(name, value);
    }

    public Map<String, Integer> getParametersLength() {
        return parametersLength;
    }

    public void setParametersLength(Map<String, Integer> parametersLength) {
        this.parametersLength = parametersLength;
    }
}
