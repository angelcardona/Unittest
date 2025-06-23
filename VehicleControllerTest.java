package com.tallercarpro.appTaller.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallercarpro.appTaller.dto.VehicleDTO;
import com.tallercarpro.appTaller.exception.ResourceNotFoundException;
import com.tallercarpro.appTaller.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class VehicleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleController vehicleController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleController)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void createVehicle_shouldReturnCreatedVehicle() throws Exception {
        // Given
        VehicleDTO inputDTO = new VehicleDTO(null, "NEW-CAR", "NewBrand", "NewModel", 2022, 1L, null);
        VehicleDTO outputDTO = new VehicleDTO(1L, "NEW-CAR", "NewBrand", "NewModel", 2022, 1L, "ClientName");
        when(vehicleService.createVehicle(any(VehicleDTO.class))).thenReturn(outputDTO);

        // When & Then
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.licensePlate").value("NEW-CAR"));

        verify(vehicleService, times(1)).createVehicle(any(VehicleDTO.class));
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getVehicleById_shouldReturnVehicleWhenFound() throws Exception {
        // Given
        VehicleDTO vehicleDTO = new VehicleDTO(1L, "ABC-123", "Toyota", "Corolla", 2015, 1L, "Juan Perez");
        when(vehicleService.getVehicleById(1L)).thenReturn(vehicleDTO);

        // When & Then
        mockMvc.perform(get("/api/vehicles/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.licensePlate").value("ABC-123"));

        verify(vehicleService, times(1)).getVehicleById(1L);
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getVehicleById_shouldReturnNotFoundWhenVehicleNotFound() throws Exception {
        // Given
        when(vehicleService.getVehicleById(99L)).thenThrow(new ResourceNotFoundException("Vehicle not found"));

        // When & Then
        mockMvc.perform(get("/api/vehicles/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(vehicleService, times(1)).getVehicleById(99L);
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void updateVehicle_shouldReturnUpdatedVehicle() throws Exception {
        // Given
        VehicleDTO inputDTO = new VehicleDTO(1L, "UPD-123", "UpdatedBrand", "UpdatedModel", 2020, 1L, "Client Name");
        VehicleDTO outputDTO = new VehicleDTO(1L, "UPD-123", "UpdatedBrand", "UpdatedModel", 2020, 1L, "Client Name");
        when(vehicleService.updateVehicle(eq(1L), any(VehicleDTO.class))).thenReturn(outputDTO);

        // When & Then
        mockMvc.perform(put("/api/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("UPD-123"));

        verify(vehicleService, times(1)).updateVehicle(eq(1L), any(VehicleDTO.class));
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void deleteVehicle_shouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(vehicleService).deleteVehicle(1L);

        // When & Then
        mockMvc.perform(delete("/api/vehicles/1"))
                .andExpect(status().isNoContent());

        verify(vehicleService, times(1)).deleteVehicle(1L);
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getAllVehicles_shouldReturnListOfVehicles() throws Exception {
        // Given
        List<VehicleDTO> vehicleList = Arrays.asList(
                new VehicleDTO(1L, "V1", "B1", "M1", 2000, 1L, "C1"),
                new VehicleDTO(2L, "V2", "B2", "M2", 2001, 2L, "C2")
        );
        when(vehicleService.getAllVehicles()).thenReturn(vehicleList);

        // When & Then
        mockMvc.perform(get("/api/vehicles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].licensePlate").value("V1"));

        verify(vehicleService, times(1)).getAllVehicles();
    }
}