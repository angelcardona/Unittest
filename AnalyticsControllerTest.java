package com.tallercarpro.appTaller.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallercarpro.appTaller.dto.*;
import com.tallercarpro.appTaller.service.AnalyticsService;
import com.tallercarpro.appTaller.util.ExcelGenerator; // Asegúrate de que esta clase sea accesible para la prueba
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController)
                // Esto es crucial para probar @PreAuthorize, ya que MockMvcBuilders.standaloneSetup
                // no configura el contexto de seguridad por defecto.
                // Importante: Asegúrate de tener la dependencia spring-security-test.
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Para LocalDateTime
    }

    // --- Pruebas para /api/analytics/financial-summary ---
    @Test
    @WithMockUser(roles={"ADMIN", "USER"}) // Simular un usuario autenticado con roles
    void getFinancialSummary_shouldReturnOkAndSummary() throws Exception {
        // Given
        FinancialSummaryDTO mockSummary = new FinancialSummaryDTO(
                BigDecimal.valueOf(1000.00), BigDecimal.valueOf(300.00),
                BigDecimal.valueOf(500.00), 5);
        when(analyticsService.getFinancialSummary(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockSummary);

        // When & Then
        mockMvc.perform(get("/api/analytics/financial-summary")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoicedAmount").value(1000.00))
                .andExpect(jsonPath("$.totalLaborCost").value(300.00));

        verify(analyticsService, times(1)).getFinancialSummary(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    }

    @Test
    void getFinancialSummary_shouldReturnUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/analytics/financial-summary")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isUnauthorized()); // O .isForbidden() dependiendo de la configuración de Spring Security
    }

    @Test
    @WithMockUser(roles={"ADMIN"})
    void getFinancialSummary_shouldReturnInternalServerErrorOnServiceException() throws Exception {
        // Given
        when(analyticsService.getFinancialSummary(any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/analytics/financial-summary")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    // --- Pruebas para /api/analytics/vehicles-details ---
    @Test
    @WithMockUser(roles={"ADMIN", "USER"})
    void getVehicleRepairInvoiceDetails_shouldReturnOkAndDetails() throws Exception {
        // Given
        VehicleDetailDTO detail1 = new VehicleDetailDTO(1L, "ABC-123", "BrandA", "ModelA", 2020, "Client A");
        detail1.setRepairs(Arrays.asList(new RepairDTO(10L, 1L, "Desc", LocalDateTime.now(), null, BigDecimal.TEN, "Mec", "COMPLETED")));
        List<VehicleDetailDTO> mockDetails = Collections.singletonList(detail1);

        when(analyticsService.getVehicleRepairInvoiceDetails(any(), any(), any(), any(), any()))
                .thenReturn(mockDetails);

        // When & Then
        mockMvc.perform(get("/api/analytics/vehicles-details")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-01-31T23:59:59")
                        .param("licensePlate", "ABC-123")
                        .param("brand", "BrandA")
                        .param("clientName", "Client A")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value("ABC-123"))
                .andExpect(jsonPath("$[0].repairs[0].id").value(10L));

        verify(analyticsService, times(1)).getVehicleRepairInvoiceDetails(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 1, 31, 23, 59, 59),
                "ABC-123", "BrandA", "Client A"
        );
    }

    @Test
    @WithMockUser(roles={"USER"})
    void getVehicleRepairInvoiceDetails_shouldHandleNullableParams() throws Exception {
        // Given
        when(analyticsService.getVehicleRepairInvoiceDetails(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/analytics/vehicles-details")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(analyticsService, times(1)).getVehicleRepairInvoiceDetails(
                isNull(), isNull(), isNull(), isNull(), isNull()
        );
    }

    // --- Pruebas para /api/analytics/export/repairs-summary ---
    @Test
    @WithMockUser(roles={"ADMIN"})
    void exportRepairsSummaryToExcel_shouldReturnExcelFile() throws Exception {
        // Given
        VehicleDetailDTO detail = new VehicleDetailDTO(1L, "ABC-123", "Toyota", "Corolla", 2020, "Juan Perez");
        RepairDTO repair = new RepairDTO(1L, 1L, "Oil Change", LocalDateTime.now(), LocalDateTime.now(), BigDecimal.valueOf(50), "Pedro", "COMPLETED");
        detail.setRepairs(Collections.singletonList(repair));

        List<VehicleDetailDTO> vehicleDetails = Collections.singletonList(detail);
        List<RepairSummaryExcelRowDTO> excelRows = Arrays.asList(new RepairSummaryExcelRowDTO(
            "ABC-123", "Toyota", "Corolla", 2020, "Juan Perez", 1L, "Oil Change",
            LocalDateTime.now(), LocalDateTime.now(), BigDecimal.valueOf(50), "Pedro", "COMPLETED",
            null, null, BigDecimal.ZERO, null, false
        ));

        when(analyticsService.getVehicleRepairInvoiceDetails(any(), any(), any(), any(), any()))
                .thenReturn(vehicleDetails);
        when(analyticsService.convertToRepairSummaryExcelRows(anyList()))
                .thenReturn(excelRows);

        // Mock static method ExcelGenerator.generateExcel
        // Esto es un poco más complejo porque Mockito no mockea métodos estáticos directamente
        // Se puede usar PowerMockito o Mockito-inline (Java 8+) para esto.
        // Si no quieres complicarte, puedes testear esta integración en una prueba de integración real.
        // Para una unidad test, simulamos que el ExcelGenerator hace su trabajo.
        // Si ExcelGenerator es un bean de Spring, sería más fácil mockearlo.
        try (var mockedStatic = mockStatic(ExcelGenerator.class)) {
            mockedStatic.when(() -> ExcelGenerator.generateExcel(
                    anyList(), any(String[].class), any(ByteArrayOutputStream.class), any(Class.class)
            )).thenAnswer(invocation -> {
                // Simula que se escribe algo en el OutputStream
                ByteArrayOutputStream os = invocation.getArgument(2);
                os.write("excel_content".getBytes()); // Escribe contenido simulado
                return null;
            });

            // When & Then
            mockMvc.perform(get("/api/analytics/export/repairs-summary")
                            .param("startDate", "2024-01-01T00:00:00")
                            .param("endDate", "2024-01-31T23:59:59")
                            .accept(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;filename=\"resumen_reparaciones_")))
                    .andExpect(content().bytes("excel_content".getBytes()));

            verify(analyticsService, times(1)).getVehicleRepairInvoiceDetails(any(), any(), any(), any(), any());
            verify(analyticsService, times(1)).convertToRepairSummaryExcelRows(anyList());
            mockedStatic.verify(() -> ExcelGenerator.generateExcel(
                    eq(excelRows), any(String[].class), any(ByteArrayOutputStream.class), eq(RepairSummaryExcelRowDTO.class)
            ), times(1));
        }
    }

    @Test
    @WithMockUser(roles={"USER"})
    void exportInvoiceItemsDetailToExcel_shouldReturnExcelFile() throws Exception {
        // Given
        VehicleDetailDTO detail = new VehicleDetailDTO(1L, "ABC-123", "Toyota", "Corolla", 2020, "Juan Perez");
        RepairDTO repair = new RepairDTO(1L, 1L, "Oil Change", LocalDateTime.now(), LocalDateTime.now(), BigDecimal.valueOf(50), "Pedro", "COMPLETED");
        InvoiceDTO invoice = new InvoiceDTO(1L, "INV-001", 1L, 1L, 1L, LocalDateTime.now(), BigDecimal.valueOf(100), "Juan Perez", "Pedro", Arrays.asList(new InvoiceItemDTO(1L, "Oil Filter", "PART", BigDecimal.TEN, 1, BigDecimal.TEN)));
        repair.setInvoice(invoice);
        detail.setRepairs(Collections.singletonList(repair));

        List<VehicleDetailDTO> vehicleDetails = Collections.singletonList(detail);
        List<InvoiceItemExcelRowDTO> excelRows = Arrays.asList(new InvoiceItemExcelRowDTO(
            "ABC-123", "Toyota", "Corolla", 1L, "Oil Change", 1L, "INV-001", LocalDateTime.now(), BigDecimal.valueOf(100),
            1L, "Oil Filter", "PART", BigDecimal.TEN, 1, BigDecimal.TEN
        ));

        when(analyticsService.getVehicleRepairInvoiceDetails(any(), any(), any(), any(), any()))
                .thenReturn(vehicleDetails);
        when(analyticsService.convertToInvoiceItemDetailExcelRows(anyList()))
                .thenReturn(excelRows);

        try (var mockedStatic = mockStatic(ExcelGenerator.class)) {
            mockedStatic.when(() -> ExcelGenerator.generateExcel(
                    anyList(), any(String[].class), any(ByteArrayOutputStream.class), any(Class.class)
            )).thenAnswer(invocation -> {
                ByteArrayOutputStream os = invocation.getArgument(2);
                os.write("item_excel_content".getBytes());
                return null;
            });

            // When & Then
            mockMvc.perform(get("/api/analytics/export/invoice-items-detail")
                            .param("startDate", "2024-01-01T00:00:00")
                            .param("endDate", "2024-01-31T23:59:59")
                            .accept(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;filename=\"detalle_items_factura_")))
                    .andExpect(content().bytes("item_excel_content".getBytes()));

            verify(analyticsService, times(1)).getVehicleRepairInvoiceDetails(any(), any(), any(), any(), any());
            verify(analyticsService, times(1)).convertToInvoiceItemDetailExcelRows(anyList());
            mockedStatic.verify(() -> ExcelGenerator.generateExcel(
                    eq(excelRows), any(String[].class), any(ByteArrayOutputStream.class), eq(InvoiceItemExcelRowDTO.class)
            ), times(1));
        }
    }
}