package com.hisabkitab.web;

import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.service.OrganizationService;
import com.hisabkitab.service.WorkRecordService;
import com.hisabkitab.service.WorkTaskService;
import com.hisabkitab.web.dto.WorkDtos.LogWorkRequest;
import com.hisabkitab.web.dto.WorkDtos.TaskRequest;
import com.hisabkitab.web.dto.WorkDtos.TaskSummary;
import com.hisabkitab.web.dto.WorkDtos.TaskView;
import com.hisabkitab.web.dto.WorkDtos.WorkRecordView;
import com.hisabkitab.web.dto.WorkDtos.WorkSummary;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkController {

    private final WorkTaskService taskService;
    private final WorkRecordService recordService;
    private final OrganizationService organizationService;

    public WorkController(WorkTaskService taskService,
                          WorkRecordService recordService,
                          OrganizationService organizationService) {
        this.taskService = taskService;
        this.recordService = recordService;
        this.organizationService = organizationService;
    }

    // --- Task definitions -------------------------------------------------

    @GetMapping("/tasks")
    public List<TaskView> listTasks(@AuthenticationPrincipal AuthPrincipal principal,
                                    @RequestParam(defaultValue = "false") boolean activeOnly) {
        return taskService.list(principal.organizationId(), activeOnly);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public TaskView createTask(@AuthenticationPrincipal AuthPrincipal principal,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.create(principal.organizationId(), request);
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public TaskView updateTask(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(principal.organizationId(), id, request);
    }

    /** Archives rather than deletes, so logged work keeps resolving to a task. */
    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void archiveTask(@AuthenticationPrincipal AuthPrincipal principal,
                            @PathVariable Long id) {
        taskService.archive(principal.organizationId(), id);
    }

    // --- Work done --------------------------------------------------------

    @PostMapping("/work")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkRecordView logWork(@AuthenticationPrincipal AuthPrincipal principal,
                                  @Valid @RequestBody LogWorkRequest request) {
        return recordService.log(principal, request);
    }

    @GetMapping("/work")
    public WorkSummary listWork(@AuthenticationPrincipal AuthPrincipal principal,
                                @RequestParam(required = false) Long employeeId,
                                @RequestParam(required = false) Long taskId,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate today = organizationService.today(principal.organizationId());
        // Narrowing to a task or a worker implies wanting their whole history,
        // not just this month.
        LocalDate defaultStart = (taskId != null || employeeId != null)
                ? today.minusYears(5)
                : today.withDayOfMonth(1);

        return recordService.list(
                principal.organizationId(),
                employeeId,
                taskId,
                from != null ? from : defaultStart,
                to != null ? to : today);
    }

    /** One task with every worker's cumulative output against it. */
    @GetMapping("/tasks/{id}/summary")
    public TaskSummary taskSummary(@AuthenticationPrincipal AuthPrincipal principal,
                                   @PathVariable Long id) {
        return recordService.taskSummary(principal.organizationId(), id);
    }

    @DeleteMapping("/work/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWork(@AuthenticationPrincipal AuthPrincipal principal,
                           @PathVariable Long id) {
        recordService.delete(principal.organizationId(), id);
    }
}
