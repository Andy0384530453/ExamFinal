package com.example.demo.repository;

import com.example.demo.entity.JGrade;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JGradeRepository extends JpaRepository<JGrade, UUID> {

  List<JGrade> findByStudentId(UUID studentId);

  List<JGrade> findByExamIdIn(List<UUID> examIds);
}