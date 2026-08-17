package com.example.demo.dto.group;

import java.time.Instant;
import java.util.UUID;

public record StudentGroupResponse(
    UUID id, UUID studentId, UUID groupId, Instant startDate, Instant endDate) {}
