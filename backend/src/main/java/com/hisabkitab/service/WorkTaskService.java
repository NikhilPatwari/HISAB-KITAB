package com.hisabkitab.service;

import com.hisabkitab.domain.WorkTask;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.repository.WorkRecordRepository;
import com.hisabkitab.repository.WorkTaskRepository;
import com.hisabkitab.web.dto.WorkDtos.TaskRequest;
import com.hisabkitab.web.dto.WorkDtos.TaskView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkTaskService {

    private final WorkTaskRepository tasks;
    private final WorkRecordRepository records;
    private final OrganizationRepository organizations;

    public WorkTaskService(WorkTaskRepository tasks,
                           WorkRecordRepository records,
                           OrganizationRepository organizations) {
        this.tasks = tasks;
        this.records = records;
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public List<TaskView> list(Long organizationId, boolean activeOnly) {
        List<WorkTask> rows = activeOnly
                ? tasks.findByOrganizationIdAndActiveOrderByNameAsc(organizationId, true)
                : tasks.findByOrganizationIdOrderByNameAsc(organizationId);
        return rows.stream()
                .map(task -> toView(task, records.countByWorkTaskId(task.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkTask require(Long organizationId, Long taskId) {
        return tasks.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> ApiExceptions.NotFoundException.of("Task", taskId));
    }

    @Transactional
    public TaskView create(Long organizationId, TaskRequest request) {
        WorkTask task = new WorkTask();
        task.setOrganization(organizations.getReferenceById(organizationId));
        apply(task, request);
        return toView(tasks.save(task), 0);
    }

    @Transactional
    public TaskView update(Long organizationId, Long taskId, TaskRequest request) {
        WorkTask task = require(organizationId, taskId);
        apply(task, request);
        // Changing the price affects future records only; existing ones keep the
        // price they were entered at.
        return toView(tasks.save(task), records.countByWorkTaskId(taskId));
    }

    /** Archived rather than deleted so logged work keeps resolving to a task. */
    @Transactional
    public void archive(Long organizationId, Long taskId) {
        WorkTask task = require(organizationId, taskId);
        task.setActive(false);
        tasks.save(task);
    }

    private void apply(WorkTask task, TaskRequest request) {
        task.setName(request.name().trim());
        task.setLocation(blankToNull(request.location()));
        task.setUnitOfWork(request.unitOfWork().trim());
        task.setPricePerUnit(Money.scale(request.pricePerUnit()));
        task.setNotes(blankToNull(request.notes()));
        if (request.active() != null) {
            task.setActive(request.active());
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static TaskView toView(WorkTask task, long recordCount) {
        return new TaskView(
                task.getId(),
                task.getName(),
                task.getLocation(),
                task.getUnitOfWork(),
                task.getPricePerUnit(),
                task.getNotes(),
                task.isActive(),
                recordCount);
    }
}
