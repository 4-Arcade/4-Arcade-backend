package com.fourarcade.arcadebackend.quiz;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    @EntityGraph(attributePaths= {"user"})
    Optional<Quiz> findWithUserById(UUID id);
}