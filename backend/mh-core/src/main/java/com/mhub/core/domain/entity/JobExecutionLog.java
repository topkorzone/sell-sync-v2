package com.mhub.core.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "job_execution_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
