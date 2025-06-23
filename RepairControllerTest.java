package com.tallercarpro.appTaller.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallercarpro.appTaller.dto.RepairDTO;
import com.tallercarpro.appTaller.exception.ResourceNotFoundException;
import com.tallercarpro.appTaller.service.RepairService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class RepairControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RepairService repairService;

    @InjectMocks
    private RepairController repairController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(repairController)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Para LocalDateTime
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void createRepair_shouldReturnCreatedRepair() throws Exception {
        RepairDTO inputDTO = new RepairDTO(null, 1L, "New Repair", LocalDateTime.now(), null, BigDecimal.valueOf(100), "Mechanic Name", "PENDING");
        RepairDTO outputDTO = new RepairDTO(1L, 1L, "New Repair", LocalDateTime.now(), null, BigDecimal.valueOf(100), "Mechanic Name", "PENDING");
        when(repairService.createRepair(any(RepairDTO.class))).thenReturn(outputDTO);

        mockMvc.perform(post("/api/repairs") // Ajusta la URL del endpoint
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("New Repair"));

        verify(repairService, times(1)).createRepair(any(RepairDTO.class));
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getRepairById_shouldReturnRepairWhenFound() throws Exception {
        RepairDTO repairDTO = new RepairDTO(1L, 1L, "Existing Repair", LocalDateTime.now(), LocalDateTime.now(), BigDecimal.valueOf(150), "Mechanic", "COMPLETED");
        when(repairService.getRepairById(1L)).thenReturn(repairDTO);

        mockMvc.perform(get("/api/repairs/1") // Ajusta la URL del endpoint
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("Existing Repair"));
    }

    // ... otros tests para PUT, DELETE, GET all, error handling, etc. análogos a ClientControllerTest

}