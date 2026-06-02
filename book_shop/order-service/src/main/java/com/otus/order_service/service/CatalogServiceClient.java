package com.otus.order_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class CatalogServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CatalogServiceClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${catalog.service.url:http://localhost:8081}")
    private String catalogServiceUrl;

    @Value("${user.service.url:http://localhost:8083}")
    private String userServiceUrl;

    /**
     * Get book by ID
     * @param bookId
     * @return
     */
    public Map<String, Object> getBook(Long bookId) {
        try {
            String url = catalogServiceUrl + "/api/catalog/books/" + bookId;
            Map<String, Object> book = restTemplate.getForObject(url, Map.class);
            logger.debug("Book found: id={}, title={}", bookId, book != null ? book.get("title") : "null");
            return book;
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Book not found with id: {}", bookId);
            return null;
        } catch (ResourceAccessException e) {
            logger.error("Catalog service unavailable: {}", e.getMessage());
            throw new RuntimeException("Catalog service is unavailable", e);
        }
    }

    /**
     * Check and reserve stock
     * @param bookId
     * @param quantity
     * @return
     */
    public boolean checkAndReserveStock(Long bookId, Integer quantity) {
        try {
            String url = catalogServiceUrl + "/api/catalog/books/" + bookId + "/check-stock?quantity=" + quantity;
            Boolean result = restTemplate.postForObject(url, null, Boolean.class);
            if (Boolean.TRUE.equals(result)) {
                logger.debug("Stock reserved successfully for book: {}, quantity: {}", bookId, quantity);
            } else {
                logger.warn("Failed to reserve stock for book: {}, quantity: {}", bookId, quantity);
            }
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to reserve stock for book {}: {}", bookId, e.getMessage());
            return false;
        }
    }

    /**
     * Return stock to catalog (when order is cancelled or returned)
     * @param bookId
     * @param quantity
     * @return
     */
    public boolean returnStock(Long bookId, Integer quantity) {
        try {
            String url = catalogServiceUrl + "/api/catalog/books/" + bookId + "/return-stock?quantity=" + quantity;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            Boolean result = restTemplate.exchange(url, HttpMethod.POST, entity, Boolean.class).getBody();
            if (Boolean.TRUE.equals(result)) {
                logger.debug("Stock returned successfully for book: {}, quantity: {}", bookId, quantity);
            } else {
                logger.warn("Failed to return stock for book: {}, quantity: {}", bookId, quantity);
            }
            return Boolean.TRUE.equals(result);
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Book not found when returning stock: {}", bookId);
            return false;
        } catch (Exception e) {
            logger.error("Failed to return stock for book {}: {}", bookId, e.getMessage());
            return false;
        }
    }

    /**
     * Get user by ID
     * @param userId
     * @return
     */
    public Map<String, Object> getUser(Long userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            Map<String, Object> user = restTemplate.getForObject(url, Map.class);
            logger.debug("User found: id={}", userId);
            return user;
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("User not found with id: {}", userId);
            return null;
        } catch (ResourceAccessException e) {
            logger.error("User service unavailable: {}", e.getMessage());
            throw new RuntimeException("User service is unavailable", e);
        }
    }
}
