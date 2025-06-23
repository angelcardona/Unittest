package com.tallercarpro.appTaller.service;

import com.tallercarpro.appTaller.dto.*;
import com.tallercarpro.appTaller.model.*;
import com.tallercarpro.appTaller.repository.*;
import com.tallercarpro.appTaller.util.ExcelGenerator; // Asegúrate de que esta clase sea accesible para la prueba

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example; // Para testear con Example Matcher si lo usas

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private RepairRepository repairRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private MechanicRepository mechanicRepository; // Asumiendo que necesitas este mock para liquidaciones
    // Si ExcelGenerator no es estático y es un bean de Spring, podrías mockearlo.
    // Si es una clase de utilidad con métodos estáticos, la pruebas en su propio archivo o asumes que funciona.
    // Por simplicidad, aquí asumiré que AnalyticsService solo DELEGA la generación a ella.

    @InjectMocks
    private AnalyticsService analyticsService;

    // Datos de prueba comunes
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Vehicle testVehicle;
    private Client testClient;
    private Mechanic testMechanic;
    private Repair testRepair;
    private Invoice testInvoice;
    private InvoiceItem testInvoiceItemPart;
    private InvoiceItem testInvoiceItemLabor;

    @BeforeEach
    void setUp() {
        // Inicializar mocks y la instancia del servicio antes de cada prueba
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
        startDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        endDateTime = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        testClient = new Client(1L, "Juan Perez", "juan@example.com", "123456789");
        testVehicle = new Vehicle(100L, "ABC-123", "Toyota", "Corolla", 2015, testClient);
        testMechanic = new Mechanic(1L, "Pedro Mecanico", "password", "ROLE_MECHANIC", "activo");

        testRepair = new Repair(200L, testVehicle, testMechanic, "Cambio de aceite",
                                startDateTime, endDateTime, BigDecimal.valueOf(50.00), "COMPLETED");

        testInvoice = new Invoice(300L, "INV-2024-001", testRepair, testClient, testMechanic,
                                  LocalDateTime.of(2024, 1, 25, 10, 0, 0), BigDecimal.valueOf(150.00));

        testInvoiceItemPart = new InvoiceItem(400L, testInvoice, "Filtro de Aceite", "PART",
                                            BigDecimal.valueOf(30.00), 1, BigDecimal.valueOf(30.00));
        testInvoiceItemLabor = new InvoiceItem(401L, testInvoice, "Mano de obra cambio", "LABOR",
                                             BigDecimal.valueOf(100.00), 1, BigDecimal.valueOf(100.00)); // Esto es parte de la factura, no el laborCost de la reparación
                                             // El laborCost de la reparación es por el trabajo en sí.
    }

    // --- Pruebas para getFinancialSummary ---
    @Test
    void getFinancialSummary_shouldReturnCorrectSummary() {
        // Given
        Invoice invoice1 = new Invoice(301L, "INV-001", null, null, null, LocalDateTime.of(2024, 1, 15, 0, 0), BigDecimal.valueOf(200.00));
        Invoice invoice2 = new Invoice(302L, "INV-002", null, null, null, LocalDateTime.of(2024, 1, 20, 0, 0), BigDecimal.valueOf(300.00));

        InvoiceItem item1_part = new InvoiceItem(501L, invoice1, "Filtro", "PART", BigDecimal.valueOf(50), 1, BigDecimal.valueOf(50));
        InvoiceItem item1_labor = new InvoiceItem(502L, invoice1, "Mano de obra", "LABOR", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        invoice1.setInvoiceItems(Arrays.asList(item1_part, item1_labor));

        InvoiceItem item2_part = new InvoiceItem(503L, invoice2, "Bujías", "PART", BigDecimal.valueOf(80), 1, BigDecimal.valueOf(80));
        InvoiceItem item2_labor = new InvoiceItem(504L, invoice2, "Diagnóstico", "LABOR", BigDecimal.valueOf(150), 1, BigDecimal.valueOf(150));
        invoice2.setInvoiceItems(Arrays.asList(item2_part, item2_labor));
        
        // Mock the repairs associated with these invoices to get laborCost and vehicle count
        Repair repair1 = new Repair(1L, new Vehicle(), new Mechanic(), "Rep1", LocalDateTime.of(2024,1,14,0,0), LocalDateTime.of(2024,1,15,0,0), BigDecimal.valueOf(70.00), "COMPLETED");
        Repair repair2 = new Repair(2L, new Vehicle(), new Mechanic(), "Rep2", LocalDateTime.of(2024,1,19,0,0), LocalDateTime.of(2024,1,20,0,0), BigDecimal.valueOf(120.00), "COMPLETED");
        invoice1.setRepair(repair1); // Link invoice to repair
        invoice2.setRepair(repair2);

        when(invoiceRepository.findByIssueDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(invoice1, invoice2));

        // When
        FinancialSummaryDTO summary = analyticsService.getFinancialSummary(startDate, endDate);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalInvoicedAmount()).isEqualTo(BigDecimal.valueOf(500.00)); // 200 + 300
        assertThat(summary.getTotalLaborCost()).isEqualTo(BigDecimal.valueOf(190.00)); // 70 + 120 (from repairs, not invoice items)
        assertThat(summary.getTotalPartsCost()).isEqualTo(BigDecimal.valueOf(130.00)); // 50 + 80 (from invoice items)
        assertThat(summary.getTotalVehiclesServiced()).isEqualTo(2); // Two distinct repairs/vehicles
        verify(invoiceRepository, times(1)).findByIssueDateBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    @Test
    void getFinancialSummary_shouldReturnZeroWhenNoInvoices() {
        // Given
        when(invoiceRepository.findByIssueDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        FinancialSummaryDTO summary = analyticsService.getFinancialSummary(startDate, endDate);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalInvoicedAmount()).isZero();
        assertThat(summary.getTotalLaborCost()).isZero();
        assertThat(summary.getTotalPartsCost()).isZero();
        assertThat(summary.getTotalVehiclesServiced()).isZero();
    }

    // --- Pruebas para getTotalPaidToSuppliers ---
    @Test
    void getTotalPaidToSuppliers_shouldReturnCorrectSummary() {
        // Given - Simular pagos a proveedores (asumo que tienes una entidad para esto, o es parte de InvoiceItem)
        // Por simplicidad, simularemos que InvoiceItem "PART" tienen un costo de proveedor asociado
        // Idealmente, tendrías una entidad `SupplierPayment` o similar.
        InvoiceItem item1 = new InvoiceItem(1L, null, "Oil Filter", "PART", BigDecimal.valueOf(30.0), 1, BigDecimal.valueOf(30.0));
        InvoiceItem item2 = new InvoiceItem(2L, null, "Tires", "PART", BigDecimal.valueOf(200.0), 4, BigDecimal.valueOf(800.0));
        
        // Simular que invoiceRepository tiene un método para obtener ítems comprados a proveedores.
        // O si los pagos a proveedores son independientes de las facturas a clientes, mockear ese repositorio.
        // Para este ejemplo, asumo que el total pagado a proveedores viene de los ítems de factura tipo PART
        // y que tienes una lógica para determinar su "costo de proveedor" (aquí lo asumiré igual al subtotal del item PART)
        when(invoiceItemRepository.findByItemTypeAndInvoice_IssueDateBetween("PART", startDateTime, endDateTime))
            .thenReturn(Arrays.asList(item1, item2));

        // When
        SupplierSummaryDTO summary = analyticsService.getTotalPaidToSuppliers(startDate, endDate);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalPaid()).isEqualTo(BigDecimal.valueOf(830.0)); // 30 + 800
        verify(invoiceItemRepository, times(1)).findByItemTypeAndInvoice_IssueDateBetween("PART", startDateTime, endDateTime);
    }

    @Test
    void getTotalPaidToSuppliers_shouldReturnZeroWhenNoItems() {
        // Given
        when(invoiceItemRepository.findByItemTypeAndInvoice_IssueDateBetween("PART", startDateTime, endDateTime))
            .thenReturn(Collections.emptyList());

        // When
        SupplierSummaryDTO summary = analyticsService.getTotalPaidToSuppliers(startDate, endDate);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalPaid()).isZero();
    }

    // --- Pruebas para getMonthlySalesData ---
    @Test
    void getMonthlySalesData_shouldAggregateSalesCorrectly() {
        // Given
        Invoice inv1 = new Invoice(1L, "INV001", null, null, null, LocalDateTime.of(2024, 1, 10, 0, 0), BigDecimal.valueOf(100));
        Invoice inv2 = new Invoice(2L, "INV002", null, null, null, LocalDateTime.of(2024, 1, 20, 0, 0), BigDecimal.valueOf(150));
        Invoice inv3 = new Invoice(3L, "INV003", null, null, null, LocalDateTime.of(2024, 2, 5, 0, 0), BigDecimal.valueOf(200));

        when(invoiceRepository.findByIssueDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(inv1, inv2, inv3));

        // When
        List<SalesChartDataDTO> data = analyticsService.getMonthlySalesData(startDate, endDate.plusMonths(1)); // Test across months

        // Then
        assertThat(data).isNotNull().hasSize(2); // Jan and Feb
        assertThat(data.get(0).getMonth()).isEqualTo("Enero 2024");
        assertThat(data.get(0).getTotalSales()).isEqualTo(BigDecimal.valueOf(250.0)); // 100 + 150
        assertThat(data.get(1).getMonth()).isEqualTo("Febrero 2024");
        assertThat(data.get(1).getTotalSales()).isEqualTo(BigDecimal.valueOf(200.0));
    }

    @Test
    void getMonthlySalesData_shouldReturnEmptyListIfNoSales() {
        // Given
        when(invoiceRepository.findByIssueDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<SalesChartDataDTO> data = analyticsService.getMonthlySalesData(startDate, endDate);

        // Then
        assertThat(data).isNotNull().isEmpty();
    }

    // --- Pruebas para getVehicleRepairInvoiceDetails ---
    @Test
    void getVehicleRepairInvoiceDetails_shouldReturnDetailsWithAllFilters() {
        // Given
        // Crear un VehicleDetailDTO anidado con RepairDTO y InvoiceDTO
        Client mockClient = new Client(1L, "Test Client", "test@test.com", "123456789");
        Vehicle mockVehicle = new Vehicle(1L, "XYZ-789", "Nissan", "Sentra", 2018, mockClient);
        Mechanic mockMechanic = new Mechanic(1L, "Test Mechanic", "pass", "ROLE_MECHANIC", "active");

        Repair mockRepair = new Repair(1L, mockVehicle, mockMechanic, "Engine Check",
                LocalDateTime.of(2024, 1, 10, 0, 0), LocalDateTime.of(2024, 1, 12, 0, 0),
                BigDecimal.valueOf(80.00), "COMPLETED");

        Invoice mockInvoice = new Invoice(1L, "INV-001", mockRepair, mockClient, mockMechanic,
                LocalDateTime.of(2024, 1, 12, 14, 30), BigDecimal.valueOf(250.00));
        InvoiceItem mockItemPart = new InvoiceItem(1L, mockInvoice, "Oil Filter", "PART", BigDecimal.valueOf(30), 1, BigDecimal.valueOf(30));
        InvoiceItem mockItemLabor = new InvoiceItem(2L, mockInvoice, "Diagnostic Labor", "LABOR", BigDecimal.valueOf(140), 1, BigDecimal.valueOf(140));
        mockInvoice.setInvoiceItems(Arrays.asList(mockItemPart, mockItemLabor));
        mockRepair.setInvoice(mockInvoice); // Link repair to invoice

        mockVehicle.setRepairs(Collections.singletonList(mockRepair)); // Link vehicle to repair

        // Mock repository calls
        when(vehicleRepository.findAll()).thenReturn(Collections.singletonList(mockVehicle));
        // You might need to mock findByClientId, findByLicensePlateContainingIgnoreCase etc. based on your service implementation.
        // For simplicity, let's assume getVehicleRepairInvoiceDetails internally filters on a list returned by a broader find.
        // A more robust mock would simulate the actual repository queries.
        // Example with Example Matcher for more precise mocking:
        when(vehicleRepository.findAll(any(Example.class)))
             .thenReturn(Collections.singletonList(mockVehicle));


        // When
        List<VehicleDetailDTO> details = analyticsService.getVehicleRepairInvoiceDetails(
                startDateTime.minusDays(1), endDateTime.plusDays(1), "XYZ-789", "Nissan", "Test Client");

        // Then
        assertThat(details).isNotNull().hasSize(1);
        VehicleDetailDTO dto = details.get(0);
        assertThat(dto.getLicensePlate()).isEqualTo("XYZ-789");
        assertThat(dto.getRepairs()).hasSize(1);
        assertThat(dto.getRepairs().get(0).getInvoice().getTotalAmount()).isEqualTo(BigDecimal.valueOf(250.00));
        assertThat(dto.getRepairs().get(0).getInvoice().getInvoiceItems()).hasSize(2);

        // Verify that the filtering logic within the service is correct
        // This implicitly tests if the service's filtering logic works with the provided parameters
    }

    @Test
    void getVehicleRepairInvoiceDetails_shouldReturnEmptyListIfNoMatch() {
        // Given
        when(vehicleRepository.findAll(any(Example.class))) // or whatever method it uses for filtering
            .thenReturn(Collections.emptyList());

        // When
        List<VehicleDetailDTO> details = analyticsService.getVehicleRepairInvoiceDetails(
                startDateTime, endDateTime, "NON-EXISTENT", "Ford", "No Client");

        // Then
        assertThat(details).isNotNull().isEmpty();
    }


    // --- Pruebas para convertToRepairSummaryExcelRows ---
    @Test
    void convertToRepairSummaryExcelRows_shouldConvertCorrectly() {
        // Given
        VehicleDetailDTO vehicleDetailDTO = new VehicleDetailDTO(
            1L, "ABC-123", "Toyota", "Corolla", 2015, "Juan Perez"
        );
        RepairDTO repairDTO = new RepairDTO(
            10L, 1L, "Cambio de llantas", LocalDateTime.of(2024, 5, 1, 9, 0), LocalDateTime.of(2024, 5, 1, 11, 0),
            BigDecimal.valueOf(75.00), "Pedro Mecanico", "COMPLETED"
        );
        InvoiceDTO invoiceDTO = new InvoiceDTO(
            100L, "INV-001", 10L, 1L, 1L, LocalDateTime.of(2024, 5, 1, 12, 0), BigDecimal.valueOf(300.00),
            "Juan Perez", "Pedro Mecanico", Collections.emptyList() // No items for this test
        );
        repairDTO.setInvoice(invoiceDTO);
        vehicleDetailDTO.setRepairs(Collections.singletonList(repairDTO));

        List<VehicleDetailDTO> input = Collections.singletonList(vehicleDetailDTO);

        // When
        List<RepairSummaryExcelRowDTO> output = analyticsService.convertToRepairSummaryExcelRows(input);

        // Then
        assertThat(output).isNotNull().hasSize(1);
        RepairSummaryExcelRowDTO row = output.get(0);

        assertThat(row.getVehicleLicensePlate()).isEqualTo("ABC-123");
        assertThat(row.getRepairDescription()).isEqualTo("Cambio de llantas");
        assertThat(row.getInvoiceTotalAmount()).isEqualTo(BigDecimal.valueOf(300.00));
        assertThat(row.isHasInvoiceItems()).isFalse();
    }

    @Test
    void convertToRepairSummaryExcelRows_shouldHandleNullInvoice() {
        // Given
        VehicleDetailDTO vehicleDetailDTO = new VehicleDetailDTO(
            1L, "ABC-123", "Toyota", "Corolla", 2015, "Juan Perez"
        );
        RepairDTO repairDTO = new RepairDTO(
            10L, 1L, "Inspección", LocalDateTime.of(2024, 5, 1, 9, 0), null,
            BigDecimal.valueOf(30.00), "Pedro Mecanico", "PENDING"
        );
        repairDTO.setInvoice(null); // Explicitly set invoice to null
        vehicleDetailDTO.setRepairs(Collections.singletonList(repairDTO));

        List<VehicleDetailDTO> input = Collections.singletonList(vehicleDetailDTO);

        // When
        List<RepairSummaryExcelRowDTO> output = analyticsService.convertToRepairSummaryExcelRows(input);

        // Then
        assertThat(output).isNotNull().hasSize(1);
        RepairSummaryExcelRowDTO row = output.get(0);
        assertThat(row.getInvoiceId()).isNull(); // Should be null if no invoice
        assertThat(row.getInvoiceTotalAmount()).isZero(); // Or whatever default you set
        assertThat(row.isHasInvoiceItems()).isFalse();
    }

    // --- Pruebas para convertToInvoiceItemDetailExcelRows ---
    @Test
    void convertToInvoiceItemDetailExcelRows_shouldConvertCorrectly() {
        // Given
        VehicleDetailDTO vehicleDetailDTO = new VehicleDetailDTO(
            1L, "XYZ-789", "Nissan", "Sentra", 2018, "Maria Lopez"
        );
        RepairDTO repairDTO = new RepairDTO(
            20L, 1L, "Revisión frenos", LocalDateTime.of(2024, 6, 1, 9, 0), LocalDateTime.of(2024, 6, 1, 10, 0),
            BigDecimal.valueOf(50.00), "Ana Ingeniera", "COMPLETED"
        );
        InvoiceItemDTO item1 = new InvoiceItemDTO(100L, "Pastillas de freno", "PART", BigDecimal.valueOf(80.00), 2, BigDecimal.valueOf(160.00));
        InvoiceItemDTO item2 = new InvoiceItemDTO(101L, "Líquido de frenos", "PART", BigDecimal.valueOf(20.00), 1, BigDecimal.valueOf(20.00));
        InvoiceDTO invoiceDTO = new InvoiceDTO(
            200L, "INV-002", 20L, 1L, 2L, LocalDateTime.of(2024, 6, 1, 11, 0), BigDecimal.valueOf(230.00),
            "Maria Lopez", "Ana Ingeniera", Arrays.asList(item1, item2)
        );
        repairDTO.setInvoice(invoiceDTO);
        vehicleDetailDTO.setRepairs(Collections.singletonList(repairDTO));

        List<VehicleDetailDTO> input = Collections.singletonList(vehicleDetailDTO);

        // When
        List<InvoiceItemExcelRowDTO> output = analyticsService.convertToInvoiceItemDetailExcelRows(input);

        // Then
        assertThat(output).isNotNull().hasSize(2); // Two invoice items

        InvoiceItemExcelRowDTO row1 = output.get(0);
        assertThat(row1.getVehicleLicensePlate()).isEqualTo("XYZ-789");
        assertThat(row1.getRepairDescription()).isEqualTo("Revisión frenos");
        assertThat(row1.getInvoiceNumber()).isEqualTo("INV-002");
        assertThat(row1.getItemDescription()).isEqualTo("Pastillas de freno");
        assertThat(row1.getItemQuantity()).isEqualTo(2);

        InvoiceItemExcelRowDTO row2 = output.get(1);
        assertThat(row2.getItemDescription()).isEqualTo("Líquido de frenos");
        assertThat(row2.getItemQuantity()).isEqualTo(1);
    }

    @Test
    void convertToInvoiceItemDetailExcelRows_shouldHandleNoInvoiceItems() {
        // Given
        VehicleDetailDTO vehicleDetailDTO = new VehicleDetailDTO(
            1L, "XYZ-789", "Nissan", "Sentra", 2018, "Maria Lopez"
        );
        RepairDTO repairDTO = new RepairDTO(
            20L, 1L, "Revisión", LocalDateTime.of(2024, 6, 1, 9, 0), LocalDateTime.of(2024, 6, 1, 10, 0),
            BigDecimal.valueOf(50.00), "Ana Ingeniera", "COMPLETED"
        );
        InvoiceDTO invoiceDTO = new InvoiceDTO(
            200L, "INV-002", 20L, 1L, 2L, LocalDateTime.of(2024, 6, 1, 11, 0), BigDecimal.valueOf(50.00),
            "Maria Lopez", "Ana Ingeniera", Collections.emptyList() // No items
        );
        repairDTO.setInvoice(invoiceDTO);
        vehicleDetailDTO.setRepairs(Collections.singletonList(repairDTO));

        List<VehicleDetailDTO> input = Collections.singletonList(vehicleDetailDTO);

        // When
        List<InvoiceItemExcelRowDTO> output = analyticsService.convertToInvoiceItemDetailExcelRows(input);

        // Then
        assertThat(output).isNotNull().isEmpty(); // Should be empty if no items found
    }

    // --- Otros métodos de servicio (SupplierProfitability, MechanicLiquidations, etc.) serían similares ---
    // Simular los repositorios y verificar los cálculos/transformaciones.

}