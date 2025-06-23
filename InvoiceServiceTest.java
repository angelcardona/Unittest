package com.tallercarpro.appTaller.service;

import com.tallercarpro.appTaller.dto.InvoiceDTO;
import com.tallercarpro.appTaller.dto.InvoiceItemDTO;
import com.tallercarpro.appTaller.exception.ResourceNotFoundException;
import com.tallercarpro.appTaller.model.*;
import com.tallercarpro.appTaller.repository.ClientRepository;
import com.tallercarpro.appTaller.repository.InvoiceItemRepository;
import com.tallercarpro.appTaller.repository.InvoiceRepository;
import com.tallercarpro.appTaller.repository.MechanicRepository;
import com.tallercarpro.appTaller.repository.RepairRepository;
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
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private RepairRepository repairRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private MechanicRepository mechanicRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Repair testRepair;
    private Client testClient;
    private Mechanic testMechanic;
    private Invoice testInvoice;
    private InvoiceDTO testInvoiceDTO;
    private InvoiceItem testInvoiceItemPart;
    private InvoiceItem testInvoiceItemLabor;

    @BeforeEach
    void setUp() {
        testClient = new Client(1L, "Juan Perez", "juan@example.com", "123456789");
        testMechanic = new Mechanic(1L, "Pedro Mecanico", "password", "ROLE_MECHANIC", "active");
        testRepair = new Repair(10L, new Vehicle(), testMechanic, "Repair Desc",
                LocalDateTime.now(), LocalDateTime.now(), BigDecimal.valueOf(100.0), "COMPLETED");

        testInvoice = new Invoice(100L, "INV-001", testRepair, testClient, testMechanic,
                LocalDateTime.of(2024, 1, 15, 10, 0), BigDecimal.valueOf(250.00));
        testInvoiceItemPart = new InvoiceItem(1L, testInvoice, "Oil Filter", "PART", BigDecimal.valueOf(50), 1, BigDecimal.valueOf(50));
        testInvoiceItemLabor = new InvoiceItem(2L, testInvoice, "Labor", "LABOR", BigDecimal.valueOf(200), 1, BigDecimal.valueOf(200));
        testInvoice.setInvoiceItems(Arrays.asList(testInvoiceItemPart, testInvoiceItemLabor));


        testInvoiceDTO = new InvoiceDTO(100L, "INV-001", 10L, 1L, 1L, LocalDateTime.of(2024, 1, 15, 10, 0), BigDecimal.valueOf(250.00),
                "Juan Perez", "Pedro Mecanico",
                Arrays.asList(
                        new InvoiceItemDTO(1L, "Oil Filter", "PART", BigDecimal.valueOf(50), 1, BigDecimal.valueOf(50)),
                        new InvoiceItemDTO(2L, "Labor", "LABOR", BigDecimal.valueOf(200), 1, BigDecimal.valueOf(200))
                ));
    }

    @Test
    void createInvoice_shouldReturnCreatedInvoiceDTO() {
        // Given
        InvoiceDTO newInvoiceDTO = new InvoiceDTO(null, "INV-002", 10L, 1L, 1L,
                LocalDateTime.now(), BigDecimal.ZERO, null, null,
                Arrays.asList(
                        new InvoiceItemDTO(null, "New Part", "PART", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100)),
                        new InvoiceItemDTO(null, "New Labor", "LABOR", BigDecimal.valueOf(50), 1, BigDecimal.valueOf(50))
                ));
        Invoice savedInvoice = new Invoice(101L, "INV-002", testRepair, testClient, testMechanic,
                newInvoiceDTO.getIssueDate(), BigDecimal.valueOf(150.00));

        when(repairRepository.findById(10L)).thenReturn(Optional.of(testRepair));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(testMechanic));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);
        when(invoiceItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]); // Return saved items

        // When
        InvoiceDTO result = invoiceService.createInvoice(newInvoiceDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(150.00)); // Verify total calculation
        assertThat(result.getInvoiceItems()).hasSize(2);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(invoiceItemRepository, times(1)).saveAll(anyList());
    }

    @Test
    void createInvoice_shouldThrowResourceNotFoundExceptionIfRepairNotFound() {
        // Given
        InvoiceDTO newInvoiceDTO = new InvoiceDTO(null, "INV-002", 99L, 1L, 1L,
                LocalDateTime.now(), BigDecimal.ZERO, null, null, Collections.emptyList());
        when(repairRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.createInvoice(newInvoiceDTO));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void getInvoiceById_shouldReturnInvoiceDTOWhenFound() {
        // Given
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(testInvoice));
        // You might need to mock invoiceItemRepository.findByInvoiceId(100L) if your service fetches items separately
        when(invoiceItemRepository.findByInvoiceId(100L)).thenReturn(Arrays.asList(testInvoiceItemPart, testInvoiceItemLabor));

        // When
        InvoiceDTO result = invoiceService.getInvoiceById(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(250.00));
        assertThat(result.getInvoiceItems()).hasSize(2);
        verify(invoiceRepository, times(1)).findById(100L);
    }

    @Test
    void getInvoiceById_shouldThrowResourceNotFoundExceptionWhenNotFound() {
        // Given
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(99L));
        verify(invoiceRepository, times(1)).findById(99L);
    }

    @Test
    void updateInvoice_shouldReturnUpdatedInvoiceDTO() {
        // Given
        InvoiceDTO updatedInvoiceDTO = new InvoiceDTO(100L, "INV-001-UPD", 10L, 1L, 1L,
                LocalDateTime.of(2024, 1, 15, 10, 0), BigDecimal.ZERO, null, null,
                Arrays.asList(
                        new InvoiceItemDTO(1L, "Updated Part", "PART", BigDecimal.valueOf(60), 1, BigDecimal.valueOf(60)), // Existing item updated
                        new InvoiceItemDTO(null, "New Service", "LABOR", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100)) // New item added
                ));
        
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(testInvoice)); // Return original invoice
        when(repairRepository.findById(10L)).thenReturn(Optional.of(testRepair));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(testMechanic));
        
        // Simular que save se llama con la factura actualizada
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setTotalAmount(BigDecimal.valueOf(160.00)); // Total sum of new items
            return inv;
        });

        when(invoiceItemRepository.findByInvoiceId(100L)).thenReturn(Arrays.asList(testInvoiceItemPart, testInvoiceItemLabor)); // Original items
        doNothing().when(invoiceItemRepository).deleteAll(anyList()); // Simulate deletion of old items
        when(invoiceItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]); // Simulate saving new items

        // When
        InvoiceDTO result = invoiceService.updateInvoice(100L, updatedInvoiceDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-001-UPD");
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(160.00));
        assertThat(result.getInvoiceItems()).hasSize(2);
        verify(invoiceRepository, times(1)).findById(100L);
        verify(invoiceItemRepository, times(1)).findByInvoiceId(100L); // To fetch old items
        verify(invoiceItemRepository, times(1)).deleteAll(anyList()); // To delete old items
        verify(invoiceItemRepository, times(1)).saveAll(anyList()); // To save new items
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void deleteInvoice_shouldDeleteInvoiceSuccessfully() {
        // Given
        when(invoiceRepository.existsById(100L)).thenReturn(true);
        doNothing().when(invoiceRepository).deleteById(100L);

        // When
        invoiceService.deleteInvoice(100L);

        // Then
        verify(invoiceRepository, times(1)).existsById(100L);
        verify(invoiceRepository, times(1)).deleteById(100L);
    }

    @Test
    void deleteInvoice_shouldThrowResourceNotFoundExceptionWhenInvoiceNotFound() {
        // Given
        when(invoiceRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.deleteInvoice(99L));
        verify(invoiceRepository, times(1)).existsById(99L);
        verify(invoiceRepository, never()).deleteById(anyLong());
    }

    // You would add tests for getInvoicesByRepairId, getInvoicesByClientId as well
}