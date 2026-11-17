package com.logistics.controller;

import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.DashboardSummary;
import com.logistics.entity.enums.ShipmentStatus;
import com.logistics.service.ReportService;
import com.logistics.service.ReportService.ReportFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Every report accepts from, to, warehouseId, driverId, vehicleId, status. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary() {
        return ApiResponse.ok(reportService.summary());
    }

    @GetMapping("/shipments-by-day")
    public ApiResponse<List<Map<String, Object>>> shipmentsByDay(@ModelAttribute FilterParams p) {
        return ApiResponse.ok(reportService.shipmentsByDay(p.toFilter()));
    }

    @GetMapping("/on-time")
    public ApiResponse<List<Map<String, Object>>> onTime(@ModelAttribute FilterParams p) {
        return ApiResponse.ok(reportService.onTime(p.toFilter()));
    }

    @GetMapping("/driver-performance")
    public ApiResponse<List<Map<String, Object>>> driverPerformance(@ModelAttribute FilterParams p) {
        return ApiResponse.ok(reportService.driverPerformance(p.toFilter()));
    }

    @GetMapping("/vehicle-utilisation")
    public ApiResponse<List<Map<String, Object>>> vehicleUtilisation(@ModelAttribute FilterParams p) {
        return ApiResponse.ok(reportService.vehicleUtilisation(p.toFilter()));
    }

    @GetMapping("/maintenance-cost")
    public ApiResponse<List<Map<String, Object>>> maintenanceCost(@ModelAttribute FilterParams p) {
        return ApiResponse.ok(reportService.maintenanceCost(p.toFilter()));
    }

    @GetMapping("/pending-by-warehouse")
    public ApiResponse<List<Map<String, Object>>> pendingByWarehouse(@ModelAttribute FilterParams p) {
        return ApiResponse.ok(reportService.pendingByWarehouse(p.toFilter()));
    }

    /** /api/reports/export?report=driver-performance&type=csv|pdf&from=...&to=... */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "shipments-by-day") String report,
                                         @RequestParam(defaultValue = "csv") String type,
                                         @ModelAttribute FilterParams p) {
        ReportService.ExportFile file = reportService.export(report, type, p.toFilter());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    /** Query-string binding for the report filters. */
    @lombok.Data
    public static class FilterParams {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate from;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate to;
        private Long warehouseId;
        private Long driverId;
        private Long vehicleId;
        private ShipmentStatus status;

        ReportFilter toFilter() {
            return new ReportFilter(from, to, warehouseId, driverId, vehicleId, status);
        }
    }
}
