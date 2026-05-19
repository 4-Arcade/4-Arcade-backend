package com.fourarcade.arcadebackend.quiz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    @EntityGraph(attributePaths= {"user"})
    Optional<Quiz> findWithUserById(UUID id);

    @EntityGraph(attributePaths = {"user"})
    Page<Quiz> findByIsPublicTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Quiz> findByIsPublicTrueAndCategory(QuizCategory category, Pageable pageable);

    Page<Quiz> findByUser_Id(UUID userId, Pageable pageable);

    // 공개 퀴즈 검색: title 또는 description에 keyword 포함
    @EntityGraph(attributePaths = {"user"})
    Page<Quiz> findByIsPublicTrueAndTitleContainingIgnoreCaseOrIsPublicTrueAndDescriptionContainingIgnoreCase(
            String titleKeyword,
            String descriptionKeyword,
            Pageable pageable
    );

    // 공개 퀴즈 검색 + 카테고리 필터
    @EntityGraph(attributePaths = {"user"})
    Page<Quiz> findByIsPublicTrueAndCategoryAndTitleContainingIgnoreCaseOrIsPublicTrueAndCategoryAndDescriptionContainingIgnoreCase(
            QuizCategory category1,
            String titleKeyword,
            QuizCategory category2,
            String descriptionKeyword,
            Pageable pageable
    );

    // 마이페이지 내 퀴즈 검색: 내가 만든 퀴즈 중 title 또는 description에 keyword 포함
    Page<Quiz> findByUser_IdAndTitleContainingIgnoreCaseOrUser_IdAndDescriptionContainingIgnoreCase(
            UUID userId1,
            String titleKeyword,
            UUID userId2,
            String descriptionKeyword,
            Pageable pageable
    );
}