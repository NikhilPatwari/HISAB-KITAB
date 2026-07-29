package com.hisabkitab.service;

import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.WorkRecord;
import com.hisabkitab.domain.WorkTask;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.AppUserRepository;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.repository.WorkRecordRepository;
import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.web.dto.WorkDtos.LogWorkRequest;
import com.hisabkitab.web.dto.WorkDtos.TaskSummary;
import com.hisabkitab.web.dto.WorkDtos.TaskWorkerTotal;
import com.hisabkitab.web.dto.WorkDtos.WorkRecordView;
import com.hisabkitab.web.dto.WorkDtos.WorkSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class WorkRecordService {

    private final WorkRecordRepository records;
    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final EmployeeService employeeService;
    private final WorkTaskService taskService;
    private final OrganizationService organizationService;

    public WorkRecordService(WorkRecordRepository records,
                             OrganizationRepository organizations,
                             AppUserRepository users,
                             EmployeeService employeeService,
                             WorkTaskService taskService,
                             OrganizationService organizationService) {
        this.records = records;
        this.organizations = organizations;
        this.users = users;
        this.employeeService = employeeService;
        this.taskService = taskService;
        this.organizationService = organizationService;
    }

    /**
     * Logs completed units. Any employee may do piece work regardless of type —
     * a permanent worker picking cotton at the weekend earns both.
     */
    @Transactional
    public WorkRecordView log(AuthPrincipal principal, LogWorkRequest request) {
        Long orgId = principal.organizationId();
        Employee employee = employeeService.require(orgId, request.employeeId());
        WorkTask task = taskService.require(orgId, request.workTaskId());

        if (!task.isActive()) {
            throw new ApiExceptions.BadRequestException(
                    task.getName() + " is archived. Reactivate it before logging work.");
        }
        LocalDate today = organizationService.today(orgId);
        if (request.workDate().isAfter(today)) {
            throw new ApiExceptions.BadRequestException("Date cannot be in the future");
        }
        if (request.workDate().isBefore(employee.getJoinedOn())) {
            throw new ApiExceptions.BadRequestException(
                    "Date is before " + employee.getName() + " joined on " + employee.getJoinedOn());
        }

        // Snapshot the price: piece work is agreed at a rate for the job, so
        // next season's price must not reprice this record.
        BigDecimal unitPrice = request.unitPrice() != null
                ? Money.scale(request.unitPrice())
                : task.getPricePerUnit();
        BigDecimal quantity = request.quantity().setScale(3, RoundingMode.HALF_UP);

        // Several deliveries a day are normal — a morning batch and an afternoon
        // one — so each entry is its own row and the day's pay is their sum.
        // Keeping them separate preserves the price and note each was agreed at.
        WorkRecord record = new WorkRecord();
        record.setOrganization(organizations.getReferenceById(orgId));
        record.setEmployee(employee);
        record.setWorkTask(task);
        record.setWorkDate(request.workDate());
        record.setQuantity(quantity);
        record.setUnitPrice(unitPrice);
        record.setAmount(Money.scale(quantity.multiply(unitPrice)));
        record.setNote(blankToNull(request.note()));
        record.setCreatedBy(users.getReferenceById(principal.userId()));

        return toView(records.save(record), dayTotalFor(employee.getId(), task.getId(), request.workDate()));
    }

    /** The running total for that worker on that job that day, including this entry. */
    private BigDecimal dayTotalFor(Long employeeId, Long taskId, LocalDate date) {
        return records.findForEmployeeBetween(employeeId, date, date).stream()
                .filter(r -> r.getWorkTask().getId().equals(taskId))
                .map(WorkRecord::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Deleting is allowed here, unlike ledger entries: a work record is a
     * measurement rather than a money movement, and it only becomes money when
     * the month closes. Removing one before then is a correction, not a rewrite.
     */
    @Transactional
    public void delete(Long organizationId, Long recordId) {
        WorkRecord record = records.findByIdAndOrganizationId(recordId, organizationId)
                .orElseThrow(() -> ApiExceptions.NotFoundException.of("Work record", recordId));
        records.delete(record);
    }

    /** Either filter may be null, which widens rather than excludes. */
    @Transactional(readOnly = true)
    public WorkSummary list(Long organizationId, Long employeeId, Long taskId,
                            LocalDate from, LocalDate to) {
        if (employeeId != null) {
            employeeService.require(organizationId, employeeId);
        }
        List<WorkRecordView> rows =
                records.findFiltered(organizationId, employeeId, taskId, from, to)
                        .stream().map(WorkRecordService::toView).toList();
        return summarise(from, to, rows);
    }

    /**
     * A task with each worker's cumulative output. This is the view that answers
     * "who has picked how much, and what do we owe them for it".
     */
    @Transactional(readOnly = true)
    public TaskSummary taskSummary(Long organizationId, Long taskId) {
        WorkTask task = taskService.require(organizationId, taskId);

        List<TaskWorkerTotal> workers = records.totalsByEmployeeForTask(taskId).stream()
                .map(row -> new TaskWorkerTotal(
                        row.getEmployeeId(),
                        row.getEmployeeName(),
                        row.getQuantity(),
                        Money.nullToZero(row.getAmount()),
                        row.getEntries(),
                        row.getLastWorkedOn()))
                .toList();

        BigDecimal totalQuantity = workers.stream()
                .map(TaskWorkerTotal::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = workers.stream()
                .map(TaskWorkerTotal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TaskSummary(
                WorkTaskService.toView(task, records.countByWorkTaskId(taskId)),
                totalQuantity,
                Money.scale(totalAmount),
                workers);
    }

    private static WorkSummary summarise(LocalDate from, LocalDate to, List<WorkRecordView> rows) {
        BigDecimal total = rows.stream()
                .map(WorkRecordView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WorkSummary(from, to, Money.scale(total), rows);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static WorkRecordView toView(WorkRecord record) {
        return toView(record, null);
    }

    static WorkRecordView toView(WorkRecord record, BigDecimal dayTotalQuantity) {
        WorkTask task = record.getWorkTask();
        return new WorkRecordView(
                record.getId(),
                record.getEmployee().getId(),
                record.getEmployee().getName(),
                task.getId(),
                task.getName(),
                task.getLocation(),
                task.getUnitOfWork(),
                record.getWorkDate(),
                record.getQuantity(),
                record.getUnitPrice(),
                record.getAmount(),
                record.getNote(),
                dayTotalQuantity);
    }
}
