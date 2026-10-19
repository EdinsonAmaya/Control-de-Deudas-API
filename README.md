# Control de Deudas API

API REST para la gestión y control de deudas personales o grupales, organizadas por **carpetas** y **usuarios**. Diseñada como backend para una futura aplicación web en Angular, bajo principios RESTful, respuestas JSON, CORS y validaciones.

Montos en **COP** con `BigDecimal` (precisión financiera, `DECIMAL(15,2)` en base de datos).

---

## Tabla de contenidos

- [Características](#características)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura y estructura](#arquitectura-y-estructura)
- [Modelo de datos](#modelo-de-datos)
- [Endpoints](#endpoints)
- [Reglas de negocio](#reglas-de-negocio)
- [Manejo de errores](#manejo-de-errores)
- [Requisitos previos](#requisitos-previos)
- [Configuración](#configuración)
- [Cómo ejecutar](#cómo-ejecutar)
- [Tests](#tests)
- [Documentación interactiva (Swagger)](#documentación-interactiva-swagger)
- [Ejemplos de uso (cURL)](#ejemplos-de-uso-curl)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Mejoras futuras](#mejoras-futuras)

---

## Características

| Requerimiento | Estado |
|---|---|
| RF-01 Registro y consulta de usuarios | Implementado |
| RF-02 Crear, listar carpetas y detalle con total calculado | Implementado |
| RF-03 Crear y consultar deudas dentro de una carpeta | Implementado |
| RF-04 Registrar abonos e historial, con actualización automática de estado | Implementado |
| Seguridad stateless con JWT y validación de propiedad (anti-IDOR) | Implementado |
| Manejo global de errores con JSON estandarizado | Implementado |
| Documentación OpenAPI / Swagger UI | Implementado |

---

## Stack tecnológico

| Componente | Versión / Detalle |
|---|---|
| Java | 17 (Temurin 17.0.20) |
| Spring Boot | 4.1.1 |
| Build | Maven (wrapper `mvnw`) |
| Persistencia | Spring Data JPA / Hibernate 7.4.5 |
| Base de datos | MySQL 8 (`mysql-connector-j`) |
| Seguridad | Spring Security + `oauth2-resource-server` (JWT HS256) |
| Serialización JSON | Jackson 3 (`tools.jackson`) |
| Boilerplate | Lombok |
| Documentación API | springdoc-openapi 3.1.1 (Swagger UI) |
| Tests | JUnit 5, Mockito, MockMvc, H2 en memoria |

---

## Arquitectura y estructura

Organización **package-by-feature** (cada módulo agrupa su controller, service, repositorio y DTOs).

```
src/main/java/com/edinsonamaya/control_deudas/
├── ControlDeudasApplication.java   # @SpringBootApplication + @ConfigurationPropertiesScan
├── auth/                           # registro y login (emisión de JWT)
├── user/                           # usuario autenticado (/api/users/me)
├── folder/                         # carpetas y su detalle
├── debt/                           # deudas
├── payment/                        # abonos e historial
├── security/                       # SecurityConfig, JwtService, CurrentUserService, handlers
├── config/                         # JwtProperties, CorsProperties, OpenApiConfig
└── common/error/                   # excepciones + @RestControllerAdvice global

src/test/java/com/edinsonamaya/control_deudas/
├── ControlDeudasApplicationTests.java   # carga de contexto
├── DebtApiIntegrationTest.java          # flujo end-to-end con MockMvc
└── payment/PaymentServiceTest.java      # tests unitarios con Mockito
```

---

## Modelo de datos

```
User 1 ──── N Folder 1 ──── N Debt 1 ──── N Payment
```

| Entidad | Tabla | Campos principales |
|---|---|---|
| `User` | `users` | `id`, `email` (único), `password` (BCrypt), `fullName`, `createdAt` |
| `Folder` | `folders` | `id`, `name`, `user_id` (FK), `createdAt` |
| `Debt` | `debts` | `id`, `folder_id` (FK), `description`, `amount` `DECIMAL(15,2)`, `status` (`PENDING`/`PARTIAL`/`PAID`), `due_date`, `created_at` |
| `Payment` | `payments` | `id`, `debt_id` (FK), `amount` `DECIMAL(15,2)`, `payment_date`, `notes`, `created_at` |

- Restricción `CHECK (amount > 0)` en `debts` y `payments`.
- Los saldos (`paid`, `balance`, `totalDebt`) se calculan dinámicamente con consultas JPQL agregadas.

---

## Endpoints

Todas las rutas protegidas requieren el encabezado `Authorization: Bearer <JWT>`.

| Método | Endpoint | Seguridad | Código éxito | Descripción |
|---|---|---|---|---|
| POST | `/api/auth/register` | Público | 201 | Registra un usuario y devuelve un JWT |
| POST | `/api/auth/login` | Público | 200 | Autentica y devuelve un JWT |
| GET | `/api/users/me` | JWT | 200 | Datos del usuario autenticado |
| GET | `/api/users/me/folders` | JWT | 200 | Lista las carpetas del usuario con el saldo pendiente de cada una |
| POST | `/api/users/me/folders` | JWT | 201 | Crea una carpeta para el usuario autenticado |
| GET | `/api/folders/{folderId}` | JWT + propiedad | 200 | Detalle de la carpeta con su lista de deudas y total |
| POST | `/api/folders/{folderId}/debts` | JWT + propiedad | 201 | Agrega una deuda a la carpeta |
| GET | `/api/folders/{folderId}/debts` | JWT + propiedad | 200 | Lista las deudas de la carpeta |
| POST | `/api/debts/{debtId}/payments` | JWT + propiedad | 201 | Registra un abono (valida que no exceda el saldo) |
| GET | `/api/debts/{debtId}/payments` | JWT + propiedad | 200 | Historial de abonos y saldo de una deuda |

### Cuerpos de petición

```jsonc
// POST /api/auth/register
{ "email": "user@test.com", "password": "minimo8caracteres", "fullName": "Nombre Apellido" }

// POST /api/auth/login
{ "email": "user@test.com", "password": "secreta123" }

// POST /api/users/me/folders
{ "name": "Sebastian" }

// POST /api/folders/{folderId}/debts
{ "description": "comida", "amount": 20000, "date": "2026-10-10" }

// POST /api/debts/{debtId}/payments   (date y notes son opcionales)
{ "amount": 5000, "date": "2026-10-16", "notes": "abono parcial" }
```

Respuestas destacadas:

```jsonc
// AuthResponse (register/login)
{ "token": "<jwt>", "email": "usuario@correo.com" }

// DebtResponse
{ "id": 1, "description": "comida", "amount": 20000.00, "dueDate": "2026-10-10",
  "createdAt": "2026-10-15T14:25:41.10", "status": "PENDING", "paid": 0, "balance": 20000.00 }

// PaymentHistoryResponse
{ "debtId": 1, "amount": 20000.00, "paid": 5000.00, "balance": 15000.00,
  "status": "PARTIAL", "payments": [ ... ] }
```

---

## Reglas de negocio

- **Montos**: siempre `BigDecimal` y positivos (`> 0`) en deudas y abonos.
- **Abono mayor al saldo pendiente** → `400 Bad Request` (no se registra).
- **Abono sobre una deuda ya `PAID`** → `400 Bad Request`.
- **Email ya registrado** → `409 Conflict`.
- **Acceso a carpetas/deudas/abonos de otro usuario (IDOR)** → `403 Forbidden`.
- **Recurso inexistente** → `404 Not Found`.
- El estado de la deuda se actualiza automáticamente: `PENDING` → `PARTIAL` → `PAID` cuando el saldo llega a 0.
- Cada operación de escritura es transaccional (`@Transactional`).
- **Concurrencia**: el registro de abonos bloquea la fila de la deuda (`SELECT ... FOR UPDATE`, `LockModeType.PESSIMISTIC_WRITE`) para evitar condiciones de carrera entre abonos simultáneos.

---

## Manejo de errores

Todas las excepciones se traducen a un JSON uniforme mediante un `@RestControllerAdvice` global:

```json
{
  "timestamp": "2026-10-15T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El monto del abono no puede ser mayor al saldo pendiente de la deuda.",
  "path": "/api/debts/5/payments"
}
```

| Código | Cuándo |
|---|---|
| 400 | Validación de datos, montos inválidos, abono mayor al saldo, deuda ya pagada |
| 401 | Credenciales inválidas o token JWT ausente/expirado |
| 403 | Se accede a un recurso del que no se es propietario |
| 404 | Carpeta, deuda o usuario inexistente |
| 405 | Método HTTP no soportado |
| 409 | Conflicto (email ya registrado) |
| 500 | Error interno inesperado |

---

## Requisitos previos

- **JDK 17** o superior.
- **Maven** (opcional; el proyecto incluye `mvnw` / `mvnw.cmd`).
- **MySQL 8** (o Docker para levantar uno con el `docker-compose.yml` incluido).

> En Windows, si el `java` del `PATH` es Java 8, **hay que apuntar `JAVA_HOME` a un JDK 17** antes de ejecutar cualquier comando Maven (ver [Cómo ejecutar](#cómo-ejecutar)). De lo contrario falla con `UnsupportedClassVersionError`.

---

## Configuración

Archivo: `src/main/resources/application.properties`. Valores sobreescribibles por variables de entorno:

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/control_deudas?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota` | URL JDBC (crea la BD si no existe) |
| `DB_USERNAME` | `root` | Usuario de MySQL |
| `DB_PASSWORD` | `root` | Contraseña de MySQL |
| `JWT_SECRET` | `control-deudas-dev-secret-key-32-bytes-min!!` | Secreto HS256 (mínimo 32 bytes). **Sobreescribir en producción** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Orígenes permitidos (frontend Angular) |

Otras propiedades fijas: `spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.open-in-view=false`, issuer `control-deudas-api`, expiración del token `PT24H`.

---

## Cómo ejecutar

### 1. Levantar MySQL con Docker (opcional)

```bash
docker compose up -d      # mysql:8.4, usuario root/root, base de datos control_deudas
docker compose down       # detener
```

> El `docker-compose.yml` publica el puerto **3306**. Si ya tienes un MySQL local usando ese puerto, deténlo o cambia el mapeo en el compose (p. ej. `"3307:3306"`) y ajusta `DB_URL` con ese puerto.

### 2. Arrancar la aplicación

**Git Bash:**

```bash
export JAVA_HOME="C:/Users/Edinson Amaya/.jdks/temurin-17.0.20"
./mvnw spring-boot:run
```

**PowerShell:**

```powershell
$env:JAVA_HOME="C:\Users\Edinson Amaya\.jdks\temurin-17.0.20"
.\mvnw.cmd spring-boot:run
```

Con credenciales distintas a `root/root`:

```bash
DB_USERNAME=root DB_PASSWORD=toor \
JAVA_HOME="C:/Users/Edinson Amaya/.jdks/temurin-17.0.20" \
./mvnw spring-boot:run
```

### 3. Ejecutar el JAR empaquetado

```bash
export JAVA_HOME="C:/Users/Edinson Amaya/.jdks/temurin-17.0.20"
./mvnw clean package
java -jar target/control_deudas-0.0.1-SNAPSHOT.jar
```

> Usa `"$JAVA_HOME/bin/java"` si el `java` del `PATH` es Java 8.

La aplicación queda disponible en **`http://localhost:8080`** (verás `Started ControlDeudasApplication`). Detén con `Ctrl+C`.

---

## Tests

Los tests usan **H2 en memoria** (`src/test/resources/application.properties` reemplaza la configuración de MySQL), por lo que **no requieren base de datos**.

```bash
export JAVA_HOME="C:/Users/Edinson Amaya/.jdks/temurin-17.0.20"
./mvnw test
```

| Suite | Tipo | Cobertura |
|---|---|---|
| `PaymentServiceTest` | Unitario (Mockito) | Abono parcial/total, exceso de saldo, deuda pagada, monto inválido, 404, 403, historial |
| `DebtApiIntegrationTest` | Integración (`@SpringBootTest` + MockMvc) | Flujo completo registro→carpeta→deuda→abonos, 401/403/404/409, validaciones, OpenAPI |
| `ControlDeudasApplicationTests` | Contexto | Carga del `ApplicationContext` |

---

## Documentación interactiva (Swagger)

Con la aplicación en ejecución:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Ambos son de acceso público. El esquema `bearerAuth` (JWT) está declarado, por lo que se puede autenticar desde la propia UI.

---

## Ejemplos de uso (cURL)

```bash
# 1. Registro (devuelve token) -> 201
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"edinson@test.com","password":"secreta123","fullName":"Edinson Amaya"}'

# 2. Login -> 200 (guardar el token)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"edinson@test.com","password":"secreta123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

# 3. Crear carpeta -> 201
curl -X POST http://localhost:8080/api/users/me/folders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Sebastian"}'

# 4. Crear deuda (folderId=1) -> 201
curl -X POST http://localhost:8080/api/folders/1/debts \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"description":"comida","amount":20000,"date":"2026-10-10"}'

# 5. Registrar abono (debtId=1) -> 201
curl -X POST http://localhost:8080/api/debts/1/payments \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"amount":5000,"notes":"abono parcial"}'

# 6. Historial de abonos -> 200
curl http://localhost:8080/api/debts/1/payments -H "Authorization: Bearer $TOKEN"
```

---

## Decisiones de diseño

- **Stateless + JWT HS256** con `oauth2-resource-server` (sin filtro JWT propio). El token incluye el claim `roles=["USER"]`.
- **Lombok** para reducir boilerplate (builders, getters/setters, constructores).
- **Package-by-feature** para mantener cohesionados los módulos y facilitar escalabilidad.
- **Cálculo dinámico de saldos** con consultas JPQL agregadas en lugar de campos desnormalizados, evitando inconsistencias.
- **Bloqueo pesimista** en abonos en vez de caché distribuida (Redis), por simplicidad.
- **Jackson 3** (nativo de Spring Boot 4): los `ObjectMapper` inyectados usan `tools.jackson.databind.ObjectMapper`.
- **Exclusión de relaciones `@ManyToOne` en las respuestas JSON**: los DTOs (`record`) exponen solo lo necesario, evitando ciclos y exposición de datos.

---

## Mejoras futuras

- Migraciones de base de datos con **Flyway/Liquibase** en lugar de `ddl-auto=update`.
- Operaciones de actualización y borrado (PUT/PATCH/DELETE) de carpetas, deudas y abonos.
- Paginación y filtros en listados; refresh tokens y roles adicionales.
- Perfil `application-prod.properties` con configuración segura y `springdoc` deshabilitado.
- Cobertura con JaCoCo y pipeline CI (build + tests).
