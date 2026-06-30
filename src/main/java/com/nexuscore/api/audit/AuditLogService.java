package com.nexuscore.api.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(String resource, String action, String performedBy, String details) {
        AuditLog entry = AuditLog.builder()
                .resource(resource)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(entry);
        log.info("[AUDITORÍA] recurso={} acción={} usuario={} detalle={}", resource, action, performedBy, details);
    }
}
