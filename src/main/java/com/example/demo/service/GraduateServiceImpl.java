package com.example.demo.service;

import com.example.demo.dto.graduate.GraduateResponse;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.entity.JUser;
import com.example.demo.excel.GraduateExcelGenerator;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.repository.JUserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GraduateServiceImpl implements GraduateService {

  private static final double PASSING_AVERAGE = 10.0;

  private final JPromotionRepository promotionRepository;
  private final JGroupRepository groupRepository;
  private final JStudentGroupRepository studentGroupRepository;
  private final JUserRepository userRepository;
  private final TranscriptDataBuilder transcriptDataBuilder;
  private final GraduateExcelGenerator excelGenerator;

  public GraduateServiceImpl(
      JPromotionRepository promotionRepository,
      JGroupRepository groupRepository,
      JStudentGroupRepository studentGroupRepository,
      JUserRepository userRepository,
      TranscriptDataBuilder transcriptDataBuilder,
      GraduateExcelGenerator excelGenerator) {
    this.promotionRepository = promotionRepository;
    this.groupRepository = groupRepository;
    this.studentGroupRepository = studentGroupRepository;
    this.userRepository = userRepository;
    this.transcriptDataBuilder = transcriptDataBuilder;
    this.excelGenerator = excelGenerator;
  }

  @Override
  public List<GraduateResponse> getGraduates(UUID promotionId) {
    checkPromotionExists(promotionId);
    List<UUID> finishedStudentIds = findFinishedStudentIds(promotionId);
    if (finishedStudentIds.isEmpty()) {
      return List.of();
    }
    Map<UUID, JUser> studentsById = loadStudentsById(finishedStudentIds);
    return finishedStudentIds.stream()
        .map(studentId -> toGraduate(studentId, studentsById.get(studentId), promotionId))
        .flatMap(Optional::stream)
        .sorted(
            Comparator.comparing(GraduateResponse::lastName)
                .thenComparing(GraduateResponse::firstName))
        .toList();
  }

  @Override
  public byte[] getGraduatesExcel(UUID promotionId) {
    var promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Promotion not found with id: " + promotionId));
    return excelGenerator.generate(promotion, getGraduates(promotionId));
  }

  private void checkPromotionExists(UUID promotionId) {
    if (!promotionRepository.existsById(promotionId)) {
      throw new ResourceNotFoundException("Promotion not found with id: " + promotionId);
    }
  }

  private List<UUID> findFinishedStudentIds(UUID promotionId) {
    List<JGroup> groups = groupRepository.findByPromotionId(promotionId);
    if (groups.isEmpty()) {
      return List.of();
    }
    List<UUID> groupIds = groups.stream().map(JGroup::getId).toList();
    return studentGroupRepository.findByGroupIdIn(groupIds).stream()
        .filter(membership -> membership.getEndDate() != null)
        .map(JStudentGroup::getStudentId)
        .distinct()
        .toList();
  }

  private Map<UUID, JUser> loadStudentsById(List<UUID> studentIds) {
    return userRepository.findAllById(studentIds).stream()
        .collect(Collectors.toMap(JUser::getId, Function.identity()));
  }

  private Optional<GraduateResponse> toGraduate(UUID studentId, JUser student, UUID promotionId) {
    if (student == null) {
      return Optional.empty();
    }
    double average = computeAverage(studentId, promotionId);
    if (average < PASSING_AVERAGE) {
      return Optional.empty();
    }
    return Optional.of(
        new GraduateResponse(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            student.getEmail(),
            round(average)));
  }

  private double computeAverage(UUID studentId, UUID promotionId) {
    return transcriptDataBuilder.buildItems(studentId, promotionId).stream()
        .mapToDouble(item -> item.getGrade() == null ? 0.0 : item.getGrade())
        .average()
        .orElse(0.0);
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
