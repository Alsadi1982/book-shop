package com.otus.book_catalog_service.repository;

import com.otus.book_catalog_service.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM categories");
        category = new Category();
        category.setCode("FIC");
        category.setName("Fiction");
        category.setDescription("Books with fictional stories");
    }

    @Test
    public void findByCode_ExistingCode_ReturnsCategory() {
        // Given
        categoryRepository.save(category);

        // When
        Optional<Category> result = categoryRepository.findByCode("FIC");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Fiction", result.get().getName());
        assertEquals("Books with fictional stories", result.get().getDescription());
    }

    @Test
    public void findByCode_NonExistingCode_ReturnsEmpty() {
        // When
        Optional<Category> result = categoryRepository.findByCode("NONEXIST");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void save_Category_PersistsToDatabase() {
        // When
        Category savedCategory = categoryRepository.save(category);

        // Then
        assertNotNull(savedCategory.getId());
        assertEquals("FIC", savedCategory.getCode());
        assertTrue(categoryRepository.findById(savedCategory.getId()).isPresent());
    }

    @Test
    public void delete_Category_RemovesFromDatabase() {
        // Given
        Category savedCategory = categoryRepository.save(category);

        // When
        categoryRepository.deleteById(savedCategory.getId());

        // Then
        assertFalse(categoryRepository.findById(savedCategory.getId()).isPresent());
    }

    @Test
    public void existsById_ExistingId_ReturnsTrue() {
        // Given
        Category savedCategory = categoryRepository.save(category);

        // When
        boolean exists = categoryRepository.existsById(savedCategory.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsById_NonExistingId_ReturnsFalse() {
        // When
        boolean exists = categoryRepository.existsById(999L);

        // Then
        assertFalse(exists);
    }

    @Test
    public void findByCode_DuplicateCode_ReturnsFirstCategory() {
        // Given
        categoryRepository.saveAndFlush(category);
        Category duplicateCategory = new Category();
        duplicateCategory.setCode("FIC");
        duplicateCategory.setName("Duplicate Fiction");
        duplicateCategory.setDescription("This should fail");
        // This should fail due to @Column(unique = true) constraint

        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryRepository.saveAndFlush(duplicateCategory);
        });

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE code = 'FIC'",
                Integer.class
        );
        assertEquals(1, count);
    }
}