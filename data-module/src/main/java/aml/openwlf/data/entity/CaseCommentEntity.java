package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 케이스 코멘트 엔티티
 * 케이스에 대한 분석가의 코멘트와 메모를 저장합니다.
 */
@Entity
@Table(name = "case_comments", indexes = {
        @Index(name = "idx_comment_case_id", columnList = "case_id"),
        @Index(name = "idx_comment_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseCommentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 연결된 케이스
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseEntity caseEntity;
    
    /**
     * 코멘트 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * 코멘트 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false, length = 30)
    private CommentType commentType;
    
    /**
     * 내부 전용 여부 (true: 내부용, false: 고객에게도 공개 가능)
     */
    @Column(name = "is_internal")
    @Builder.Default
    private Boolean isInternal = true;
    
    /**
     * 작성자
     */
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    /**
     * 작성일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 수정일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (commentType == null) {
            commentType = CommentType.NOTE;
        }
        if (isInternal == null) {
            isInternal = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 코멘트 유형
     */
    public enum CommentType {
        NOTE,           // 일반 메모
        ANALYSIS,       // 분석 내용
        FINDING,        // 발견 사항
        RECOMMENDATION, // 권고 사항
        ESCALATION,     // 에스컬레이션 사유
        DECISION,       // 최종 결정 사유
        SYSTEM          // 시스템 자동 생성
    }
}
