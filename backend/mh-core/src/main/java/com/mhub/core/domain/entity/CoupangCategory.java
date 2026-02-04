package com.mhub.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupang_category")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoupangCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "display_category_code", nullable = false, unique = true)
    private Long displayCategoryCode;

    @Column(name = "display_category_name", nullable = false)
    private String displayCategoryName;

    @Column(name = "parent_category_code")
    private Long parentCategoryCode;

    @Column(name = "depth_level", nullable = false)
    private Integer depthLevel;

    @Column(name = "root_category_code")
    private Long rootCategoryCode;

    @Column(name = "root_category_name")
    private String rootCategoryName;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (syncedAt == null) {
            syncedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
