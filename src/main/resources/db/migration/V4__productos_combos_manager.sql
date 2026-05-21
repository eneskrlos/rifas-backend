-- =============================================================
--  SISTEMA DE RIFAS — V4: Productos, Combos, rol Manager
--  Nota: max_por_persona y combo_id ya están en V1.
--        Este script solo agrega las tablas nuevas y el rol.
-- =============================================================

-- -------------------------------------------------------------
-- 1. PRODUCTOS
-- -------------------------------------------------------------
CREATE TABLE productos (
    id              SERIAL          PRIMARY KEY,
    creado_por      INT             NOT NULL REFERENCES usuarios(id),
    nombre          VARCHAR(150)    NOT NULL,
    descripcion     TEXT,
    imagen_url      VARCHAR(500),
    valor_estimado  NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_valor_positivo CHECK (valor_estimado >= 0)
);

CREATE INDEX idx_productos_activo     ON productos(activo);
CREATE INDEX idx_productos_creado_por ON productos(creado_por);

-- -------------------------------------------------------------
-- 2. COMBOS
-- -------------------------------------------------------------
CREATE TABLE combos (
    id          SERIAL        PRIMARY KEY,
    creado_por  INT           NOT NULL REFERENCES usuarios(id),
    nombre      VARCHAR(150)  NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_combos_activo     ON combos(activo);
CREATE INDEX idx_combos_creado_por ON combos(creado_por);

-- -------------------------------------------------------------
-- 3. COMBO_PRODUCTOS (tabla pivote)
-- -------------------------------------------------------------
CREATE TABLE combo_productos (
    id          SERIAL  PRIMARY KEY,
    combo_id    INT     NOT NULL REFERENCES combos(id)    ON DELETE CASCADE,
    producto_id INT     NOT NULL REFERENCES productos(id) ON DELETE RESTRICT,
    cantidad    INT     NOT NULL DEFAULT 1,
    CONSTRAINT uq_combo_producto     UNIQUE (combo_id, producto_id),
    CONSTRAINT chk_cantidad_positiva CHECK  (cantidad > 0)
);

CREATE INDEX idx_combo_productos_combo    ON combo_productos(combo_id);
CREATE INDEX idx_combo_productos_producto ON combo_productos(producto_id);

-- Vista: combo con valor total estimado
CREATE VIEW vista_combos_detalle AS
SELECT
    c.id             AS combo_id,
    c.nombre         AS combo_nombre,
    c.descripcion    AS combo_descripcion,
    COUNT(cp.id)     AS total_productos,
    SUM(p.valor_estimado * cp.cantidad) AS valor_total_estimado,
    c.activo,
    c.creado_en
FROM combos c
LEFT JOIN combo_productos cp ON cp.combo_id  = c.id
LEFT JOIN productos       p  ON p.id         = cp.producto_id
GROUP BY c.id, c.nombre, c.descripcion, c.activo, c.creado_en;

-- -------------------------------------------------------------
-- 4. AGREGAR combo_id A RIFAS
-- -------------------------------------------------------------
ALTER TABLE rifas
    ADD COLUMN combo_id INT REFERENCES combos(id) ON DELETE RESTRICT;

CREATE INDEX idx_rifas_combo ON rifas(combo_id);

-- -------------------------------------------------------------
-- 5. NUEVO ROL: MANAGER
-- -------------------------------------------------------------
INSERT INTO roles (nombre, descripcion) VALUES (
    'manager',
    'Gestor de rifas: crea productos, combos, maneja números y ejecuta sorteos'
)
ON CONFLICT (nombre) DO NOTHING;

-- -------------------------------------------------------------
-- 6. AUDITORÍA
-- -------------------------------------------------------------
INSERT INTO auditoria (usuario_id, accion, detalle)
SELECT id, 'MIGRACION_V4',
       'Migración V4: productos, combos, combo_id en rifas, rol manager'
FROM usuarios
WHERE email = 'admin@rifas.com';
