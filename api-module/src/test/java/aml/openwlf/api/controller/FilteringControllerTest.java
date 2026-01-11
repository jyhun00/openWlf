package aml.openwlf.api.controller;

import aml.openwlf.api.dto.CustomerFilterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilteringControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/filter/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Watchlist Filtering Service is operational"));
    }
    
    @Test
    void testFilterCustomer_NoMatch() throws Exception {
        // Use a completely unique name that won't match any watchlist entry
        CustomerFilterRequest request = CustomerFilterRequest.builder()
                .name("Xyzabc Qwerty Testname")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .nationality("AU")
                .customerId("TEST-001")
                .build();

        mockMvc.perform(post("/api/filter/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alert").value(false))
                .andExpect(jsonPath("$.score").exists())
                .andExpect(jsonPath("$.explanation").exists());
    }
    
    @Test
    void testFilterCustomer_Match() throws Exception {
        CustomerFilterRequest request = CustomerFilterRequest.builder()
                .name("John Smith")
                .dateOfBirth(LocalDate.of(1975, 5, 15))
                .nationality("US")
                .customerId("TEST-002")
                .build();
        
        mockMvc.perform(post("/api/filter/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").exists())
                .andExpect(jsonPath("$.matchedRules").isArray())
                .andExpect(jsonPath("$.explanation").exists());
    }
    
    @Test
    void testFilterCustomer_InvalidRequest() throws Exception {
        CustomerFilterRequest request = CustomerFilterRequest.builder()
                .name("") // Empty name
                .build();
        
        mockMvc.perform(post("/api/filter/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
