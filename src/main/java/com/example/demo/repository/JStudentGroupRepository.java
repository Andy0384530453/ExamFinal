package com.example.demo.repository;

import com.example.demo.entity.JStudentGroup;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JStudentGroupRepository extends JpaRepository<JStudentGroup, UUID> {

  List<JStudentGroup> findByGroupIdIn(Collection<UUID> groupIds);

  List<JStudentGroup> findByStudentIdOrderByStartDateDesc(UUID studentId);

  Optional<JStudentGroup> findByStudentIdAndEndDateIsNull(UUID studentId);
}
