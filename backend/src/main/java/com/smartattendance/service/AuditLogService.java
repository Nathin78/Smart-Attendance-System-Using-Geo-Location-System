package com.smartattendance.service;

import com.smartattendance.dto.AuditLogResponse;
import com.smartattendance.entity.AuditLog;
import com.smartattendance.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String actorEmail, String action, String targetType, Long targetId, String details) {
        AuditLog log = new AuditLog();
        log.setActorEmail(actorEmail);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> getRecentLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::map)
                .toList();
    }

    private AuditLogResponse map(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setActorEmail(log.getActorEmail());
        response.setAction(log.getAction());
        response.setTargetType(log.getTargetType());
        response.setTargetId(log.getTargetId());
        response.setDetails(log.getDetails());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
