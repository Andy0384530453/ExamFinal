package com.example.demo.repository;

import com.example.demo.entity.Transcript;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {

  Optional<Transcript> findByStudentIdAndPromotionId(UUID studentId, UUID promotionId);

  Optional<Transcript> findByStudentIdAndPromotionIdIsNull(UUID studentId);
}
