package com.example.demo.dto.transcript;

import com.example.demo.enums.TranscriptStatus;
import java.util.UUID;

public record TranscriptSendEmailResponse(
    UUID transcriptId, TranscriptStatus status, String message) {}
