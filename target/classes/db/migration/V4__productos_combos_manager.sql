-- =============================================================
--  SISTEMA DE RIFAS — V4: Productos, Combos, rol Manager
--  Cambios:
--    1. Nueva tabla: productos
--    2. Nueva tabla: combos
--    3. Nueva tabla pivote: combo_productos
--    4. rifas referencia combo_id (reemplaza campo premio texto)
--    5. Nuevo rol: manager
--    6. max_por_persona pasa a NULL = sin límite (mín. 1 número)
--    7. Trigger de límite actualizado
-- =============================================================

-- -------------------------------------------------------------
-- 1. PRODUCTOS
--    Unidad mínima de un premio. Siempre pertenece a un combo.
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

CREATE INDEX idx_productos_activo    ON productos(activo);
CREATE INDEX idx_productos_creado_por ON productos(creado_por);

-- -------------------------------------------------------------
-- 2. COMBOS
--    Agrupa uno o más productos que se sortean juntos.
--    Una rifa siempre apunta a un combo.
-- -------------------------------------------------------------
CREATE TABLE combos (
    id              SERIAL          PRIMARY KEY,
    creado_por      INT             NOT NULL REFERENCES usuarios(id),
    nombre          VARCHAR(150)    NOT NULL,
    descripcion     TEXT,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_combos_activo     ON combos(activo);
CREATE INDEX idx_combos_creado_por ON combos(creado_por);

-- -------------------------------------------------------------
-- 3. COMBO_PRODUCTOS  (tabla pivote)
--    Un combo puede tener muchos productos.
--    Un producto puede pertenecer a varios combos.
--    'cantidad' indica cuántas unidades de ese producto incluye.
-- -------------------------------------------------------------
CREATE TABLE combo_productos (
    id          SERIAL  PRIMARY KEY,
    combo_id    INT     NOT NULL REFERENCES combos(id)    ON DELETE CASCADE,
    producto_id INT     NOT NULL REFERENCES productos(id) ON DELETE RESTRICT,
    cantidad    INT     NOT NULL DEFAULT 1,

    CONSTRAINT uq_combo_producto UNIQUE (combo_id, producto_id),
    CONSTRAINT chk_cantidad_positiva CHECK (cantidad > 0)
);

CREATE INDEX idx_combo_productos_combo    ON combo_productos(combo_id);
CREATE INDEX idx_combo_productos_producto ON combo_productos(producto_id);

-- Vista útil: combo con su valor total estimado y lista de productos
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
LEFT JOIN combo_productos cp ON cp.combo_id    = c.id
LEFT JOIN productos       p  ON p.id           = cp.producto_id
GROUP BY c.id, c.nombre, c.descripcion, c.activo, c.creado_en;

-- -------------------------------------------------------------
-- 4. ACTUALIZAR TABLA RIFAS
--    - Agrega combo_id (reemplaza el campo premio de texto libre)
--    - max_por_persona: NULL = sin límite (mínimo 1 por diseño)
-- -------------------------------------------------------------

-- Quitar campo texto libre 'premio' (ahora lo representa el combo)
ALTER TABLE rifas DROP COLUMN premio;

-- Referenciar el combo que se sortea
ALTER TABLE rifas
    ADD COLUMN combo_id INT REFERENCES combos(id) ON DELETE RESTRICT;

-- Cambiar max_por_persona: NULL significa sin límite
ALTER TABLE rifas
    ALTER COLUMN max_por_persona DROP NOT NULL,
    ALTER COLUMN max_por_persona SET DEFAULT NULL;

-- Actualizar el CHECK: si no es NULL, debe ser >= 1
ALTER TABLE rifas
    DROP CONSTRAINT chk_max_por_persona;

ALTER TABLE rifas
    ADD CONSTRAINT chk_max_por_persona CHECK (
        max_por_persona IS NULL OR max_por_persona >= 1
    );

CREATE INDEX idx_rifas_combo ON rifas(combo_id);

-- -------------------------------------------------------------
-- 5. NUEVO ROL: MANAGER
--    Privilegios:
--      - Crear y editar productos
--      - Crear y editar combos
--      - Crear rifa y configurarla
--      - Agregar o eliminar números disponibles de una rifa
--      - Ejecutar el sorteo
-- -------------------------------------------------------------
INSERT INTO roles (nombre, descripcion) VALUES (
    'manager',
    'Gestor de rifas: crea productos, combos, maneja números y ejecuta sorteos'
)
ON CONFLICT (nombre) DO NOTHING;

-- -------------------------------------------------------------
-- 6. ACTUALIZAR TRIGGER DE LÍMITE DE PARTICIPACIÓN
--    NULL en max_por_persona = sin límite (solo mínimo 1).
--    Se reemplaza la función existente en V3.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_validar_limite_participacion ON participaciones;

CREATE OR REPLACE FUNCTION fn_validar_limite_participacion()
RETURNS TRIGGER AS $$
DECLARE
    v_max       INT;
    v_actuales  INT;
    v_nombre    VARCHAR;
BEGIN
    SELECT max_por_persona, nombre
    INTO v_max, v_nombre
    FROM rifas
    WHERE id = NEW.rifa_id;

    -- Si max_por_persona es NULL, no hay límite superior → permitir siempre
    IF v_max IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO v_actuales
    FROM participaciones
    WHERE usuario_id = NEW.usuario_id
      AND rifa_id    = NEW.rifa_id;

    IF v_actuales >= v_max THEN
        RAISE EXCEPTION
            'El usuario ya tiene % número(s) en la rifa "%" (máximo permitido: %)',
            v_actuales, v_nombre, v_max;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_limite_participacion
    BEFORE INSERT ON participaciones
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_limite_participacion();

-- -------------------------------------------------------------
-- 7. AUDITORÍA — registrar la migración V4
-- -------------------------------------------------------------
INSERT INTO auditoria (usuario_id, accion, tabla_afectada, registro_id, detalle)
SELECT
    id,
    'MIGRACION_V4',
    NULL,
    NULL,
    'Migración V4 aplicada: productos, combos, rol manager, límite sin restricción'
FROM usuarios
WHERE email = 'admin@rifas.com';
