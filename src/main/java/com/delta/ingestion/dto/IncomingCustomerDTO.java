package com.delta.ingestion.dto;

public class IncomingCustomerDTO {

    private String external_id;
    private String name;
    private String email;
    private String country_code;
    private String status_code;

    public IncomingCustomerDTO() {
    }

    public IncomingCustomerDTO(
            String external_id,
            String name,
            String email,
            String country_code,
            String status_code
    ) {
        this.external_id = external_id;
        this.name = name;
        this.email = email;
        this.country_code = country_code;
        this.status_code = status_code;
    }

    public String getExternal_id() {
        return external_id;
    }

    public void setExternal_id(String external_id) {
        this.external_id = external_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public String getStatus_code() {
        return status_code;
    }

    public void setStatus_code(String status_code) {
        this.status_code = status_code;
    }
}
