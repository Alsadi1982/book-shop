# Book Shop - Микросервисное приложение

Микросервисная архитектура интернет-магазина книг на основе Spring Boot 3.5.14, Kubernetes и Helm.

## 🏗️ Архитектура

Проект состоит из трех микросервисов:

| Сервис | Порт | Описание |
|--------|------|----------|
| **book-catalog-service** | 8081 | Управление книгами, категориями и инвентарем |
| **order-service** | 8082 | Обработка заказов и история заказов пользователей |
| **user-service** | 8083 | Управление пользователями и профилями |

### Технологии

- **Java 17**
- **Spring Boot 3.5.14**
- **Spring Data JPA** (PostgreSQL)
- **Resilience4j** (circuit breaker, rate limiter, retry, bulkhead)
- **Lombok** (для сокращения boilerplate кода)
- **SLF4J + Logback** (логирование)
- **Prometheus + Grafana** (мониторинг)
- **Loki** (логирование)
- **Kubernetes + Helm** (оркестрация)

### Коммуникация между сервисами

- **HTTP** через `RestTemplate` / `WebClient`
- **Resilience4j** для отказоустойчивости

---

## 📦 Сборка проекта

### Требования

- Java 17+
- Maven 3.8+
- Docker (для создания образов)
- Kubernetes Desktop (для развертывания)

### Шаги сборки

```bash
# Сборка всех модулей
mvn clean install

# Сборка отдельного модуля
cd book-catalog-service && mvn clean package
cd order-service && mvn clean package
cd user-service && mvn clean package
```

### Создание Docker образов

```bash
# Для book-catalog-service
cd book-catalog-service
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=alsadi1982/catalogservice:v4

# Для order-service
cd order-service
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=alsadi1982/orderservice:v4

# Для user-service
cd user-service
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=alsadi1982/userservice:v4
```

---

## 🚀 Локальная разработка

### Запуск базы данных (PostgreSQL)

```bash
# Запуск PostgreSQL для book-catalog-service
docker run -d --name catalog-postgres \
  -e POSTGRES_DB=catalog_db \
  -e POSTGRES_USER=catalog_user \
  -e POSTGRES_PASSWORD=catalog_password \
  -p 5432:5432 \
  postgres:15-alpine

# Запуск PostgreSQL для order-service
docker run -d --name order-postgres \
  -e POSTGRES_DB=order_db \
  -e POSTGRES_USER=order_user \
  -e POSTGRES_PASSWORD=order_password \
  -p 5433:5432 \
  postgres:15-alpine

# Запуск PostgreSQL для user-service
docker run -d --name user-postgres \
  -e POSTGRES_DB=user_service_db \
  -e POSTGRES_USER=user \
  -e POSTGRES_PASSWORD=password \
  -p 5434:5432 \
  postgres:15-alpine
```

### Запуск сервисов локально

```bash
# Запуск book-catalog-service (порт 8081)
cd book-catalog-service
mvn spring-boot:run

# Запуск order-service (порт 8082)
cd order-service
mvn spring-boot:run

# Запуск user-service (порт 8083)
cd user-service
mvn spring-boot:run
```

---

## 🌐 Развертывание в Kubernetes Desktop

### Предварительные требования

1. **Docker Desktop** с включенным Kubernetes
2. **Helm** (установлен)
3. **kubectl** (установлен и сконфигурирован)

### Шаг 1: Публикация Docker образов

```bash
# Если используется Docker Desktop, убедитесь, что вы используете его Docker daemon
# В Docker Desktop перейдите в Settings → Resources → Kubernetes и включите Kubernetes

# Собрать и опубликовать образы
docker tag alsadi1982/catalogservice:v4 alsadi1982/catalogservice:v4
docker push alsadi1982/catalogservice:v4

docker tag alsadi1982/orderservice:v4 alsadi1982/orderservice:v4
docker push alsadi1982/orderservice:v4

docker tag alsadi1982/userservice:v4 alsadi1982/userservice:v4
docker push alsadi1982/userservice:v4
```

> **Примечание**: Если образы локальные, можно использовать `imagePullPolicy: IfNotPresent` в Helm чартах.

### Шаг 2: Установка через Helm

```bash
# Установка всех сервисов
helm install bookshop ./bookshop-helm

# Установка отдельного сервиса
helm install bookshop-catalog ./bookshop-helm/charts/book-catalog-service
helm install bookshop-order ./bookshop-helm/charts/order-service
helm install bookshop-user ./bookshop-helm/charts/user-service
```

### Шаг 3: Проверка статуса

```bash
# Проверка подов
kubectl get pods

# Проверка сервисов
kubectl get services

# Проверка состояния Helm релиза
helm list
```

### Шаг 4: Доступ к приложению

Команды для доступа к сервисам извне кластера:

```bash
# book-catalog-service
kubectl port-forward service/bookshop-helm-book-catalog-service 8081:8081

# order-service
kubectl port-forward service/bookshop-helm-order-service 8082:8082

# user-service
kubectl port-forward service/bookshop-helm-user-service 8083:8083
```

---

## 📊 Мониторинг и логирование

### Prometheus Metrics

Все сервисы экспортят метрики Prometheus по endpoint'у:

```
http://localhost:8081/actuator/prometheus  # catalog-service
http://localhost:8082/actuator/prometheus  # order-service
http://localhost:8083/actuator/prometheus  # user-service
```

### Health Checks

```bash
# Health check endpoints
http://localhost:8081/actuator/health
http://localhost:8082/actuator/health
http://localhost:8083/actuator/health
```

### Логирование

- **Локально**: Логи выводятся в консоль
- **Kubernetes**: Логи отправляются в Loki по адресу `http://loki:3100/loki/api/v1/push`

---

## 🔧 Troubleshooting

### Проверка логов

```bash
# Логи пода
kubectl logs <pod-name>

# Логи пода с следом
kubectl logs -f <pod-name>

# Логи всех подов с меткой
kubectl logs -l app=book-catalog-service
```

### Отладка

```bash
# Подключение к поду
kubectl exec -it <pod-name> -- /bin/bash

# Проверка портов
kubectl port-forward <pod-name> 8081:8081
```

### Распространенные проблемы

#### Под не запускается

```bash
# Проверить события
kubectl describe pod <pod-name>

# Проверить логи
kubectl logs <pod-name> --previous
```

#### Сервис недоступен

```bash
# Проверить сервис
kubectl get svc <service-name>

# Проверить endpoints
kubectl get endpoints <service-name>
```

#### Ошибки подключения к БД

- Убедитесь, что PostgreSQL запущен
- Проверьте переменные окружения `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- Убедитесь, что база данных создана и пользователь имеет права

---

## 📝 HTTP примеры

См. папку `http/` для примеров запросов:

- [`catalog-service.http`](http/catalog-service.http) - запросы к catalog-service
- [`order-service.http`](http/order-service.http) - запросы к order-service
- [`user-service.http`](http/user-service.http) - запросы к user-service

---

## 📚 Дополнительная информация

- [GIGACODE.md](GIGACODE.md) - руководство для разработчиков
- [Helm чарты](bookshop-helm/) - конфигурация Helm
- [Servicemonitor](book-catalog-service/servicemonitor.yaml) - конфигурация Prometheus Operator

---

## 🛠️ Разработка

### Новые endpoints

Следуйте паттерну:

```java
@GetMapping("/your-endpoint")
@CircuitBreaker(name = "serviceName", fallbackMethod = "fallbackMethodName")
@RateLimiter(name = "serviceName")
public ResponseEntity<?> yourMethod(@RequestParam(required = false) String param) {
    // Ваш код
}
```

### Логирование

Добавляйте логи для критических операций:

```java
logger.info("Operation started for entity: {}", id);
logger.debug("Detailed debug info");
logger.error("Error occurred: {}", e.getMessage());
```

---

## 📄 Лицензия

MIT License
