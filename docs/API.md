# Community Services API

## Base URL
```
/api/v1
```

## Аутентификация

**OAuth2 Flow:**
1. Редирект на `GET /auth/login?return_url=https://your-app.com/callback`
2. После авторизации — редирект обратно с `?token=JWT_TOKEN`
3. Использовать токен в заголовке: `Authorization: Bearer <token>`

```typescript
// Получить профиль
GET /auth/me → UserProfile
```

---

## Услуги (Services)

```typescript
// Список с фильтрами
GET /services?q=&category=&type=&city=&hasBonus=&page=1&pageSize=20
→ PagedResponse<ServiceListItem>

// Детали
GET /services/{id} → Service

// Создать (auth required)
POST /services ← CreateServiceRequest → Service

// Обновить (auth required, owner only)
PUT /services/{id} ← UpdateServiceRequest → Service

// Удалить (auth required, owner/moderator)
DELETE /services/{id} → 204

// Мои услуги (auth required)
GET /my/services → PagedResponse<ServiceListItem>
```

---

## Категории

```typescript
// Дерево категорий
GET /categories → CategoryTree[]

// Плоский список
GET /categories?flat=true → Category[]
```

---

## Типы данных

```typescript
// Enums
type ServiceType = 'ONLINE' | 'OFFLINE' | 'HYBRID'
type BonusType = 'DISCOUNT_PERCENT' | 'DISCOUNT_FIXED' | 'FREE_TRIAL' | 'GIFT' | 'CUSTOM'

// Вложенные объекты
interface Contacts {
  email?: string
  phone?: string
  telegram?: string
  whatsapp?: string
  website?: string
}

interface Location {
  country?: string
  city?: string
  address?: string
  latitude?: number
  longitude?: number
}

interface Bonus {
  type: BonusType
  value?: string        // "10" для 10%, "500" для фикс. скидки
  description: string   // "Скидка 10% для членов клуба"
}

// Пользователь (публичный)
interface User {
  id: string
  slug: string
  fullName: string
  avatarUrl?: string
  country?: string
  city?: string
}

// Категория
interface Category {
  id: string
  name: string
  slug: string
  icon?: string
  children?: Category[]
}

// Услуга (список)
interface ServiceListItem {
  id: string
  title: string
  description: string   // обрезано до 200 символов
  type: ServiceType
  location?: Location
  bonus?: Bonus
  owner: User
  categories: Category[]
  createdAt: string     // ISO datetime
}

// Услуга (полная)
interface Service extends ServiceListItem {
  status: 'ACTIVE' | 'INACTIVE'
  contacts: Contacts
  tags: string[]
  updatedAt: string
}

// Пагинация
interface PagedResponse<T> {
  items: T[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

// Ошибка
interface ErrorResponse {
  error: string         // код: "not_found", "validation_error"
  message: string       // человекочитаемое описание
}
```

---

## Создание услуги

```typescript
interface CreateServiceRequest {
  title: string
  description: string
  type: ServiceType
  contacts: Contacts            // минимум email или telegram
  location?: Location
  bonus?: Bonus
  tags?: string[]
  categoryIds?: string[]        // UUID категорий
}
```

**Пример:**
```json
{
  "title": "Разработка мобильных приложений",
  "description": "iOS и Android разработка...",
  "type": "ONLINE",
  "contacts": {
    "telegram": "@developer",
    "email": "dev@example.com"
  },
  "bonus": {
    "type": "DISCOUNT_PERCENT",
    "value": "15",
    "description": "Скидка 15% для членов клуба"
  },
  "categoryIds": ["uuid-1", "uuid-2"]
}
```

---

## HTTP коды

| Код | Значение |
|-----|----------|
| 200 | OK |
| 201 | Created |
| 204 | No Content (успешное удаление) |
| 400 | Validation error |
| 401 | Unauthorized (нет/невалидный токен) |
| 403 | Forbidden (нет прав) |
| 404 | Not found |
