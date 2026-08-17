package com.example.demo.repository;

import com.example.demo.entity.JPromotion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JPromotionRepository extends JpaRepository<JPromotion, UUID> {

  java.util.Optional<JPromotion> findByYear(int year);
}
