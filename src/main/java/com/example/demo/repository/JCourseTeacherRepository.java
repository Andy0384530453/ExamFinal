package com.example.demo.repository;

import com.example.demo.entity.JCourseTeacher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JCourseTeacherRepository extends JpaRepository<JCourseTeacher, UUID> {

  boolean existsByTeacherIdAndCourseId(UUID teacherId, UUID courseId);

  Optional<JCourseTeacher> findByCourseIdAndTeacherId(UUID courseId, UUID teacherId);

  List<JCourseTeacher> findByCourseId(UUID courseId);

  void deleteByCourseIdAndTeacherId(UUID courseId, UUID teacherId);
}
