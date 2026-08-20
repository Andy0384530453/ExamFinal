package com.example.demo.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.enums.Role;
import com.example.demo.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;

  @Test
  void login_with_valid_credentials_returns_200_with_token() throws Exception {
    UUID userId = UUID.randomUUID();
    LoginResponse response =
        new LoginResponse(
            "jwt-token-abc", userId, "Alice", "Dupont", "alice@test.com", Role.STUDENT);

    when(authService.login(any(LoginRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token-abc"))
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.firstName").value("Alice"))
        .andExpect(jsonPath("$.lastName").value("Dupont"))
        .andExpect(jsonPath("$.email").value("alice@test.com"))
        .andExpect(jsonPath("$.role").value("STUDENT"));
  }

  @Test
  void login_with_missing_email_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"password123\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_with_missing_password_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_with_invalid_email_format_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"password123\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_with_empty_body_returns_400() throws Exception {
    mockMvc
        .perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_with_teacher_role_returns_teacher_role() throws Exception {
    UUID userId = UUID.randomUUID();
    LoginResponse response =
        new LoginResponse(
            "jwt-token-teach", userId, "Bob", "Martin", "bob@test.com", Role.TEACHER);

    when(authService.login(any(LoginRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bob@test.com\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("TEACHER"))
        .andExpect(jsonPath("$.token").value("jwt-token-teach"));
  }
}
