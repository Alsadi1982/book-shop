# HTTP Примеры запросов

В этой папке находятся примеры HTTP запросов для каждого микросервиса.

## Файлы

- **catalog-service.http** - примеры для book-catalog-service
- **order-service.http** - примеры для order-service  
- **user-service.http** - примеры для user-service

## Использование

Каждый файл содержит примеры запросов в формате, совместимом с:

- [HTTP Client для VS Code](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
- [Insomnia](https://insomnia.rest/)
- [Postman](https://www.postman.com/)

## Порты

- **book-catalog-service**: http://localhost:8081
- **order-service**: http://localhost:8082
- **user-service**: http://localhost:8083

## Типы запросов

Каждый файл содержит примеры для следующих операций:

- **GET** - получение данных
- **POST** - создание данных
- **PUT** - обновление данных
- **DELETE** - удаление данных

Каждый запрос включает:

- Описание операции
- Пример тела запроса (для POST/PUT)
- Примеры ответов (200 OK, 201 Created, 400 Bad Request, 404 Not Found)

## Примеры

### Создание заказа (order-service)

```http
POST http://localhost:8082/api/orders
Content-Type: application/json

{
  "userId": 1,
  "items": [
    {
      "bookId": 1,
      "quantity": 2,
      "price": 450.00
    }
  ],
  "shippingAddress": "г. Москва"
}
```

### Регистрация пользователя (user-service)

```http
POST http://localhost:8083/api/users/register
Content-Type: application/json

{
  "username": "new_user",
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "Иван",
  "lastName": "Петров"
}
```

### Получение всех книг (catalog-service)

```http
GET http://localhost:8081/api/catalog/books
Accept: application/json
```

## Мониторинг

Все сервисы также предоставляют endpoint'ы для мониторинга:

- **Health Check**: `/actuator/health` или `/api/*/health`
- **Prometheus Metrics**: `/actuator/prometheus`
