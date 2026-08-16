package com.example.demo.repository;

import com.example.demo.entity.JGradeModification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JGradeModificationRepository extends JpaRepository<JGradeModification, UUID> {

  List<JGradeModification> findByGradeIdOrderByModifiedAtDesc(UUID gradeId);
}
