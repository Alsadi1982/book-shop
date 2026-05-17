package com.otus.user_service.service;

import com.otus.user_service.dto.*;
import com.otus.user_service.entity.User;
import com.otus.user_service.enums.UserRoleType;
import com.otus.user_service.mapper.UserMapper;
import com.otus.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Ручное кэширование с java.util.concurrent
    private final ConcurrentHashMap<Long, UserResponseDTO> userCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserResponseDTO> userByEmailCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    @Transactional
    public UserResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        logger.info("Registering new user: {}", registrationDTO.getUsername());

        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            logger.warn("Username already exists: {}", registrationDTO.getUsername());
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            logger.warn("Email already exists: {}", registrationDTO.getEmail());
            throw new RuntimeException("Email already exists");
        }

        User user = userMapper.toEntity(registrationDTO);
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setRole(UserRoleType.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        UserResponseDTO responseDTO = userMapper.toResponseDTO(savedUser);

        cacheLock.writeLock().lock();
        try {
            userCache.put(savedUser.getId(), responseDTO);
            userByEmailCache.put(savedUser.getEmail(), responseDTO);
        } finally {
            cacheLock.writeLock().unlock();
        }

        return responseDTO;
    }

    public LoginResponseDTO login(UserLoginDTO loginDTO) {
        logger.info("Login attempt: {}", loginDTO.getUsernameOrEmail());

        User user = userRepository.findByUsername(loginDTO.getUsernameOrEmail())
                .orElseGet(() -> userRepository.findByEmail(loginDTO.getUsernameOrEmail()).orElse(null));

        if (user == null) {
            logger.warn("Login failed - user not found: {}", loginDTO.getUsernameOrEmail());
            return new LoginResponseDTO(false, "Invalid credentials", null);
        }

        if (!user.getActive()) {
            logger.warn("Login failed - account deactivated: {}", user.getUsername());
            return new LoginResponseDTO(false, "Account is deactivated", null);
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            logger.warn("Login failed - invalid password: {}", user.getUsername());
            return new LoginResponseDTO(false, "Invalid credentials", null);
        }


        userRepository.save(user);

        logger.info("User logged in successfully: {}", user.getUsername());

        return new LoginResponseDTO(true, "Login successful", userMapper.toResponseDTO(user));
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO getUser(Long id) {
        logger.debug("Fetching user by ID: {}", id);

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            logger.warn("User not found with ID: {}", id);
            return null;
        }

        return userMapper.toResponseDTO(user);
    }

    @Cacheable(value = "usersByEmail", key = "#email")
    public UserResponseDTO getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            logger.warn("User not found with email: {}", email);
            return null;
        }

        return userMapper.toResponseDTO(user);
    }

    public List<UserResponseDTO> getAllUsers() {
        logger.info("Fetching all users");

        List<User> users = userRepository.findAll();
        logger.debug("Found {} users", users.size());

        return users.stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public UserResponseDTO updateUser(Long id, UserUpdateDTO updateDTO) {
        logger.info("Updating user ID: {}", id);

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            logger.warn("User not found for update: {}", id);
            throw new RuntimeException("User not found");
        }

        userMapper.updateEntity(user, updateDTO);

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully: {} (ID: {})", updatedUser.getUsername(), updatedUser.getId());

        UserResponseDTO responseDTO = userMapper.toResponseDTO(updatedUser);

        cacheLock.writeLock().lock();
        try {
            userCache.put(id, responseDTO);
            userByEmailCache.put(updatedUser.getEmail(), responseDTO);
        } finally {
            cacheLock.writeLock().unlock();
        }

        return responseDTO;
    }

    @Transactional
    public boolean changePassword(Long id, ChangePasswordDTO changePasswordDTO) {
        logger.info("Password change request for user ID: {}", id);

        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            logger.warn("Password mismatch for user ID: {}", id);
            throw new RuntimeException("New password and confirmation do not match");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            logger.warn("User not found for password change: {}", id);
            throw new RuntimeException("User not found");
        }

        if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
            logger.warn("Invalid current password for user: {}", user.getUsername());
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        userRepository.save(user);

        logger.info("Password changed successfully for user: {}", user.getUsername());
        return true;
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deactivateUser(Long id) {
        logger.info("Deactivating user ID: {}", id);

        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setActive(false);
            userRepository.save(user);

            cacheLock.writeLock().lock();
            try {
                userCache.remove(id);
                userByEmailCache.remove(user.getEmail());
            } finally {
                cacheLock.writeLock().unlock();
            }

            logger.info("User deactivated: {} (ID: {})", user.getUsername(), id);
        } else {
            logger.warn("User not found for deactivation: {}", id);
        }
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void activateUser(Long id) {
        logger.info("Activating user ID: {}", id);

        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setActive(true);
            userRepository.save(user);
            logger.info("User activated: {} (ID: {})", user.getUsername(), id);
        } else {
            logger.warn("User not found for activation: {}", id);
        }
    }

    public UserResponseDTO getUserWithManualCache(Long id) {
        cacheLock.readLock().lock();
        try {
            UserResponseDTO cached = userCache.get(id);
            if (cached != null) {
                logger.debug("Returning user from cache: {}", id);
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            UserResponseDTO responseDTO = userMapper.toResponseDTO(user);
            cacheLock.writeLock().lock();
            try {
                userCache.put(id, responseDTO);
                userByEmailCache.put(user.getEmail(), responseDTO);
            } finally {
                cacheLock.writeLock().unlock();
            }
            return responseDTO;
        }
        return null;
    }

    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            userCache.clear();
            userByEmailCache.clear();
            logger.info("User cache cleared");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
