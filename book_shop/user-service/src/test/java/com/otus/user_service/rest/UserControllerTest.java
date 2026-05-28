package com.otus.user_service.rest;

import com.otus.user_service.dto.*;
import com.otus.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserRegistrationDTO registrationDTO;
    private UserLoginDTO loginDTO;
    private UserUpdateDTO updateDTO;
    private ChangePasswordDTO changePasswordDTO;
    private UserResponseDTO userResponseDTO;
    private LoginResponseDTO loginResponseDTO;

    @BeforeEach
    public void setUp() {
        // Setup registration DTO
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("testuser");
        registrationDTO.setEmail("test@example.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setFirstName("Test");
        registrationDTO.setLastName("User");
        registrationDTO.setPhone("+1234567890");

        // Setup login DTO
        loginDTO = new UserLoginDTO();
        loginDTO.setUsernameOrEmail("testuser");
        loginDTO.setPassword("password123");

        // Setup update DTO
        updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("User");
        updateDTO.setPhone("+0987654321");

        // Setup change password DTO
        changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setCurrentPassword("password123");
        changePasswordDTO.setNewPassword("newpassword123");
        changePasswordDTO.setConfirmPassword("newpassword123");

        // Setup user response DTO
        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(1L);
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");
        userResponseDTO.setFirstName("Test");
        userResponseDTO.setLastName("User");
        userResponseDTO.setPhone("+1234567890");
        userResponseDTO.setRole("CUSTOMER");
        userResponseDTO.setActive(true);
        userResponseDTO.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Setup login response DTO
        loginResponseDTO = new LoginResponseDTO(true, "Login successful", userResponseDTO);
    }

    @Test
    public void register_ValidRequest_RegistersUser() throws Exception {
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(userResponseDTO);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\",\"firstName\":\"Test\",\"lastName\":\"User\",\"phone\":\"+1234567890\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    public void register_InvalidRequest_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"email\":\"invalid\",\"password\":\"\",\"firstName\":\"\",\"lastName\":\"\",\"phone\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any(UserRegistrationDTO.class));
    }

    @Test
    public void login_ValidCredentials_ReturnsSuccessResponse() throws Exception {
        when(userService.login(any(UserLoginDTO.class))).thenReturn(loginResponseDTO);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.user.username").value("testuser"));

        verify(userService, times(1)).login(any(UserLoginDTO.class));
    }

    @Test
    public void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginResponseDTO failureResponse = new LoginResponseDTO(false, "Invalid credentials", null);
        when(userService.login(any(UserLoginDTO.class))).thenReturn(failureResponse);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"testuser\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(userService, times(1)).login(any(UserLoginDTO.class));
    }

    @Test
    public void getUser_ExistingId_ReturnsUser() throws Exception {
        when(userService.getUser(1L)).thenReturn(userResponseDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.phone").value("+1234567890"));

        verify(userService, times(1)).getUser(1L);
    }

    @Test
    public void getUser_NonExistingId_ReturnsNotFound() throws Exception {
        when(userService.getUser(999L)).thenReturn(null);

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUser(999L);
    }

    @Test
    public void getUserByEmail_ExistingEmail_ReturnsUser() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(userResponseDTO);

        mockMvc.perform(get("/api/users/email/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).getUserByEmail("test@example.com");
    }

    @Test
    public void getUserByEmail_NonExistingEmail_ReturnsNotFound() throws Exception {
        when(userService.getUserByEmail("nonexist@example.com")).thenReturn(null);

        mockMvc.perform(get("/api/users/email/nonexist@example.com"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserByEmail("nonexist@example.com");
    }

    @Test
    public void getAllUsers_ReturnsAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(Arrays.asList(userResponseDTO));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("testuser"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    public void updateUser_ExistingUser_UpdatesUser() throws Exception {
        UserResponseDTO updatedUserDTO = new UserResponseDTO();
        updatedUserDTO.setId(1L);
        updatedUserDTO.setId(1L);
        updatedUserDTO.setFirstName("Updated");
        updatedUserDTO.setLastName("User");
        updatedUserDTO.setPhone("+0987654321");

        when(userService.updateUser(eq(1L), any(UserUpdateDTO.class))).thenReturn(updatedUserDTO);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Updated\",\"lastName\":\"User\",\"phone\":\"+0987654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phone").value("+0987654321"));

        verify(userService, times(1)).updateUser(eq(1L), any(UserUpdateDTO.class));
    }

    @Test
    public void updateUser_NonExistingUser_ReturnsNotFound() throws Exception {
        when(userService.updateUser(eq(999L), any(UserUpdateDTO.class))).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Updated\",\"lastName\":\"User\",\"phone\":\"+0987654321\"}"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq(999L), any(UserUpdateDTO.class));
    }

    @Test
    public void changePassword_ValidRequest_ChangesPassword() throws Exception {
        when(userService.changePassword(eq(1L), any(ChangePasswordDTO.class))).thenReturn(true);

        mockMvc.perform(post("/api/users/1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123\",\"newPassword\":\"newpassword123\",\"confirmPassword\":\"newpassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(userService, times(1)).changePassword(eq(1L), any(ChangePasswordDTO.class));
    }

    @Test
    public void changePassword_InvalidRequest_ReturnsBadRequest() throws Exception {
        when(userService.changePassword(eq(1L), any(ChangePasswordDTO.class))).thenThrow(new RuntimeException("Current password is incorrect"));

        mockMvc.perform(post("/api/users/1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrongpassword\",\"newPassword\":\"newpassword123\",\"confirmPassword\":\"newpassword123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password is incorrect"));

        verify(userService, times(1)).changePassword(eq(1L), any(ChangePasswordDTO.class));
    }

    @Test
    public void deactivateUser_ExistingUser_DeactivatesUser() throws Exception {
        doNothing().when(userService).deactivateUser(1L);

        mockMvc.perform(post("/api/users/1/deactivate"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deactivateUser(1L);
    }

    @Test
   public void activateUser_ExistingUser_ActivatesUser() throws Exception {
        doNothing().when(userService).activateUser(1L);

        mockMvc.perform(post("/api/users/1/activate"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).activateUser(1L);
    }

    @Test
   public void clearCache_ClearsUserCache() throws Exception {
        doNothing().when(userService).clearCache();

        mockMvc.perform(delete("/api/users/cache"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).clearCache();
    }

    @Test
   public void health_ReturnsHealthStatus() throws Exception {
        mockMvc.perform(get("/api/users/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("user-service"));
    }
}