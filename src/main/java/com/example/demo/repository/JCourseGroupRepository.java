package com.example.demo.repository;

import com.example.demo.entity.JCourseGroup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JCourseGroupRepository extends JpaRepository<JCourseGroup, UUID> {}
