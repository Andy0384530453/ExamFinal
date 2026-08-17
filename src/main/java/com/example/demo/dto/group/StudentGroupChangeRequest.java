package com.example.demo.dto.group;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record StudentGroupChangeRequest(@NotNull UUID groupId, LocalDate startDate) {}
