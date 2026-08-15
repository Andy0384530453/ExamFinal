package com.example.demo.repository;

import com.example.demo.entity.JUser;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JUserRepository extends JpaRepository<JUser, UUID> {}
