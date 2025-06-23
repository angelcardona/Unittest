package com.tallercarpro.appTaller.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallercarpro.appTaller.dto.ClientDTO;
import com.tallercarpro.appTaller.exception.ResourceNotFoundException; // Asumo esta excepción
import com.tallercarpro.appTaller.service.ClientService;
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

public class ClientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(clientController)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void createClient_shouldReturnCreatedClient() throws Exception {
        // Given
        ClientDTO inputClientDTO = new ClientDTO(null, "New Client", "new@example.com", "111222333");
        ClientDTO outputClientDTO = new ClientDTO(1L, "New Client", "new@example.com", "111222333");
        when(clientService.createClient(any(ClientDTO.class))).thenReturn(outputClientDTO);

        // When & Then
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputClientDTO)))
                .andExpect(status().isCreated()) // Asumo que el POST devuelve 201 Created
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("New Client"));

        verify(clientService, times(1)).createClient(any(ClientDTO.class));
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getClientById_shouldReturnClientWhenFound() throws Exception {
        // Given
        ClientDTO clientDTO = new ClientDTO(1L, "Juan Perez", "juan@example.com", "123456789");
        when(clientService.getClientById(1L)).thenReturn(clientDTO);

        // When & Then
        mockMvc.perform(get("/api/clients/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Juan Perez"));

        verify(clientService, times(1)).getClientById(1L);
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getClientById_shouldReturnNotFoundWhenClientNotFound() throws Exception {
        // Given
        when(clientService.getClientById(99L)).thenThrow(new ResourceNotFoundException("Client not found"));

        // When & Then
        mockMvc.perform(get("/api/clients/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).getClientById(99L);
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void updateClient_shouldReturnUpdatedClient() throws Exception {
        // Given
        ClientDTO inputDTO = new ClientDTO(1L, "Juan Updated", "juan_u@e.com", "9999");
        ClientDTO outputDTO = new ClientDTO(1L, "Juan Updated", "juan_u@e.com", "9999");
        when(clientService.updateClient(eq(1L), any(ClientDTO.class))).thenReturn(outputDTO);

        // When & Then
        mockMvc.perform(put("/api/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Updated"));

        verify(clientService, times(1)).updateClient(eq(1L), any(ClientDTO.class));
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void deleteClient_shouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(clientService).deleteClient(1L);

        // When & Then
        mockMvc.perform(delete("/api/clients/1"))
                .andExpect(status().isNoContent()); // O 200 OK si no devuelve 204 No Content

        verify(clientService, times(1)).deleteClient(1L);
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getAllClients_shouldReturnListOfClients() throws Exception {
        // Given
        List<ClientDTO> clientList = Arrays.asList(
                new ClientDTO(1L, "Client A", "a@e.com", "111"),
                new ClientDTO(2L, "Client B", "b@e.com", "222")
        );
        when(clientService.getAllClients()).thenReturn(clientList);

        // When & Then
        mockMvc.perform(get("/api/clients")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Client A"));

        verify(clientService, times(1)).getAllClients();
    }
}