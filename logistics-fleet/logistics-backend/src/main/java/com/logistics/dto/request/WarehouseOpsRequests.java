package com.logistics.dto.request;

import com.logistics.entity.enums.ConditionStatus;
import com.logistics.entity.enums.PackingType;
import com.logistics.entity.enums.WarehouseTxnType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/** Receive -> pack -> ready -> load -> dispatch. */
public final class WarehouseOpsRequests {

    private WarehouseOpsRequests() {
    }

    @Data
    public static class Receive {
        @NotBlank(message = "Tracking number is required")
        private String trackingNumber;

        @NotNull(message = "Package count is required")
        @Min(value = 1, message = "Package count must be at least 1")
        private Integer packageCount;

        @NotNull(message = "Condition is required")
        private ConditionStatus conditionStatus;

        @Size(max = 500)
        private String remarks;
    }

    @Data
    public static class Pack {
        @NotNull(message = "Select a package")
        private Long packageId;

        @NotNull(message = "Packing type is required")
        private PackingType packingType;

        private boolean labelPrinted;

        @Size(max = 500)
        private String remarks;
    }

    @Data
    public static class Ready {
        @NotNull(message = "Select a shipment")
        private Long shipmentId;

        @NotBlank(message = "Storage bin / location is required")
        @Size(max = 30)
        private String storageLocation;

        @Size(max = 500)
        private String remarks;
    }

    @Data
    public static class Loading {
        @NotNull(message = "Select an assignment")
        private Long assignmentId;

        @NotNull(message = "Transaction type is required")
        private WarehouseTxnType transactionType;

        @NotNull
        @Min(value = 1, message = "Package count must be at least 1")
        private Integer packageCount;

        @Size(max = 500)
        private String remarks;
    }

    @Data
    public static class Dispatch {
        @NotNull(message = "Select a shipment")
        private Long shipmentId;

        private LocalDateTime dispatchTime;

        @NotBlank(message = "Gate pass number is required")
        @Size(max = 30)
        private String gatePassNumber;

        @Size(max = 500)
        private String remarks;
    }
}
