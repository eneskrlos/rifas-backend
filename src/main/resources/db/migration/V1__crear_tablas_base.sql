-- =============================================================
--  SISTEMA DE RIFAS — V1: Creación de tablas base
--  Motor: PostgreSQL 16+
--  Nota: estados usan VARCHAR + CHECK constraint en lugar de
--        ENUM de PostgreSQL para compatibilidad con Hibernate.
-- =============================================================

-- -------------------------------------------------------------
-- 1. ROLES
-- -------------------------------------------------------------
CREATE TABLE roles (
    id          SERIAL        PRIMARY KEY,
    nombre      VARCHAR(50)   NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    creado_en   TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------------
-- 2. USUARIOS
-- -------------------------------------------------------------
CREATE TABLE usuarios (
    id              SERIAL        PRIMARY KEY,
    rol_id          INT           NOT NULL REFERENCES roles(id),
    nombre_completo VARCHAR(150)  NOT NULL,
    email           VARCHAR(150)  UNIQUE,
    telefono        VARCHAR(20)   UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_contacto CHECK (
        email IS NOT NULL OR telefono IS NOT NULL
    )
);

CREATE INDEX idx_usuarios_email    ON usuarios(email)    WHERE email    IS NOT NULL;
CREATE INDEX idx_usuarios_telefono ON usuarios(telefono) WHERE telefono IS NOT NULL;
CREATE INDEX idx_usuarios_rol      ON usuarios(rol_id);

-- -------------------------------------------------------------
-- 3. RIFAS
--    estado: VARCHAR con CHECK — compatible con Hibernate EnumType.STRING
-- -------------------------------------------------------------
CREATE TABLE rifas (
    id              SERIAL        PRIMARY KEY,
    creado_por      INT           NOT NULL REFERENCES usuarios(id),
    nombre          VARCHAR(150)  NOT NULL,
    descripcion     TEXT,
    total_numeros   INT           NOT NULL DEFAULT 200,
    max_por_persona INT,
    estado          VARCHAR(20)   NOT NULL DEFAULT 'BORRADOR',
    inicio_en       TIMESTAMP,
    sorteo_en       TIMESTAMP,
    creado_en       TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_estado_rifa     CHECK (estado IN ('BORRADOR','ACTIVA','CERRADA','SORTEADA','CANCELADA')),
    CONSTRAINT chk_total_numeros   CHECK (total_numeros BETWEEN 1 AND 10000),
    CONSTRAINT chk_max_por_persona CHECK (max_por_persona IS NULL OR max_por_persona >= 1),
    CONSTRAINT chk_fechas          CHECK (sorteo_en IS NULL OR inicio_en IS NULL OR sorteo_en > inicio_en)
);

CREATE INDEX idx_rifas_estado     ON rifas(estado);
CREATE INDEX idx_rifas_creado_por ON rifas(creado_por);

-- -------------------------------------------------------------
-- 4. NUMEROS_RIFA
-- -------------------------------------------------------------
CREATE TABLE numeros_rifa (
    id      SERIAL       PRIMARY KEY,
    rifa_id INT          NOT NULL REFERENCES rifas(id) ON DELETE CASCADE,
    numero  INT          NOT NULL,
    estado  VARCHAR(15)  NOT NULL DEFAULT 'DISPONIBLE',
    CONSTRAINT uq_numero_por_rifa  UNIQUE (rifa_id, numero),
    CONSTRAINT chk_numero_positivo CHECK (numero > 0),
    CONSTRAINT chk_estado_numero   CHECK (estado IN ('DISPONIBLE','RESERVADO'))
);

CREATE INDEX idx_numeros_rifa_id     ON numeros_rifa(rifa_id);
CREATE INDEX idx_numeros_disponibles ON numeros_rifa(rifa_id, estado)
    WHERE estado = 'DISPONIBLE';

-- -------------------------------------------------------------
-- 5. PARTICIPACIONES
-- -------------------------------------------------------------
CREATE TABLE participaciones (
    id             SERIAL    PRIMARY KEY,
    usuario_id     INT       NOT NULL REFERENCES usuarios(id),
    rifa_id        INT       NOT NULL REFERENCES rifas(id),
    numero_rifa_id INT       NOT NULL REFERENCES numeros_rifa(id),
    asignado_en    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_numero_participacion UNIQUE (numero_rifa_id)
);

CREATE INDEX idx_participaciones_usuario ON participaciones(usuario_id);
CREATE INDEX idx_participaciones_rifa    ON participaciones(rifa_id);

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
-- -------------------------------------------------------------
CREATE TABLE sorteos (
    id                 SERIAL       PRIMARY KEY,
    rifa_id            INT          NOT NULL UNIQUE REFERENCES rifas(id),
    numero_ganador_id  INT          NOT NULL REFERENCES numeros_rifa(id),
    usuario_ganador_id INT          NOT NULL REFERENCES usuarios(id),
    algoritmo          VARCHAR(100) NOT NULL DEFAULT 'java.security.SecureRandom',
    semilla            VARCHAR(255) NOT NULL,
    ejecutado_en       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sorteos_rifa    ON sorteos(rifa_id);
CREATE INDEX idx_sorteos_usuario ON sorteos(usuario_ganador_id);

-- -------------------------------------------------------------
-- 7. AUDITORÍA
-- -------------------------------------------------------------
CREATE TABLE auditoria (
    id             SERIAL       PRIMARY KEY,
    usuario_id     INT,
    accion         VARCHAR(100) NOT NULL,
    tabla_afectada VARCHAR(100),
    registro_id    INT,
    detalle        TEXT,
    ip_origen      VARCHAR(45),
    ocurrido_en    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_usuario   ON auditoria(usuario_id);
CREATE INDEX idx_auditoria_accion    ON auditoria(accion);
CREATE INDEX idx_auditoria_fecha     ON auditoria(ocurrido_en);
CREATE INDEX idx_auditoria_tabla_reg ON auditoria(tabla_afectada, registro_id);
