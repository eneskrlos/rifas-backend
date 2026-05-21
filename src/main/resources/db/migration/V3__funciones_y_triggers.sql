-- =============================================================
--  SISTEMA DE RIFAS — V3: Funciones y Triggers
--  Valores de ENUMs en MAYÚSCULAS
-- =============================================================

-- -------------------------------------------------------------
-- FUNCIÓN 1: Generar números al activar la rifa
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_generar_numeros_rifa()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'ACTIVA' AND OLD.estado = 'BORRADOR' THEN
        INSERT INTO numeros_rifa (rifa_id, numero, estado)
        SELECT NEW.id, gs.num, 'DISPONIBLE'
        FROM generate_series(1, NEW.total_numeros) AS gs(num);

        INSERT INTO auditoria (usuario_id, accion, tabla_afectada, registro_id, detalle)
        VALUES (
            NEW.creado_por,
            'RIFA_ACTIVADA',
            'rifas',
            NEW.id,
            FORMAT('Rifa "%s" activada. %s numeros generados (1..%s)',
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
-- FUNCIÓN 2: Validar límite de participación
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_limite_participacion()
RETURNS TRIGGER AS $$
DECLARE
    v_max      INT;
    v_actuales INT;
    v_nombre   VARCHAR;
BEGIN
    SELECT max_por_persona, nombre
    INTO v_max, v_nombre
    FROM rifas
    WHERE id = NEW.rifa_id;

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
            'El usuario ya tiene % numero(s) en la rifa "%" (maximo permitido: %)',
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
-- FUNCIÓN 3: Reservar número al participar
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reservar_numero()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE numeros_rifa
    SET estado = 'RESERVADO'
    WHERE id = NEW.numero_rifa_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reservar_numero_al_participar
    AFTER INSERT ON participaciones
    FOR EACH ROW
    EXECUTE FUNCTION fn_reservar_numero();
