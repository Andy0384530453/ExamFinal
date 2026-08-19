package com.example.demo.repository;

import com.example.demo.entity.JCourseGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JCourseGroupRepository extends JpaRepository<JCourseGroup, UUID> {

  List<JCourseGroup> findByCourseId(UUID courseId);

  Optional<JCourseGroup> findByCourseIdAndGroupId(UUID courseId, UUID groupId);
}
