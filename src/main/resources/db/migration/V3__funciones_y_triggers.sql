-- =============================================================
--  SISTEMA DE RIFAS — V3: Funciones y Triggers
--  1. Auto-generar números al activar una rifa
--  2. Validar límite de números por persona
-- =============================================================

-- -------------------------------------------------------------
-- FUNCIÓN 1: Generar números de la rifa automáticamente
--   Se ejecuta cuando una rifa pasa de 'borrador' a 'activa'.
--   Inserta N filas en numeros_rifa (1..total_numeros).
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_generar_numeros_rifa()
RETURNS TRIGGER AS $$
BEGIN
    -- Solo actuar cuando el estado cambia a 'activa'
    IF NEW.estado = 'activa' AND OLD.estado = 'borrador' THEN

        -- Insertar números del 1 al total_numeros
        INSERT INTO numeros_rifa (rifa_id, numero, estado)
        SELECT NEW.id, gs.num, 'disponible'
        FROM generate_series(1, NEW.total_numeros) AS gs(num);

        -- Log de auditoría
        INSERT INTO auditoria (usuario_id, accion, tabla_afectada, registro_id, detalle)
        VALUES (
            NEW.creado_por,
            'RIFA_ACTIVADA',
            'rifas',
            NEW.id,
            FORMAT('Rifa "%s" activada. %s números generados (1..%s)',
                   NEW.nombre, NEW.total_numeros, NEW.total_numeros)
        );

    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generar_numeros_al_activar
    AFTER UPDATE OF estado ON rifas
    FOR EACH ROW
    EXECUTE FUNCTION fn_generar_numeros_rifa();

-- -------------------------------------------------------------
-- FUNCIÓN 2: Validar que un usuario no supere max_por_persona
--   Corre BEFORE INSERT en participaciones.
--   Doble capa de seguridad: la app valida, la BD garantiza.
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_limite_participacion()
RETURNS TRIGGER AS $$
DECLARE
    v_max       INT;
    v_actuales  INT;
    v_nombre    VARCHAR;
BEGIN
    -- Obtener el límite configurado para esta rifa
    SELECT max_por_persona, nombre
    INTO v_max, v_nombre
    FROM rifas
    WHERE id = NEW.rifa_id;

    -- Contar cuántos números ya tiene este usuario en esta rifa
    SELECT COUNT(*)
    INTO v_actuales
    FROM participaciones
    WHERE usuario_id = NEW.usuario_id
      AND rifa_id    = NEW.rifa_id;

    -- Rechazar si supera el límite
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
-- FUNCIÓN 3: Marcar número como 'reservado' al participar
--   Corre AFTER INSERT en participaciones.
--   Mantiene consistencia entre participaciones y numeros_rifa.
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reservar_numero()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE numeros_rifa
    SET estado = 'reservado'
    WHERE id = NEW.numero_rifa_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reservar_numero_al_participar
    AFTER INSERT ON participaciones
    FOR EACH ROW
    EXECUTE FUNCTION fn_reservar_numero();
