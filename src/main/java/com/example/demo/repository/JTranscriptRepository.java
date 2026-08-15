package com.example.demo.repository;

import com.example.demo.entity.JTranscript;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JTranscriptRepository extends JpaRepository<JTranscript, UUID> {

  Optional<JTranscript> findByStudentIdAndPromotionId(UUID studentId, UUID promotionId);

  Optional<JTranscript> findByStudentIdAndPromotionIdIsNull(UUID studentId);
}
