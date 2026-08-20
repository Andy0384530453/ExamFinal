package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.graduate.GraduateResponse;
import com.example.demo.dto.graduate.GraduatesResponse;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.excel.GraduateExcelGenerator;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.repository.JUserRepository;
import com.example.demo.validator.EntityValidator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraduateServiceImplTest {

  private JPromotionRepository promotionRepository;
  private JGroupRepository groupRepository;
  private JStudentGroupRepository studentGroupRepository;
  private JUserRepository userRepository;
  private GraduateCalculator graduateCalculator;
  private GraduateExcelGenerator excelGenerator;
  private GraduateServiceImpl service;

  @BeforeEach
  void setUp() {
    promotionRepository = mock(JPromotionRepository.class);
    groupRepository = mock(JGroupRepository.class);
    studentGroupRepository = mock(JStudentGroupRepository.class);
    userRepository = mock(JUserRepository.class);
    graduateCalculator = mock(GraduateCalculator.class);
    excelGenerator = mock(GraduateExcelGenerator.class);
    EntityValidator validator =
        new EntityValidator(
            null, groupRepository, userRepository, promotionRepository, null, null, null);
    service =
        new GraduateServiceImpl(
            groupRepository,
            studentGroupRepository,
            userRepository,
            graduateCalculator,
            excelGenerator,
            validator);
  }

  @Test
  void returns_graduates_with_passing_average() {
    UUID promotionId = UUID.randomUUID();
    JGroup group = group(promotionId);
    JUser student = student();
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of(group));
    when(studentGroupRepository.findByGroupIdIn(List.of(group.getId())))
        .thenReturn(List.of(membership(student.getId(), group.getId(), Instant.now())));
    when(userRepository.findAllById(List.of(student.getId()))).thenReturn(List.of(student));
    when(graduateCalculator.passingAverage(student.getId(), promotionId))
        .thenReturn(Optional.of(13.0));

    List<GraduateResponse> graduates = service.getGraduates(promotionId);

    assertThat(graduates).hasSize(1);
    GraduateResponse graduate = graduates.get(0);
    assertThat(graduate.studentId()).isEqualTo(student.getId());
    assertThat(graduate.firstName()).isEqualTo(student.getFirstName());
    assertThat(graduate.lastName()).isEqualTo(student.getLastName());
    assertThat(graduate.email()).isEqualTo(student.getEmail());
    assertThat(graduate.average()).isEqualTo(13.0);
  }

  @Test
  void excludes_students_still_studying() {
    UUID promotionId = UUID.randomUUID();
    JGroup group = group(promotionId);
    JUser student = student();
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of(group));
    when(studentGroupRepository.findByGroupIdIn(List.of(group.getId())))
        .thenReturn(List.of(membership(student.getId(), group.getId(), null)));

    List<GraduateResponse> graduates = service.getGraduates(promotionId);

    assertThat(graduates).isEmpty();
  }

  @Test
  void excludes_students_with_low_average() {
    UUID promotionId = UUID.randomUUID();
    JGroup group = group(promotionId);
    JUser student = student();
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of(group));
    when(studentGroupRepository.findByGroupIdIn(List.of(group.getId())))
        .thenReturn(List.of(membership(student.getId(), group.getId(), Instant.now())));
    when(userRepository.findAllById(List.of(student.getId()))).thenReturn(List.of(student));
    when(graduateCalculator.passingAverage(student.getId(), promotionId))
        .thenReturn(Optional.empty());

    List<GraduateResponse> graduates = service.getGraduates(promotionId);

    assertThat(graduates).isEmpty();
  }

  @Test
  void unknown_promotion_throws_not_found() {
    UUID promotionId = UUID.randomUUID();
    when(promotionRepository.existsById(promotionId)).thenReturn(false);

    assertThatThrownBy(() -> service.getGraduates(promotionId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void promotion_without_groups_returns_empty() {
    UUID promotionId = UUID.randomUUID();
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of());

    List<GraduateResponse> graduates = service.getGraduates(promotionId);

    assertThat(graduates).isEmpty();
  }

  @Test
  void graduates_response_wraps_promotion_information() {
    UUID promotionId = UUID.randomUUID();
    JPromotion promotion = promotion();
    promotion.setId(promotionId);
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of());

    GraduatesResponse response = service.getGraduatesResponse(promotionId);

    assertThat(response.promotionId()).isEqualTo(promotionId);
    assertThat(response.promotionRef()).isEqualTo(promotion.getRef());
    assertThat(response.promotionYear()).isEqualTo(promotion.getYear());
    assertThat(response.graduates()).isEmpty();
  }

  @Test
  void graduates_response_contains_computed_graduates() {
    UUID promotionId = UUID.randomUUID();
    JPromotion promotion = promotion();
    promotion.setId(promotionId);
    JGroup group = group(promotionId);
    JUser student = student();
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of(group));
    when(studentGroupRepository.findByGroupIdIn(List.of(group.getId())))
        .thenReturn(List.of(membership(student.getId(), group.getId(), Instant.now())));
    when(userRepository.findAllById(List.of(student.getId()))).thenReturn(List.of(student));
    when(graduateCalculator.passingAverage(student.getId(), promotionId))
        .thenReturn(Optional.of(13.5));

    GraduatesResponse response = service.getGraduatesResponse(promotionId);

    assertThat(response.promotionRef()).isEqualTo(promotion.getRef());
    assertThat(response.graduates()).hasSize(1);
    assertThat(response.graduates().get(0).studentId()).isEqualTo(student.getId());
    assertThat(response.graduates().get(0).average()).isEqualTo(13.5);
  }

  @Test
  void graduates_response_unknown_promotion_throws_not_found() {
    UUID promotionId = UUID.randomUUID();
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getGraduatesResponse(promotionId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void excel_returns_generated_bytes() {
    UUID promotionId = UUID.randomUUID();
    JPromotion promotion = promotion();
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of());
    when(excelGenerator.generate(promotion, List.of())).thenReturn(new byte[] {1, 2, 3});

    byte[] content = service.getGraduatesExcel(promotionId);

    assertThat(content).containsExactly(1, 2, 3);
  }

  private static JGroup group(UUID promotionId) {
    JGroup group = new JGroup();
    group.setId(UUID.randomUUID());
    group.setRef("GRP");
    group.setPromotionId(promotionId);
    return group;
  }

  private static JStudentGroup membership(UUID studentId, UUID groupId, Instant endDate) {
    JStudentGroup membership = new JStudentGroup();
    membership.setId(UUID.randomUUID());
    membership.setStudentId(studentId);
    membership.setGroupId(groupId);
    membership.setStartDate(Instant.parse("2023-09-01T08:00:00Z"));
    membership.setEndDate(endDate);
    return membership;
  }

  private static JUser student() {
    JUser user = new JUser();
    user.setId(UUID.randomUUID());
    user.setRef("REF");
    user.setFirstName("First");
    user.setLastName("Last");
    user.setEmail("student@school.com");
    user.setRole(Role.STUDENT);
    return user;
  }

  private static JPromotion promotion() {
    JPromotion promotion = new JPromotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-2024");
    promotion.setYear(2024);
    return promotion;
  }
}
