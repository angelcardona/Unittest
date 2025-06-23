package com.tallercarpro.appTaller.service;

import com.tallercarpro.appTaller.dto.ClientDTO;
import com.tallercarpro.appTaller.exception.ResourceNotFoundException; // Asumo esta excepción personalizada
import com.tallercarpro.appTaller.model.Client;
import com.tallercarpro.appTaller.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    private Client testClient;
    private ClientDTO testClientDTO;

    @BeforeEach
    void setUp() {
        testClient = new Client(1L, "Juan Perez", "juan@example.com", "123456789");
        testClientDTO = new ClientDTO(1L, "Juan Perez", "juan@example.com", "123456789");
    }

    @Test
    void createClient_shouldReturnCreatedClientDTO() {
        // Given
        ClientDTO newClientDTO = new ClientDTO(null, "Maria Gomez", "maria@example.com", "987654321");
        Client savedClient = new Client(2L, "Maria Gomez", "maria@example.com", "987654321");
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        // When
        ClientDTO result = clientService.createClient(newClientDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo("Maria Gomez");
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    void getClientById_shouldReturnClientDTOWhenFound() {
        // Given
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));

        // When
        ClientDTO result = clientService.getClientById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Juan Perez");
        verify(clientRepository, times(1)).findById(1L);
    }

    @Test
    void getClientById_shouldThrowResourceNotFoundExceptionWhenNotFound() {
        // Given
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(99L));
        verify(clientRepository, times(1)).findById(99L);
    }

    @Test
    void updateClient_shouldReturnUpdatedClientDTO() {
        // Given
        ClientDTO updatedClientDTO = new ClientDTO(1L, "Juan Perez Updated", "juan_updated@example.com", "999999999");
        Client existingClient = new Client(1L, "Juan Perez", "juan@example.com", "123456789");
        Client savedClient = new Client(1L, "Juan Perez Updated", "juan_updated@example.com", "999999999");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        // When
        ClientDTO result = clientService.updateClient(1L, updatedClientDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Juan Perez Updated");
        assertThat(result.getEmail()).isEqualTo("juan_updated@example.com");
        verify(clientRepository, times(1)).findById(1L);
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    void updateClient_shouldThrowResourceNotFoundExceptionWhenClientNotFound() {
        // Given
        ClientDTO updatedClientDTO = new ClientDTO(1L, "Non Existent", "no@example.com", "111");
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> clientService.updateClient(99L, updatedClientDTO));
        verify(clientRepository, times(1)).findById(99L);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void deleteClient_shouldDeleteClientSuccessfully() {
        // Given
        when(clientRepository.existsById(1L)).thenReturn(true);
        doNothing().when(clientRepository).deleteById(1L);

        // When
        clientService.deleteClient(1L);

        // Then
        verify(clientRepository, times(1)).existsById(1L);
        verify(clientRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteClient_shouldThrowResourceNotFoundExceptionWhenClientNotFound() {
        // Given
        when(clientRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> clientService.deleteClient(99L));
        verify(clientRepository, times(1)).existsById(99L);
        verify(clientRepository, never()).deleteById(anyLong());
    }

    @Test
    void getAllClients_shouldReturnListOfClientDTOs() {
        // Given
        Client client1 = new Client(1L, "Juan", "juan@e.com", "1");
        Client client2 = new Client(2L, "Maria", "maria@e.com", "2");
        when(clientRepository.findAll()).thenReturn(Arrays.asList(client1, client2));

        // When
        List<ClientDTO> result = clientService.getAllClients();

        // Then
        assertThat(result).isNotNull().hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Juan");
        assertThat(result.get(1).getName()).isEqualTo("Maria");
        verify(clientRepository, times(1)).findAll();
    }

    @Test
    void getAllClients_shouldReturnEmptyListIfNoClients() {
        // Given
        when(clientRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<ClientDTO> result = clientService.getAllClients();

        // Then
        assertThat(result).isNotNull().isEmpty();
        verify(clientRepository, times(1)).findAll();
    }
}