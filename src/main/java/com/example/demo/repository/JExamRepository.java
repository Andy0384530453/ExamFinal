package com.example.demo.repository;

import com.example.demo.entity.JExam;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JExamRepository extends JpaRepository<JExam, UUID> {

  List<JExam> findByCourseIdIn(List<UUID> courseIds);
}