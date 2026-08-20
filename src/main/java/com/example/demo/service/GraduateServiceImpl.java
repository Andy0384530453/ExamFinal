package com.example.demo.service;

import com.example.demo.dto.graduate.GraduateResponse;
import com.example.demo.dto.graduate.GraduatesResponse;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.entity.JUser;
import com.example.demo.excel.GraduateExcelGenerator;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.repository.JUserRepository;
import com.example.demo.validator.EntityValidator;
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

  private final JGroupRepository groupRepository;
  private final JStudentGroupRepository studentGroupRepository;
  private final JUserRepository userRepository;
  private final GraduateCalculator graduateCalculator;
  private final GraduateExcelGenerator excelGenerator;
  private final EntityValidator validator;

  public GraduateServiceImpl(
      JGroupRepository groupRepository,
      JStudentGroupRepository studentGroupRepository,
      JUserRepository userRepository,
      GraduateCalculator graduateCalculator,
      GraduateExcelGenerator excelGenerator,
      EntityValidator validator) {
    this.groupRepository = groupRepository;
    this.studentGroupRepository = studentGroupRepository;
    this.userRepository = userRepository;
    this.graduateCalculator = graduateCalculator;
    this.excelGenerator = excelGenerator;
    this.validator = validator;
  }

  @Override
  public List<GraduateResponse> getGraduates(UUID promotionId) {
    validator.assertPromotionExists(promotionId);
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
  public GraduatesResponse getGraduatesResponse(UUID promotionId) {
    JPromotion promotion = validator.requirePromotion(promotionId);
    return new GraduatesResponse(
        promotion.getId(), promotion.getRef(), promotion.getYear(), getGraduates(promotionId));
  }

  @Override
  public byte[] getGraduatesExcel(UUID promotionId) {
    JPromotion promotion = validator.requirePromotion(promotionId);
    return excelGenerator.generate(promotion, getGraduates(promotionId));
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
    return graduateCalculator
        .passingAverage(studentId, promotionId)
        .map(
            average ->
                new GraduateResponse(
                    student.getId(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getEmail(),
                    average));
  }
}
