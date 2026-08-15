package com.example.demo.service;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.transcript.TranscriptItemResponse;
import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.dto.transcript.TranscriptSendEmailResponse;
import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JTranscriptItem;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.enums.TranscriptStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TranscriptMapper;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JTranscriptRepository;
import com.example.demo.repository.JUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TranscriptServiceImpl implements TranscriptService {

  private final TokenProvider tokenProvider;
  private final JUserRepository userRepository;
  private final JPromotionRepository promotionRepository;
  private final JTranscriptRepository transcriptRepository;
  private final TranscriptMapper transcriptMapper;
  private final TranscriptDataBuilder transcriptDataBuilder;
  private final EventProducer<TranscriptEmailRequested> eventProducer;

  public TranscriptServiceImpl(
      TokenProvider tokenProvider,
      JUserRepository userRepository,
      JPromotionRepository promotionRepository,
      JTranscriptRepository transcriptRepository,
      TranscriptMapper transcriptMapper,
      TranscriptDataBuilder transcriptDataBuilder,
      EventProducer<TranscriptEmailRequested> eventProducer) {
    this.tokenProvider = tokenProvider;
    this.userRepository = userRepository;
    this.promotionRepository = promotionRepository;
    this.transcriptRepository = transcriptRepository;
    this.transcriptMapper = transcriptMapper;
    this.transcriptDataBuilder = transcriptDataBuilder;
    this.eventProducer = eventProducer;
  }

  @Override
  public TranscriptResponse getStudentTranscript(UUID studentId, UUID promotionId, Jwt jwt) {
    checkAccess(studentId, jwt);
    checkStudentExists(studentId);
    checkPromotionExists(promotionId);

    List<JTranscriptItem> items = transcriptDataBuilder.buildItems(studentId, promotionId);
    JTranscript transcript = resolveTranscript(studentId, promotionId);
    List<TranscriptItemResponse> itemResponses =
        items.stream().map(transcriptMapper::toItemResponse).toList();
    return transcriptMapper.toResponse(transcript, itemResponses);
  }

  @Override
  public TranscriptSendEmailResponse requestTranscriptEmail(UUID studentId, Jwt jwt) {
    checkAccess(studentId, jwt);
    JUser student = findStudentOrThrow(studentId);

    JTranscript transcript = resolveTranscript(studentId, null);
    transcript.setStatus(TranscriptStatus.PENDING);
    transcript.setEmail(student.getEmail());
    transcriptRepository.save(transcript);

    eventProducer.accept(
        List.of(TranscriptEmailRequested.builder().transcriptId(transcript.getId()).build()));

    return new TranscriptSendEmailResponse(
        transcript.getId(),
        TranscriptStatus.PENDING,
        "Processing in progress, the transcript will be sent by email.");
  }

  private void checkAccess(UUID studentId, Jwt jwt) {
    String role = tokenProvider.getRole(jwt);
    if (Role.ADMIN.name().equals(role)) {
      return;
    }
    if (Role.STUDENT.name().equals(role)) {
      UUID authenticatedId = UUID.fromString(tokenProvider.getUserId(jwt));
      if (!authenticatedId.equals(studentId)) {
        throw new AccessDeniedException("A student can only access their own transcript");
      }
      return;
    }
    throw new AccessDeniedException("Access denied: insufficient role");
  }

  private void checkStudentExists(UUID studentId) {
    findStudentOrThrow(studentId);
  }

  private JUser findStudentOrThrow(UUID studentId) {
    return userRepository
        .findById(studentId)
        .filter(user -> user.getRole() == Role.STUDENT)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student not found with id: " + studentId));
  }

  private void checkPromotionExists(UUID promotionId) {
    if (promotionId != null && !promotionRepository.existsById(promotionId)) {
      throw new ResourceNotFoundException("Promotion not found with id: " + promotionId);
    }
  }

  private JTranscript resolveTranscript(UUID studentId, UUID promotionId) {
    Optional<JTranscript> persisted =
        promotionId == null
            ? transcriptRepository.findByStudentIdAndPromotionIdIsNull(studentId)
            : transcriptRepository.findByStudentIdAndPromotionId(studentId, promotionId);
    if (persisted.isPresent()) {
      return persisted.get();
    }
    JTranscript transcript = new JTranscript();
    transcript.setId(UUID.randomUUID());
    transcript.setStudentId(studentId);
    transcript.setPromotionId(promotionId);
    transcript.setStatus(TranscriptStatus.PENDING);
    return transcript;
  }
}
