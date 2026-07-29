package com.hisabkitab.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class WorkDtos {

    private WorkDtos() {
    }

    public record TaskRequest(
            @NotBlank(message = "Task name is required")
            @Size(max = 160, message = "Name is too long")
            String name,

            @Size(max = 160, message = "Location is too long")
            String location,

            @NotBlank(message = "Unit of work is required")
            @Size(max = 32, message = "Unit is too long")
            String unitOfWork,

            @NotNull(message = "Price per unit is required")
            @DecimalMin(value = "0.0", message = "Price cannot be negative")
            BigDecimal pricePerUnit,

            @Size(max = 1000, message = "Notes are too long")
            String notes,

            Boolean active) {
    }

    /**
     * @param recordCount how much work has been logged against this task, so the
     *                    UI can explain why it archives rather than deletes
     */
    public record TaskView(
            Long id,
            String name,
            String location,
            String unitOfWork,
            BigDecimal pricePerUnit,
            String notes,
            boolean active,
            long recordCount) {
    }

    public record LogWorkRequest(
            @NotNull(message = "Task is required")
            Long workTaskId,

            @NotNull(message = "Worker is required")
            Long employeeId,

            @NotNull(message = "Date is required")
            LocalDate workDate,

            @NotNull(message = "Quantity is required")
            @DecimalMin(value = "0.001", message = "Quantity must be more than zero")
            @Digits(integer = 11, fraction = 3, message = "Quantity has too many digits")
            BigDecimal quantity,

            /**
             * Overrides the task's current price for this record only. Leave null
             * to use the task price.
             */
            @DecimalMin(value = "0.0", message = "Price cannot be negative")
            BigDecimal unitPrice,

            @Size(max = 500, message = "Note is too long")
            String note) {
    }

    public record WorkRecordView(
            Long id,
            Long employeeId,
            String employeeName,
            Long workTaskId,
            String taskName,
            String location,
            String unitOfWork,
            LocalDate workDate,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String note,
            /**
             * Everything this worker has logged on this task for this date,
             * including the entry just saved. Null when not computed. Lets the
             * screen show the running total after a second batch is added.
             */
            BigDecimal dayTotalQuantity) {
    }

    public record WorkSummary(
            LocalDate from,
            LocalDate to,
            BigDecimal totalAmount,
            List<WorkRecordView> records) {
    }

    /** One task with every worker's running total against it. */
    public record TaskSummary(
            TaskView task,
            BigDecimal totalQuantity,
            BigDecimal totalAmount,
            List<TaskWorkerTotal> workers) {
    }

    public record TaskWorkerTotal(
            Long employeeId,
            String employeeName,
            BigDecimal quantity,
            BigDecimal amount,
            long entries,
            LocalDate lastWorkedOn) {
    }
}
