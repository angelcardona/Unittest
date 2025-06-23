package com.tallercarpro.appTaller.service;

import com.tallercarpro.appTaller.dto.VehicleDTO;
import com.tallercarpro.appTaller.exception.ResourceNotFoundException;
import com.tallercarpro.appTaller.model.Client;
import com.tallercarpro.appTaller.model.Vehicle;
import com.tallercarpro.appTaller.repository.ClientRepository;
import com.tallercarpro.appTaller.repository.VehicleRepository;
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
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private ClientRepository clientRepository; // Para asociar vehículo a cliente

    @InjectMocks
    private VehicleService vehicleService;

    private Client testClient;
    private Vehicle testVehicle;
    private VehicleDTO testVehicleDTO;

    @BeforeEach
    void setUp() {
        testClient = new Client(1L, "Juan Perez", "juan@example.com", "123456789");
        testVehicle = new Vehicle(10L, "ABC-123", "Toyota", "Corolla", 2015, testClient);
        testVehicleDTO = new VehicleDTO(10L, "ABC-123", "Toyota", "Corolla", 2015, 1L, "Juan Perez");
    }

    @Test
    void createVehicle_shouldReturnCreatedVehicleDTO() {
        // Given
        VehicleDTO newVehicleDTO = new VehicleDTO(null, "XYZ-456", "Nissan", "Sentra", 2018, 1L, null);
        Vehicle savedVehicle = new Vehicle(11L, "XYZ-456", "Nissan", "Sentra", 2018, testClient);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        // When
        VehicleDTO result = vehicleService.createVehicle(newVehicleDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getLicensePlate()).isEqualTo("XYZ-456");
        assertThat(result.getClientId()).isEqualTo(1L);
        verify(clientRepository, times(1)).findById(1L);
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_shouldThrowResourceNotFoundExceptionWhenClientNotFound() {
        // Given
        VehicleDTO newVehicleDTO = new VehicleDTO(null, "XYZ-456", "Nissan", "Sentra", 2018, 99L, null);
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> vehicleService.createVehicle(newVehicleDTO));
        verify(clientRepository, times(1)).findById(99L);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void getVehicleById_shouldReturnVehicleDTOWhenFound() {
        // Given
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(testVehicle));

        // When
        VehicleDTO result = vehicleService.getVehicleById(10L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getLicensePlate()).isEqualTo("ABC-123");
        verify(vehicleRepository, times(1)).findById(10L);
    }

    @Test
    void getVehicleById_shouldThrowResourceNotFoundExceptionWhenNotFound() {
        // Given
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> vehicleService.getVehicleById(99L));
        verify(vehicleRepository, times(1)).findById(99L);
    }

    @Test
    void updateVehicle_shouldReturnUpdatedVehicleDTO() {
        // Given
        VehicleDTO updatedVehicleDTO = new VehicleDTO(10L, "ABC-UPDATED", "Honda", "Civic", 2017, 1L, "Juan Perez");
        Vehicle existingVehicle = new Vehicle(10L, "ABC-123", "Toyota", "Corolla", 2015, testClient);
        Vehicle savedVehicle = new Vehicle(10L, "ABC-UPDATED", "Honda", "Civic", 2017, testClient);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(existingVehicle));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient)); // Client exists for update
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        // When
        VehicleDTO result = vehicleService.updateVehicle(10L, updatedVehicleDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getLicensePlate()).isEqualTo("ABC-UPDATED");
        assertThat(result.getBrand()).isEqualTo("Honda");
        verify(vehicleRepository, times(1)).findById(10L);
        verify(clientRepository, times(1)).findById(1L);
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void updateVehicle_shouldThrowResourceNotFoundExceptionWhenVehicleNotFound() {
        // Given
        VehicleDTO updatedVehicleDTO = new VehicleDTO(99L, "NON-EXISTENT", "Ford", "Focus", 2010, 1L, null);
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> vehicleService.updateVehicle(99L, updatedVehicleDTO));
        verify(vehicleRepository, times(1)).findById(99L);
        verify(clientRepository, never()).findById(anyLong());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void deleteVehicle_shouldDeleteVehicleSuccessfully() {
        // Given
        when(vehicleRepository.existsById(10L)).thenReturn(true);
        doNothing().when(vehicleRepository).deleteById(10L);

        // When
        vehicleService.deleteVehicle(10L);

        // Then
        verify(vehicleRepository, times(1)).existsById(10L);
        verify(vehicleRepository, times(1)).deleteById(10L);
    }

    @Test
    void deleteVehicle_shouldThrowResourceNotFoundExceptionWhenVehicleNotFound() {
        // Given
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> vehicleService.deleteVehicle(99L));
        verify(vehicleRepository, times(1)).existsById(99L);
        verify(vehicleRepository, never()).deleteById(anyLong());
    }

    @Test
    void getAllVehicles_shouldReturnListOfVehicleDTOs() {
        // Given
        Vehicle vehicle1 = new Vehicle(1L, "V1", "B1", "M1", 2000, testClient);
        Vehicle vehicle2 = new Vehicle(2L, "V2", "B2", "M2", 2001, testClient);
        when(vehicleRepository.findAll()).thenReturn(Arrays.asList(vehicle1, vehicle2));

        // When
        List<VehicleDTO> result = vehicleService.getAllVehicles();

        // Then
        assertThat(result).isNotNull().hasSize(2);
        assertThat(result.get(0).getLicensePlate()).isEqualTo("V1");
        assertThat(result.get(1).getBrand()).isEqualTo("B2");
        verify(vehicleRepository, times(1)).findAll();
    }
}