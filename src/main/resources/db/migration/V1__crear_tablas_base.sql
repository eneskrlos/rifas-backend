-- =============================================================
--  SISTEMA DE RIFAS — V1: Creación de tablas base
--  Motor: PostgreSQL 15+
--  Autor: Sistema de Rifas
--  Fecha: 2025
-- =============================================================

-- Extensión para UUID (opcional, usamos SERIAL por simplicidad)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -------------------------------------------------------------
-- 1. ROLES
--    Define los perfiles de acceso del sistema.
-- -------------------------------------------------------------
CREATE TABLE roles (
    id          SERIAL          PRIMARY KEY,
    nombre      VARCHAR(50)     NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    creado_en   TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------------
-- 2. USUARIOS
--    Email o teléfono es obligatorio (al menos uno).
--    El CHECK garantiza integridad a nivel de BD.
-- -------------------------------------------------------------
CREATE TABLE usuarios (
    id              SERIAL          PRIMARY KEY,
    rol_id          INT             NOT NULL REFERENCES roles(id),
    nombre_completo VARCHAR(150)    NOT NULL,
    email           VARCHAR(150)    UNIQUE,
    telefono        VARCHAR(20)     UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Al menos email o teléfono debe estar presente
    CONSTRAINT chk_contacto CHECK (
        email IS NOT NULL OR telefono IS NOT NULL
    )
);

-- Índices para búsquedas frecuentes de login
CREATE INDEX idx_usuarios_email    ON usuarios(email)    WHERE email    IS NOT NULL;
CREATE INDEX idx_usuarios_telefono ON usuarios(telefono) WHERE telefono IS NOT NULL;
CREATE INDEX idx_usuarios_rol      ON usuarios(rol_id);

-- -------------------------------------------------------------
-- 3. RIFAS
--    Una rifa tiene ciclo de vida controlado por 'estado'.
--    Escalable: total_numeros y max_por_persona son configurables.
-- -------------------------------------------------------------
CREATE TYPE estado_rifa AS ENUM (
    'borrador',
    'activa',
    'cerrada',
    'sorteada',
    'cancelada'
);

CREATE TABLE rifas (
    id              SERIAL          PRIMARY KEY,
    creado_por      INT             NOT NULL REFERENCES usuarios(id),
    nombre          VARCHAR(150)    NOT NULL,
    descripcion     TEXT,
    premio          VARCHAR(255)    NOT NULL,
    total_numeros   INT             NOT NULL DEFAULT 200,
    max_por_persona INT             NOT NULL DEFAULT 2,
    estado          estado_rifa     NOT NULL DEFAULT 'borrador',
    inicio_en       TIMESTAMP,
    sorteo_en       TIMESTAMP,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Validaciones de negocio a nivel BD
    CONSTRAINT chk_total_numeros    CHECK (total_numeros   BETWEEN 1 AND 10000),
    CONSTRAINT chk_max_por_persona  CHECK (max_por_persona BETWEEN 1 AND total_numeros),
    CONSTRAINT chk_fechas           CHECK (sorteo_en IS NULL OR inicio_en IS NULL OR sorteo_en > inicio_en)
);

CREATE INDEX idx_rifas_estado     ON rifas(estado);
CREATE INDEX idx_rifas_creado_por ON rifas(creado_por);

-- -------------------------------------------------------------
-- 4. NUMEROS_RIFA
--    Se pre-generan al crear la rifa (ver V2 para función).
--    UNIQUE(rifa_id, numero) evita duplicados absolutos.
--    UNIQUE(rifa_id, numero) + estado='reservado' previene
--    condiciones de carrera en la selección.
-- -------------------------------------------------------------
CREATE TYPE estado_numero AS ENUM (
    'disponible',
    'reservado'
);

CREATE TABLE numeros_rifa (
    id          SERIAL          PRIMARY KEY,
    rifa_id     INT             NOT NULL REFERENCES rifas(id) ON DELETE CASCADE,
    numero      INT             NOT NULL,
    estado      estado_numero   NOT NULL DEFAULT 'disponible',

    -- Un número sólo puede existir una vez por rifa
    CONSTRAINT uq_numero_por_rifa UNIQUE (rifa_id, numero),

    CONSTRAINT chk_numero_positivo CHECK (numero > 0)
);

CREATE INDEX idx_numeros_rifa_id     ON numeros_rifa(rifa_id);
CREATE INDEX idx_numeros_disponibles ON numeros_rifa(rifa_id, estado) WHERE estado = 'disponible';

-- -------------------------------------------------------------
-- 5. PARTICIPACIONES
--    Une usuario + rifa + número elegido.
--    El límite de max_por_persona se valida en la aplicación
--    y reforzado con el trigger en V3.
-- -------------------------------------------------------------
CREATE TABLE participaciones (
    id              SERIAL      PRIMARY KEY,
    usuario_id      INT         NOT NULL REFERENCES usuarios(id),
    rifa_id         INT         NOT NULL REFERENCES rifas(id),
    numero_rifa_id  INT         NOT NULL REFERENCES numeros_rifa(id),
    asignado_en     TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Un número sólo puede estar asignado a una participación
    CONSTRAINT uq_numero_participacion UNIQUE (numero_rifa_id)
);

CREATE INDEX idx_participaciones_usuario ON participaciones(usuario_id);
CREATE INDEX idx_participaciones_rifa    ON participaciones(rifa_id);

-- Vista útil para el admin: cuántos números tiene cada usuario por rifa
CREATE VIEW vista_numeros_por_usuario AS
SELECT
    p.rifa_id,
    p.usuario_id,
    u.nombre_completo,
    COUNT(p.id) AS cantidad_numeros
FROM participaciones p
JOIN usuarios u ON u.id = p.usuario_id
GROUP BY p.rifa_id, p.usuario_id, u.nombre_completo;

-- -------------------------------------------------------------
-- 6. SORTEOS
--    Registra el resultado del sorteo con trazabilidad total.
--    'semilla' permite reproducir y auditar el resultado.
-- -------------------------------------------------------------
CREATE TABLE sorteos (
    id                  SERIAL          PRIMARY KEY,
    rifa_id             INT             NOT NULL UNIQUE REFERENCES rifas(id),
    numero_ganador_id   INT             NOT NULL REFERENCES numeros_rifa(id),
    usuario_ganador_id  INT             NOT NULL REFERENCES usuarios(id),
    algoritmo           VARCHAR(100)    NOT NULL DEFAULT 'java.util.Random',
    semilla             VARCHAR(255)    NOT NULL,
    ejecutado_en        TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sorteos_rifa    ON sorteos(rifa_id);
CREATE INDEX idx_sorteos_usuario ON sorteos(usuario_ganador_id);

-- -------------------------------------------------------------
-- 7. AUDITORIA
--    Log inmutable de todas las acciones importantes.
--    No tiene FK a usuarios para conservar el log aunque
--    se desactive un usuario.
-- -------------------------------------------------------------
CREATE TABLE auditoria (
    id               SERIAL          PRIMARY KEY,
    usuario_id       INT,            -- NULL si es acción del sistema
    accion           VARCHAR(100)    NOT NULL,
    tabla_afectada   VARCHAR(100),
    registro_id      INT,
    detalle          TEXT,
    ip_origen        VARCHAR(45),    -- soporta IPv4 e IPv6
    ocurrido_en      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_usuario   ON auditoria(usuario_id);
CREATE INDEX idx_auditoria_accion    ON auditoria(accion);
CREATE INDEX idx_auditoria_fecha     ON auditoria(ocurrido_en);
CREATE INDEX idx_auditoria_tabla_reg ON auditoria(tabla_afectada, registro_id);
