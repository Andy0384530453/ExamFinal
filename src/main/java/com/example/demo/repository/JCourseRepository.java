package com.example.demo.repository;

import com.example.demo.entity.JCourse;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JCourseRepository extends JpaRepository<JCourse, UUID> {}
