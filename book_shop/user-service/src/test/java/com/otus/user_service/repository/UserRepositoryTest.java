package com.otus.user_service.repository;

import com.otus.user_service.entity.User;
import com.otus.user_service.enums.UserRoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@org.springframework.test.context.jdbc.Sql(scripts = "classpath:schema.sql", executionPhase = org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedpassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhone("+1234567890");
        user.setRole(UserRoleType.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    public void findByUsername_ExistingUsername_ReturnsUser() {
        // Given
        userRepository.save(user);

        // When
        Optional<User> result = userRepository.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        assertEquals(UserRoleType.CUSTOMER, result.get().getRole());
    }

    @Test
    public void findByUsername_NonExistingUsername_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexist");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void findByEmail_ExistingEmail_ReturnsUser() {
        // Given
        userRepository.save(user);

        // When
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("+1234567890", result.get().getPhone());
    }

    @Test
    public void findByEmail_NonExistingEmail_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexist@example.com");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void findByUsernameOrEmailIgnoreCase_ExistingUsername_ReturnsUser() {
        // Given
        userRepository.save(user);

        // When
        Optional<User> result = userRepository.findByUsernameOrEmailIgnoreCase("testuser", "other@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    public void findByUsernameOrEmailIgnoreCase_ExistingEmail_ReturnsUser() {
        // Given
        userRepository.save(user);

        // When
        Optional<User> result = userRepository.findByUsernameOrEmailIgnoreCase("otheruser", "test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    public void findByUsernameOrEmailIgnoreCase_NonExisting_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByUsernameOrEmailIgnoreCase("nonexist", "nonexist@example.com");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void findByRole_ExistingRole_ReturnsUsers() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.findByRole(UserRoleType.CUSTOMER);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    public void findByRole_NonExistingRole_ReturnsEmptyList() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.findByRole(UserRoleType.ADMIN);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByActiveTrue_ActiveUsers_ReturnsUsers() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.findByActiveTrue();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    @Test
    public void findByActiveFalse_InactiveUsers_ReturnsUsers() {
        // Given
        user.setActive(false);
        userRepository.save(user);

        // When
        List<User> result = userRepository.findByActiveFalse();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getActive());
    }

    @Test
    public void existsByUsername_ExistingUsername_ReturnsTrue() {
        // Given
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByUsername("testuser");

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsByUsername_NonExistingUsername_ReturnsFalse() {
        // When
        boolean exists = userRepository.existsByUsername("nonexist");

        // Then
        assertFalse(exists);
    }

    @Test
    public void existsByEmail_ExistingEmail_ReturnsTrue() {
        // Given
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsByEmail_NonExistingEmail_ReturnsFalse() {
        // When
        boolean exists = userRepository.existsByEmail("nonexist@example.com");

        // Then
        assertFalse(exists);
    }

    @Test
    public void existsByUsernameOrEmail_ExistingUsername_ReturnsTrue() {
        // Given
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByUsernameOrEmail("testuser", "other@example.com");

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsByUsernameOrEmail_ExistingEmail_ReturnsTrue() {
        // Given
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByUsernameOrEmail("otheruser", "test@example.com");

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsByUsernameOrEmail_NonExisting_ReturnsFalse() {
        // When
        boolean exists = userRepository.existsByUsernameOrEmail("nonexist", "nonexist@example.com");

        // Then
        assertFalse(exists);
    }

    @Test
    public void searchByName_PartialMatch_ReturnsUsers() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.searchByName("Test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    public void searchByName_NoMatch_ReturnsEmptyList() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.searchByName("Nonexistent");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void searchByEmail_PartialMatch_ReturnsUsers() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.searchByEmail("example");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    public void searchByEmail_NoMatch_ReturnsEmptyList() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.searchByEmail("nonexistent");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findAllOrderByCreatedAtDesc_NewestFirst_ReturnsUsers() {
        // Given
        User olderUser = new User();
        olderUser.setUsername("olderuser");
        olderUser.setEmail("older@example.com");
        olderUser.setPassword("password");
        olderUser.setCreatedAt(LocalDateTime.now().minusDays(2));
        userRepository.save(olderUser);
        userRepository.save(user);

        // When
        List<User> result = userRepository.findAllOrderByCreatedAtDesc();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("testuser", result.get(0).getUsername()); // Newest first
        assertEquals("olderuser", result.get(1).getUsername());
    }

    @Test
    public void findByCreatedAtAfter_FutureDate_ReturnsEmptyList() {
        // Given
        userRepository.save(user);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        // When
        List<User> result = userRepository.findByCreatedAtAfter(futureDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByCreatedAtAfter_PastDate_ReturnsUsers() {
        // Given
        userRepository.save(user);
        LocalDateTime pastDate = user.getCreatedAt().minusHours(1);

        // When
        List<User> result = userRepository.findByCreatedAtAfter(pastDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    public void findByCreatedAtBefore_FutureDate_ReturnsUsers() {
        // Given
        userRepository.save(user);
        LocalDateTime futureDate = user.getCreatedAt().plusDays(1);

        // When
        List<User> result = userRepository.findByCreatedAtBefore(futureDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    public void findByCreatedAtBefore_PastDate_ReturnsEmptyList() {
        // Given
        userRepository.save(user);
        LocalDateTime pastDate = user.getCreatedAt().minusDays(2);

        // When
        List<User> result = userRepository.findByCreatedAtBefore(pastDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByCreatedAtBetween_ValidRange_ReturnsUsers() {
        // Given
        userRepository.save(user);
        LocalDateTime startDate = user.getCreatedAt().minusHours(1);
        LocalDateTime endDate = user.getCreatedAt().plusHours(1);

        // When
        List<User> result = userRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    public void findByCreatedAtBetween_OutOfRange_ReturnsEmptyList() {
        // Given
        userRepository.save(user);
        LocalDateTime startDate = user.getCreatedAt().plusDays(1);
        LocalDateTime endDate = user.getCreatedAt().plusDays(2);

        // When
        List<User> result = userRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getTotalUserCount_ReturnsTotalCount() {
        // Given
        userRepository.save(user);

        // When
        long count = userRepository.getTotalUserCount();

        // Then
        assertEquals(1L, count);
    }

    @Test
    public void getActiveUserCount_ActiveUsers_ReturnsCount() {
        // Given
        userRepository.save(user);

        // When
        long count = userRepository.getActiveUserCount();

        // Then
        assertEquals(1L, count);
    }

    @Test
    public void getActiveUserCount_InactiveUsers_ReturnsZero() {
        // Given
        user.setActive(false);
        userRepository.save(user);

        // When
        long count = userRepository.getActiveUserCount();

        // Then
        assertEquals(0L, count);
    }

    @Test
    public void countByRole_ExistingRole_ReturnsCount() {
        // Given
        userRepository.save(user);

        // When
        long count = userRepository.countByRole(UserRoleType.CUSTOMER);

        // Then
        assertEquals(1L, count);
    }

    @Test
    public void countByRole_NonExistingRole_ReturnsZero() {
        // Given
        userRepository.save(user);

        // When
        long count = userRepository.countByRole(UserRoleType.ADMIN);

        // Then
        assertEquals(0L, count);
    }

    @Test
    public void getUserRoleStatistics_ReturnsStatistics() {
        // Given
        userRepository.save(user);

        // When
        List<Object[]> result = userRepository.getUserRoleStatistics();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        Object[] stats = result.get(0);
        assertEquals(UserRoleType.CUSTOMER, stats[0]);
        assertEquals(1L, stats[1]);
    }

    @Test
    public void getRegistrationStatistics_ValidDays_ReturnsStatistics() {
        // Given
        userRepository.save(user);
        userRepository.flush();
        entityManager.clear();

        // When
        List<Object[]> result = userRepository.getRegistrationStatistics(30);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        Object[] stats = result.get(0);
        // The date should be today's date
        assertEquals(user.getCreatedAt().toLocalDate(), ((java.sql.Date) stats[0]).toLocalDate());
        assertEquals(1L, stats[1]);
    }

    @Test
    public void activateUser_ExistingUser_ActivatesUser() {
        // Given
        user.setActive(false);
        User savedUser = userRepository.save(user);
        userRepository.flush();
        entityManager.clear();

        // When
        int updatedRows = userRepository.activateUser(savedUser.getId());

        // Then
        assertEquals(1, updatedRows);
        Optional<User> updatedUser = userRepository.findById(savedUser.getId());
        assertTrue(updatedUser.isPresent());
        assertTrue(updatedUser.get().getActive());
    }

    @Test
    public void deactivateUser_ExistingUser_DeactivatesUser() {
        // Given
        User savedUser = userRepository.save(user);
        userRepository.flush();
        entityManager.clear();
        // When
        int updatedRows = userRepository.deactivateUser(savedUser.getId());
        // Then
        assertEquals(1, updatedRows);
        Optional<User> updatedUser = userRepository.findById(savedUser.getId());
        assertTrue(updatedUser.isPresent());
        assertFalse(updatedUser.get().getActive());
    }

    @Test
    public void updateUserRole_ExistingUser_UpdatesRole() {
        // Given
        User savedUser = userRepository.save(user);
        userRepository.flush();
        entityManager.clear();
        // When
        int updatedRows = userRepository.updateUserRole(savedUser.getId(), UserRoleType.ADMIN);
        // Then
        assertEquals(1, updatedRows);
        Optional<User> updatedUser = userRepository.findById(savedUser.getId());
        assertTrue(updatedUser.isPresent());
        assertEquals(UserRoleType.ADMIN, updatedUser.get().getRole());
    }

    @Test
    public void deleteInactiveUsersOlderThan_ValidDate_DeletesUsers() {
        // Given
        user.setActive(false);
        user.setCreatedAt(LocalDateTime.now().minusDays(400));
        User savedUser = userRepository.save(user);
        userRepository.flush();
        entityManager.clear();

        // When
        int deletedRows = userRepository.deleteInactiveUsersOlderThan(LocalDateTime.now().minusDays(365));

        // Then
        assertEquals(1, deletedRows);
        assertFalse(userRepository.findById(savedUser.getId()).isPresent());
    }

    @Test
    public void deleteInactiveUsersOlderThan_NoMatchingUsers_ReturnsZero() {
        // Given
        User savedUser = userRepository.save(user);

        // When
        int deletedRows = userRepository.deleteInactiveUsersOlderThan(LocalDateTime.now().plusDays(1));

        // Then
        assertEquals(0, deletedRows);
        assertTrue(userRepository.findById(savedUser.getId()).isPresent());
    }

    @Test
    public void deleteByUsername_ExistingUsername_DeletesUser() {
        // Given
        User savedUser = userRepository.save(user);
        userRepository.flush();
        entityManager.clear();

        // When
        int deletedRows = userRepository.deleteByUsername("testuser");

        // Then
        assertEquals(1, deletedRows);
        assertFalse(userRepository.findById(savedUser.getId()).isPresent());
    }

    @Test
    public void deleteByUsername_NonExistingUsername_ReturnsZero() {
        // When
        int deletedRows = userRepository.deleteByUsername("nonexist");

        // Then
        assertEquals(0, deletedRows);
    }

    @Test
    public void findByPhoneContaining_PartialMatch_ReturnsUsers() {
        // Given
        userRepository.save(user);

        // When
        List<User> result = userRepository.findByPhoneContaining("123");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }
}