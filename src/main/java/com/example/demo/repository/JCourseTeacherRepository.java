package com.example.demo.repository;

import com.example.demo.entity.JCourseTeacher;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JCourseTeacherRepository extends JpaRepository<JCourseTeacher, UUID> {

  List<JCourseTeacher> findByTeacherId(UUID teacherId);
}