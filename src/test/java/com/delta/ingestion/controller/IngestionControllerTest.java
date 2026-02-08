package com.delta.ingestion.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIngestCustomers() throws Exception {

        String payload = """
        [
          {
            "external_id": "cust_10",
            "name": "Test",
            "email": "test@test.com",
            "country_code": "US",
            "status_code": "ACTIVE"
          }
        ]
        """;

        mockMvc.perform(
                        post("/customers/ingest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").value(1));
    }
}
