package dev.tr7zw.mango2j.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(indexes = {
        @Index(name = "idx_internal_task_status", columnList = "status"),
        @Index(name = "idx_internal_task_created", columnList = "createdAt")
})
public class InternalTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter
    @Setter
    private String processorKey;

    @Getter
    @Setter
    @Column(length = 200_000)
    private String payloadJson;

    @Getter
    @Setter
    private Boolean internalTask = false;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private InternalTaskStatus status = InternalTaskStatus.QUEUED;

    @Getter
    @Setter
    private Integer progressCurrent = 0;

    @Getter
    @Setter
    private Integer progressTotal = 0;

    @Getter
    @Setter
    @Column(length = 2000)
    private String message;

    @Getter
    @Setter
    @Column(length = 10000)
    private String error;

    @Getter
    @Setter
    private Instant createdAt;

    @Getter
    @Setter
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
