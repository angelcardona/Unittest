package com.tallercarpro.appTaller.service;

import com.tallercarpro.appTaller.dto.RepairDTO;
import com.tallercarpro.appTaller.exception.ResourceNotFoundException;
import com.tallercarpro.appTaller.model.Mechanic;
import com.tallercarpro.appTaller.model.Repair;
import com.tallercarpro.appTaller.model.Vehicle;
import com.tallercarpro.appTaller.repository.MechanicRepository;
import com.tallercarpro.appTaller.repository.RepairRepository;
import com.tallercarpro.appTaller.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RepairServiceTest {

    @Mock
    private RepairRepository repairRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private MechanicRepository mechanicRepository;

    @InjectMocks
    private RepairService repairService;

    private Vehicle testVehicle;
    private Mechanic testMechanic;
    private Repair testRepair;
    private RepairDTO testRepairDTO;

    @BeforeEach
    void setUp() {
        testVehicle = new Vehicle(1L, "TEST-VEH", "Brand", "Model", 2020, null);
        testMechanic = new Mechanic(1L, "Test Mechanic", "pass", "ROLE_MECHANIC", "active");
        testRepair = new Repair(100L, testVehicle, testMechanic, "Engine Repair",
                LocalDateTime.of(2024, 1, 1, 9, 0), LocalDateTime.of(2024, 1, 5, 17, 0),
                BigDecimal.valueOf(150.00), "COMPLETED");
        testRepairDTO = new RepairDTO(100L, 1L, "Engine Repair", LocalDateTime.of(2024, 1, 1, 9, 0),
                LocalDateTime.of(2024, 1, 5, 17, 0), BigDecimal.valueOf(150.00),
                "Test Mechanic", "COMPLETED");
    }

    @Test
    void createRepair_shouldReturnCreatedRepairDTO() {
        // Given
        RepairDTO newRepairDTO = new RepairDTO(null, 1L, "New Repair",
                LocalDateTime.now(), null, BigDecimal.valueOf(50.00), null, "PENDING");
        Repair savedRepair = new Repair(101L, testVehicle, testMechanic, "New Repair",
                LocalDateTime.now(), null, BigDecimal.valueOf(50.00), "PENDING");

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(mechanicRepository.findByName("Test Mechanic")).thenReturn(Optional.of(testMechanic)); // Assuming mechanic is found by name
        when(repairRepository.save(any(Repair.class))).thenReturn(savedRepair);

        // When
        RepairDTO result = repairService.createRepair(newRepairDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getDescription()).isEqualTo("New Repair");
        verify(vehicleRepository, times(1)).findById(1L);
        // verify(mechanicRepository, times(1)).findByName("Test Mechanic"); // If your service handles mechanic lookup
        verify(repairRepository, times(1)).save(any(Repair.class));
    }

    @Test
    void createRepair_shouldThrowExceptionWhenVehicleNotFound() {
        // Given
        RepairDTO newRepairDTO = new RepairDTO(null, 99L, "New Repair",
                LocalDateTime.now(), null, BigDecimal.valueOf(50.00), null, "PENDING");
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> repairService.createRepair(newRepairDTO));
        verify(vehicleRepository, times(1)).findById(99L);
        verify(repairRepository, never()).save(any(Repair.class));
    }

    @Test
    void getRepairById_shouldReturnRepairDTOWhenFound() {
        // Given
        when(repairRepository.findById(100L)).thenReturn(Optional.of(testRepair));

        // When
        RepairDTO result = repairService.getRepairById(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getDescription()).isEqualTo("Engine Repair");
        verify(repairRepository, times(1)).findById(100L);
    }

    @Test
    void getRepairById_shouldThrowResourceNotFoundExceptionWhenNotFound() {
        // Given
        when(repairRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> repairService.getRepairById(99L));
        verify(repairRepository, times(1)).findById(99L);
    }

    @Test
    void updateRepair_shouldReturnUpdatedRepairDTO() {
        // Given
        RepairDTO updatedRepairDTO = new RepairDTO(100L, 1L, "Engine Repair Updated",
                LocalDateTime.of(2024, 1, 1, 9, 0), LocalDateTime.of(2024, 1, 6, 10, 0),
                BigDecimal.valueOf(200.00), "Test Mechanic", "COMPLETED");
        Repair existingRepair = testRepair; // Use the initial testRepair
        Repair savedRepair = new Repair(100L, testVehicle, testMechanic, "Engine Repair Updated",
                LocalDateTime.of(2024, 1, 1, 9, 0), LocalDateTime.of(2024, 1, 6, 10, 0),
                BigDecimal.valueOf(200.00), "COMPLETED");

        when(repairRepository.findById(100L)).thenReturn(Optional.of(existingRepair));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(mechanicRepository.findByName("Test Mechanic")).thenReturn(Optional.of(testMechanic));
        when(repairRepository.save(any(Repair.class))).thenReturn(savedRepair);

        // When
        RepairDTO result = repairService.updateRepair(100L, updatedRepairDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getDescription()).isEqualTo("Engine Repair Updated");
        assertThat(result.getLaborCost()).isEqualTo(BigDecimal.valueOf(200.00));
        verify(repairRepository, times(1)).findById(100L);
        verify(repairRepository, times(1)).save(any(Repair.class));
    }

    @Test
    void updateRepair_shouldThrowResourceNotFoundExceptionWhenRepairNotFound() {
        // Given
        RepairDTO updatedRepairDTO = new RepairDTO(99L, 1L, "Non Existent",
                LocalDateTime.now(), null, BigDecimal.TEN, null, "PENDING");
        when(repairRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> repairService.updateRepair(99L, updatedRepairDTO));
        verify(repairRepository, times(1)).findById(99L);
        verify(repairRepository, never()).save(any(Repair.class));
    }

    @Test
    void deleteRepair_shouldDeleteRepairSuccessfully() {
        // Given
        when(repairRepository.existsById(100L)).thenReturn(true);
        doNothing().when(repairRepository).deleteById(100L);

        // When
        repairService.deleteRepair(100L);

        // Then
        verify(repairRepository, times(1)).existsById(100L);
        verify(repairRepository, times(1)).deleteById(100L);
    }

    @Test
    void deleteRepair_shouldThrowResourceNotFoundExceptionWhenRepairNotFound() {
        // Given
        when(repairRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> repairService.deleteRepair(99L));
        verify(repairRepository, times(1)).existsById(99L);
        verify(repairRepository, never()).deleteById(anyLong());
    }

    // You would add tests for getRepairsByVehicleId as well
}