package com.delta.ingestion.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingCustomerDTO {

    private String externalId;
    private String name;
    private String email;
    private String countryCode;
    private String statusCode;
}