package com.example.demo.dto.graduate;

import java.util.List;
import java.util.UUID;

public record GraduatesResponse(
    UUID promotionId, String promotionRef, Integer promotionYear, List<GraduateResponse> graduates) {}
