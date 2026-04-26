package com.fourarcade.arcadebackend.question;

import com.fourarcade.arcadebackend.quiz.Quiz;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "youtube_url", nullable = false, length = 255)
    private String youtubeUrl;

    @Column(name = "start_sec", nullable = false)
    private int startSec;

    @Column(name = "end_sec", nullable = false)
    private int endSec;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "answers", nullable = false, columnDefinition = "text[]")
    private String[] answers;

    @Column(name = "hint", length = 20)
    private String hint;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Question(
            Quiz quiz,
            String youtubeUrl,
            int startSec,
            int endSec,
            String[] answers,
            String hint,
            int orderIndex
    ) {
        this.quiz = quiz;
        this.youtubeUrl = youtubeUrl;
        this.startSec = startSec;
        this.endSec = endSec;
        this.answers = answers;
        this.hint = hint;
        this.orderIndex = orderIndex;
    }
    public void update(
            String youtubeUrl,
            int startSec,
            int endSec,
            String[] answers,
            String hint
    ) {
        this.youtubeUrl = youtubeUrl;
        this.startSec = startSec;
        this.endSec = endSec;
        this.answers = answers;
        this.hint = hint;
    }
    public void updateOrderIndex(int orderIndex){
        this.orderIndex = orderIndex;
    }
}