package com.otus.user_service.repository;

import com.otus.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ==================== Базовые методы поиска ====================

    /**
     * Найти пользователя по username (логину)
     * @param username имя пользователя
     * @return Optional с пользователем или пустой Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * Найти пользователя по email
     * @param email электронная почта
     * @return Optional с пользователем или пустой Optional
     */
    Optional<User> findByEmail(String email);

    /**
     * Найти пользователя по username или email (регистронезависимо)
     * @param username имя пользователя
     * @param email электронная почта
     * @return Optional с пользователем
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username) OR LOWER(u.email) = LOWER(:email)")
    Optional<User> findByUsernameOrEmailIgnoreCase(@Param("username") String username, @Param("email") String email);

    /**
     * Найти всех пользователей с определенной ролью
     * @param role роль пользователя
     * @return список пользователей
     */
    List<User> findByRole(UserRoleType role);

    /**
     * Найти всех активных пользователей
     * @return список активных пользователей
     */
    List<User> findByActiveTrue();

    /**
     * Найти всех неактивных пользователей
     * @return список неактивных пользователей
     */
    List<User> findByActiveFalse();

    // ==================== Проверка существования ====================

    /**
     * Проверить существование пользователя по username
     * @param username имя пользователя
     * @return true если существует, false если нет
     */
    boolean existsByUsername(String username);

    /**
     * Проверить существование пользователя по email
     * @param email электронная почта
     * @return true если существует, false если нет
     */
    boolean existsByEmail(String email);

    /**
     * Проверить существование пользователя по username или email
     * @param username имя пользователя
     * @param email электронная почта
     * @return true если существует, false если нет
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username OR u.email = :email")
    boolean existsByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    // ==================== Поиск по частичным совпадениям ====================

    /**
     * Поиск пользователей по части имени или фамилии (регистронезависимо)
     * @param name часть имени или фамилии
     * @return список пользователей
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> searchByName(@Param("name") String name);

    /**
     * Поиск пользователей по email (частичное совпадение)
     * @param emailDomain часть email
     * @return список пользователей
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    List<User> searchByEmail(@Param("email") String email);

    // ==================== Запросы с сортировкой и пагинацией ====================

    /**
     * Найти всех пользователей, отсортированных по дате регистрации (новые первыми)
     * @return список пользователей
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();

    /**
     * Найти всех пользователей, отсортированных по дате последнего входа (новые первыми)
     * @return список пользователей
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    List<User> findAllOrderByLastLoginAtDesc();

    /**
     * Найти пользователей, зарегистрированных после определенной даты
     * @param date дата
     * @return список пользователей
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Найти пользователей, зарегистрированных до определенной даты
     * @param date дата
     * @return список пользователей
     */
    List<User> findByCreatedAtBefore(LocalDateTime date);

    /**
     * Найти пользователей, зарегистрированных между датами
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @return список пользователей
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // ==================== Статистические запросы ====================

    /**
     * Получить общее количество пользователей
     * @return количество пользователей
     */
    @Query("SELECT COUNT(u) FROM User u")
    long getTotalUserCount();

    /**
     * Получить количество активных пользователей
     * @return количество активных пользователей
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long getActiveUserCount();

    /**
     * Получить количество пользователей по роли
     * @param role роль
     * @return количество пользователей
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRoleType role);

    /**
     * Получить статистику по ролям пользователей
     * @return массив Object[] где [0] - роль, [1] - количество
     */
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> getUserRoleStatistics();

    /**
     * Получить статистику регистраций по дням за последние N дней
     * @param days количество дней
     * @return массив Object[] где [0] - дата, [1] - количество
     */
    @Query(value = "SELECT DATE(created_at) as registration_date, COUNT(*) as count " +
            "FROM users WHERE created_at >= CURRENT_DATE - CAST(:days AS integer) " +
            "GROUP BY DATE(created_at) ORDER BY registration_date DESC",
            nativeQuery = true)
    List<Object[]> getRegistrationStatistics(@Param("days") int days);

    // ==================== Обновляющие запросы ====================

    /**
     * Обновить дату последнего входа пользователя
     * @param userId ID пользователя
     * @param lastLoginAt дата последнего входа
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.updatedAt = :lastLoginAt WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * Активировать пользователя
     * @param userId ID пользователя
     * @return количество обновленных записей
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.active = true, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int activateUser(@Param("userId") Long userId);

    /**
     * Деактивировать пользователя
     * @param userId ID пользователя
     * @return количество обновленных записей
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.active = false, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int deactivateUser(@Param("userId") Long userId);

    /**
     * Обновить роль пользователя
     * @param userId ID пользователя
     * @param role новая роль
     * @return количество обновленных записей
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = :role, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int updateUserRole(@Param("userId") Long userId, @Param("role") UserRoleType role);

    /**
     * Массово деактивировать неактивных пользователей
     * @param date дата, после которой пользователи считаются неактивными
     * @return количество обновленных записей
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.active = false WHERE u.active = true AND u.lastLoginAt < :date")
    int deactivateInactiveUsers(@Param("date") LocalDateTime date);

    // ==================== Сложные запросы с JOIN ====================

    /**
     * Получить пользователей с их адресами (если бы были адреса)
     * @return список пользователей
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.active = true")
    List<User> findActiveUsersWithAddresses();

    // ==================== Агрегатные функции ====================

    /**
     * Получить средний возраст пользователей (если есть дата рождения)
     * @return средний возраст
     */
    @Query("SELECT AVG(FUNCTION('YEAR', CURRENT_DATE) - FUNCTION('YEAR', u.createdAt)) FROM User u")
    Double getAverageUserAge();

    /**
     * Получить самого старого пользователя по дате регистрации
     * @return самый старый пользователь
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt ASC LIMIT 1")
    User getOldestUser();

    /**
     * Получить самого нового пользователя
     * @return самый новый пользователь
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC LIMIT 1")
    User getNewestUser();

    // ==================== Пагинация через параметры ====================

    /**
     * Найти пользователей с пагинацией (JPQL)
     * @param role роль пользователя
     * @param limit лимит
     * @param offset смещение
     * @return список пользователей
     */
    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.id")
    List<User> findUsersByRoleWithPagination(@Param("role") UserRoleType role,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    // ==================== Native SQL запросы ====================

    /**
     * Получить количество активных пользователей за последние 30 дней (native)
     * @return количество
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE active = true AND created_at > CURRENT_DATE - INTERVAL '30 days'",
            nativeQuery = true)
    long getActiveUsersLast30Days();

    /**
     * Получить топ N пользователей по активности (например, по количеству заказов)
     * @param limit количество пользователей
     * @return список пользователей
     */
    @Query(value = "SELECT u.* FROM users u " +
            "WHERE u.active = true " +
            "ORDER BY u.last_login_at DESC NULLS LAST " +
            "LIMIT :limit",
            nativeQuery = true)
    List<User> getMostActiveUsers(@Param("limit") int limit);

    /**
     * Поиск пользователей по телефону (частичное совпадение)
     * @param phone номер телефона
     * @return список пользователей
     */
    @Query("SELECT u FROM User u WHERE u.phone LIKE CONCAT('%', :phone, '%')")
    List<User> findByPhoneContaining(@Param("phone") String phone);

    // ==================== Удаляющие запросы ====================

    /**
     * Удалить неактивных пользователей, зарегистрированных более года назад
     * @param date дата
     * @return количество удаленных записей
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.active = false AND u.createdAt < :date")
    int deleteInactiveUsersOlderThan(@Param("date") LocalDateTime date);

    /**
     * Удалить пользователя по username
     * @param username имя пользователя
     * @return количество удаленных записей
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.username = :username")
    int deleteByUsername(@Param("username") String username);
}
