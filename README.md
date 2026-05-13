# 🎯 rifas-backend — API REST Spring Boot

## Stack tecnológico
| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3 |
| Seguridad | Spring Security + JWT (jjwt 0.12) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 16 (Docker) |
| Migraciones | Flyway |
| Mapeo DTO | MapStruct |
| Boilerplate | Lombok |
| Documentación | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3.9+ |

---

## Estructura de paquetes

```
rifas-backend/
│
├── pom.xml
│
└── src/
    ├── main/
    │   ├── java/com/rifas/
    │   │   │
    │   │   ├── RifasApplication.java          ← Punto de entrada
    │   │   │
    │   │   ├── config/                        ← Configuración global
    │   │   │   ├── SecurityConfig.java        ← Cadena de filtros, CORS, rutas públicas
    │   │   │   ├── OpenApiConfig.java         ← Swagger / bearerAuth
    │   │   │   └── AuditConfig.java           ← AuditorAware para JPA Auditing
    │   │   │
    │   │   ├── entity/                        ← Entidades JPA (mapean tablas)
    │   │   │   ├── Rol.java
    │   │   │   ├── Usuario.java
    │   │   │   ├── Producto.java
    │   │   │   ├── Combo.java
    │   │   │   ├── ComboProducto.java
    │   │   │   ├── Rifa.java
    │   │   │   ├── NumeroRifa.java
    │   │   │   ├── Participacion.java
    │   │   │   ├── Sorteo.java
    │   │   │   └── Auditoria.java
    │   │   │
    │   │   ├── repository/                    ← Interfaces JPA (acceso a BD)
    │   │   │   ├── RolRepository.java
    │   │   │   ├── UsuarioRepository.java
    │   │   │   ├── ProductoRepository.java
    │   │   │   ├── ComboRepository.java
    │   │   │   ├── ComboProductoRepository.java
    │   │   │   ├── RifaRepository.java
    │   │   │   ├── NumeroRifaRepository.java
    │   │   │   ├── ParticipacionRepository.java
    │   │   │   ├── SorteoRepository.java
    │   │   │   └── AuditoriaRepository.java
    │   │   │
    │   │   ├── dto/                           ← Objetos de transferencia (nunca exponer entidades)
    │   │   │   ├── request/                   ← Lo que recibe la API (entrada)
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   ├── RegisterRequest.java
    │   │   │   │   ├── ProductoRequest.java
    │   │   │   │   ├── ComboRequest.java
    │   │   │   │   ├── RifaRequest.java
    │   │   │   │   └── ElegirNumeroRequest.java
    │   │   │   │
    │   │   │   └── response/                  ← Lo que devuelve la API (salida)
    │   │   │       ├── AuthResponse.java
    │   │   │       ├── UsuarioResponse.java
    │   │   │       ├── ProductoResponse.java
    │   │   │       ├── ComboResponse.java
    │   │   │       ├── RifaResponse.java
    │   │   │       ├── NumeroRifaResponse.java
    │   │   │       ├── ParticipacionResponse.java
    │   │   │       ├── SorteoResponse.java
    │   │   │       └── ApiResponse.java       ← Wrapper genérico {status, message, data}
    │   │   │
    │   │   ├── service/                       ← Lógica de negocio (interfaces)
    │   │   │   ├── AuthService.java
    │   │   │   ├── UsuarioService.java
    │   │   │   ├── ProductoService.java
    │   │   │   ├── ComboService.java
    │   │   │   ├── RifaService.java
    │   │   │   ├── NumeroRifaService.java
    │   │   │   ├── ParticipacionService.java
    │   │   │   ├── SorteoService.java
    │   │   │   └── AuditoriaService.java
    │   │   │
    │   │   ├── service/impl/                  ← Implementaciones de los servicios
    │   │   │   ├── AuthServiceImpl.java
    │   │   │   ├── UsuarioServiceImpl.java
    │   │   │   ├── ProductoServiceImpl.java
    │   │   │   ├── ComboServiceImpl.java
    │   │   │   ├── RifaServiceImpl.java
    │   │   │   ├── NumeroRifaServiceImpl.java
    │   │   │   ├── ParticipacionServiceImpl.java
    │   │   │   ├── SorteoServiceImpl.java
    │   │   │   └── AuditoriaServiceImpl.java
    │   │   │
    │   │   ├── controller/                    ← Endpoints REST
    │   │   │   ├── AuthController.java        ← POST /auth/login, /auth/register
    │   │   │   ├── UsuarioController.java     ← GET/PUT /usuarios
    │   │   │   ├── ProductoController.java    ← CRUD /productos
    │   │   │   ├── ComboController.java       ← CRUD /combos
    │   │   │   ├── RifaController.java        ← CRUD + activar/cerrar /rifas
    │   │   │   ├── NumeroRifaController.java  ← GET disponibles, agregar, eliminar
    │   │   │   ├── ParticipacionController.java← POST elegir número
    │   │   │   ├── SorteoController.java      ← POST ejecutar sorteo
    │   │   │   └── AdminController.java       ← Panel admin: reportes y usuarios
    │   │   │
    │   │   ├── security/                      ← Infraestructura JWT + Spring Security
    │   │   │   ├── filter/
    │   │   │   │   └── JwtAuthFilter.java     ← Intercepta cada request y valida el token
    │   │   │   ├── util/
    │   │   │   │   └── JwtUtil.java           ← Genera, valida y parsea tokens JWT
    │   │   │   └── UserDetailsServiceImpl.java← Carga usuario desde BD para Spring Security
    │   │   │
    │   │   ├── exception/                     ← Manejo centralizado de errores
    │   │   │   ├── GlobalExceptionHandler.java← @RestControllerAdvice
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── BusinessException.java
    │   │   │   ├── UnauthorizedException.java
    │   │   │   └── ValidationException.java
    │   │   │
    │   │   └── util/                          ← Utilidades transversales
    │   │       ├── SorteoAlgoritmo.java       ← Lógica Random con semilla auditable
    │   │       └── Constants.java             ← Roles, estados, mensajes
    │   │
    │   └── resources/
    │       ├── application.yml                ← Configuración principal
    │       └── db/
    │           └── migration/                 ← Scripts Flyway (ya creados)
    │               ├── V1__crear_tablas_base.sql
    │               ├── V2__datos_iniciales.sql
    │               ├── V3__funciones_y_triggers.sql
    │               └── V4__productos_combos_manager.sql
    │
    └── test/
        └── java/com/rifas/
            ├── service/                       ← Unit tests de servicios
            └── controller/                    ← Integration tests de endpoints
```

---

## Flujo de una request HTTP

```
HTTP Request
    │
    ▼
JwtAuthFilter          ← Verifica el token JWT en el header Authorization
    │
    ▼
SecurityConfig         ← Verifica rol permitido para esa ruta
    │
    ▼
Controller             ← Recibe DTO de entrada, valida con @Valid
    │
    ▼
Service (interface)    ← Define el contrato del caso de uso
    │
    ▼
ServiceImpl            ← Ejecuta la lógica de negocio
    │
    ▼
Repository             ← Consulta / escribe en PostgreSQL vía JPA
    │
    ▼
Controller             ← Mapea entidad → DTO de respuesta con MapStruct
    │
    ▼
HTTP Response (JSON)   ← Envuelto en ApiResponse<T>
```

---

## Endpoints principales (resumen)

### Auth (público)
| Método | Ruta | Descripción |
|---|---|---|
| POST | /api/auth/register | Registrar participante |
| POST | /api/auth/login | Login → retorna JWT |

### Rifas (participante)
| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/rifas/activas | Ver rifas disponibles |
| GET | /api/rifas/{id}/numeros | Ver números disponibles |
| POST | /api/participaciones | Elegir un número |

### Manager
| Método | Ruta | Descripción |
|---|---|---|
| POST | /api/productos | Crear producto |
| POST | /api/combos | Crear combo |
| POST | /api/rifas | Crear rifa |
| PUT | /api/rifas/{id}/activar | Activar rifa (genera números) |
| POST | /api/rifas/{id}/numeros | Agregar números extra |
| DELETE | /api/rifas/{id}/numeros/{num} | Eliminar número disponible |
| POST | /api/sorteos/{rifaId} | Ejecutar sorteo |

### Admin
| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/admin/usuarios | Listar todos los usuarios |
| PUT | /api/admin/usuarios/{id}/estado | Activar/desactivar usuario |
| GET | /api/admin/auditoria | Ver log de auditoría |
| GET | /api/admin/rifas | Ver todas las rifas |

---

## Cómo levantar el proyecto

```bash
# 1. Asegurarse que Docker está corriendo con la BD
docker compose up -d

# 2. Compilar el proyecto
mvn clean install -DskipTests

# 3. Ejecutar
mvn spring-boot:run

# La API estará disponible en:
# http://localhost:8080/api

# Swagger UI:
# http://localhost:8080/api/swagger-ui.html
```
