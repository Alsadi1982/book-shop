package com.otus.user_service.service;

import com.otus.user_service.dto.*;
import com.otus.user_service.entity.User;
import com.otus.user_service.enums.UserRoleType;
import com.otus.user_service.mapper.UserMapper;
import com.otus.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDTO registrationDTO;
    private UserLoginDTO loginDTO;
    private UserUpdateDTO updateDTO;
    private ChangePasswordDTO changePasswordDTO;
    private User user;
    private UserResponseDTO userResponseDTO;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

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

        // Setup user
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhone("+1234567890");
        user.setRole(UserRoleType.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));

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
        userResponseDTO.setCreatedAt(user.getCreatedAt());
    }

    @Test
    public void registerUser_ValidRequest_RegistersUser() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userMapper.toEntity(registrationDTO)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.registerUser(registrationDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toResponseDTO(any(User.class));
    }

    @Test
    public void registerUser_UsernameExists_ThrowsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registrationDTO);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void registerUser_EmailExists_ThrowsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registrationDTO);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
   public void login_ValidCredentials_ReturnsSuccessResponse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        LoginResponseDTO result = userService.login(loginDTO);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Login successful", result.getMessage());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    public void login_ValidEmailCredentials_ReturnsSuccessResponse() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        loginDTO.setUsernameOrEmail("test@example.com");
        LoginResponseDTO result = userService.login(loginDTO);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Login successful", result.getMessage());
        assertNotNull(result.getUser());
        assertEquals("test@example.com", result.getUser().getEmail());
        verify(userRepository, times(1)).findByUsername("test@example.com");
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    public void login_InvalidUsername_ReturnsFailureResponse() {
        when(userRepository.findByUsername("invaliduser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("invaliduser")).thenReturn(Optional.empty());

        loginDTO.setUsernameOrEmail("invaliduser");
        LoginResponseDTO result = userService.login(loginDTO);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Invalid credentials", result.getMessage());
        assertNull(result.getUser());
        verify(userRepository, times(1)).findByUsername("invaliduser");
        verify(userRepository, times(1)).findByEmail("invaliduser");
    }

    @Test
    public void login_InvalidPassword_ReturnsFailureResponse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        loginDTO.setPassword("wrongpassword");
        LoginResponseDTO result = userService.login(loginDTO);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Invalid credentials", result.getMessage());
        assertNull(result.getUser());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    public void login_DeactivatedAccount_ReturnsFailureResponse() {
        user.setActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginResponseDTO result = userService.login(loginDTO);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Account is deactivated", result.getMessage());
        assertNull(result.getUser());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
   public void getUser_ExistingId_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUser(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
        verify(userMapper, times(1)).toResponseDTO(user);
    }

    @Test
   public void getUser_NonExistingId_ReturnsNull() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserResponseDTO result = userService.getUser(999L);

        assertNull(result);
        verify(userRepository, times(1)).findById(999L);
        verify(userMapper, never()).toResponseDTO(any(User.class));
    }

    @Test
   public void getUserByEmail_ExistingEmail_ReturnsUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUserByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userMapper, times(1)).toResponseDTO(user);
    }

    @Test
   public void getUserByEmail_NonExistingEmail_ReturnsNull() {
        when(userRepository.findByEmail("nonexist@example.com")).thenReturn(Optional.empty());

        UserResponseDTO result = userService.getUserByEmail("nonexist@example.com");

        assertNull(result);
        verify(userRepository, times(1)).findByEmail("nonexist@example.com");
        verify(userMapper, never()).toResponseDTO(any(User.class));
    }

    @Test
   public void getAllUsers_ReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        java.util.List<UserResponseDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
   public void updateUser_ExistingUser_UpdatesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.updateUser(1L, updateDTO);

        assertNotNull(result);
        assertEquals("Test", result.getFirstName());
        assertEquals("User", result.getLastName());
        assertEquals("+1234567890", result.getPhone());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toResponseDTO(any(User.class));
    }

    @Test
   public void updateUser_NonExistingUser_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(999L, updateDTO);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).toResponseDTO(any(User.class));
    }

    @Test
   public void changePassword_ValidRequest_ChangesPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        boolean result = userService.changePassword(1L, changePasswordDTO);

        assertTrue(result);
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
   public void changePassword_PasswordMismatch_ThrowsException() {
        changePasswordDTO.setConfirmPassword("differentpassword");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword(1L, changePasswordDTO);
        });

        assertEquals("New password and confirmation do not match", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
   public void changePassword_NonExistingUser_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword(999L, changePasswordDTO);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
   public void changePassword_InvalidCurrentPassword_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        changePasswordDTO.setCurrentPassword("wrongpassword");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword(1L, changePasswordDTO);
        });

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
   public void deactivateUser_ExistingUser_DeactivatesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deactivateUser(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
        assertFalse(user.getActive());
    }

    @Test
    public void deactivateUser_NonExistingUser_DoesNothing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        userService.deactivateUser(999L);

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
   public void activateUser_ExistingUser_ActivatesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.activateUser(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
        assertTrue(user.getActive());
    }

    @Test
   public void activateUser_NonExistingUser_DoesNothing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        userService.activateUser(999L);

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void getUserWithManualCache_CacheMiss_LoadsFromRepository() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUserWithManualCache(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findById(1L);
        verify(userMapper, times(1)).toResponseDTO(user);
    }

    @Test
   public void getUserWithManualCache_CacheHit_ReturnsFromCache() {
        // First call to populate cache
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);
        userService.getUserWithManualCache(1L);

        // Second call should use cache
        UserResponseDTO result = userService.getUserWithManualCache(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }
}