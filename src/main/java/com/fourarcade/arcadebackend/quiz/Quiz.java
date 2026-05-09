package com.fourarcade.arcadebackend.quiz;

import com.fourarcade.arcadebackend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50, nullable = false)
    private String title;

    @Column(length = 20, nullable = false)
    private QuizCategory category;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "play_count", nullable = false)
    private int playCount;


    // 퀴즈 수
    @Column(name = "question_count", nullable = false)
    private int questionCount; // 퀴즈에 포함된 총 문제 수

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public Quiz(User user, String title, QuizCategory category, boolean isPublic, int playCount) {
        this.user = user;
        this.title = title;
        this.category = category;
        this.isPublic = isPublic;
        this.playCount = playCount;
        this.questionCount = 0;
    }

    public void update(String title, QuizCategory category, boolean isPublic){
        this.title = title;
        this.category = category;
        this.isPublic = isPublic;
    }

    public void increaseQuestionCount(){
        this.questionCount++;
    }

    public void decreaseQuestionCount(){
        if(this.questionCount > 0){
            this.questionCount--;
        }
    }
}