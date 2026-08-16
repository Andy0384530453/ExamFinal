package com.example.demo.repository;

import com.example.demo.entity.JGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JGroupRepository extends JpaRepository<JGroup, UUID> {

  List<JGroup> findByPromotionId(UUID promotionId);
}
