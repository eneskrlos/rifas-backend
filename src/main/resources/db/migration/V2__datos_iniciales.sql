-- =============================================================
--  SISTEMA DE RIFAS — V2: Datos iniciales (seed)
--  Insertar roles y el usuario administrador por defecto.
-- =============================================================

-- -------------------------------------------------------------
-- Roles del sistema
-- ON CONFLICT DO NOTHING → si ya existen, no falla.
-- -------------------------------------------------------------
INSERT INTO roles (nombre, descripcion) VALUES
    ('admin',        'Administrador con acceso total al sistema'),
    ('participante', 'Usuario que puede registrarse y elegir números')
ON CONFLICT (nombre) DO NOTHING;

-- -------------------------------------------------------------
-- Usuario administrador por defecto
-- IMPORTANTE: Cambiar la contraseña en el primer login.
-- El hash corresponde a: Admin1234! (BCrypt, cost=12)
-- En producción, generar el hash con BCryptPasswordEncoder.
-- -------------------------------------------------------------
INSERT INTO usuarios (rol_id, nombre_completo, email, password_hash)
SELECT
    r.id,
    'Administrador del Sistema',
    'admin@rifas.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4oMUMVKlWq'
FROM roles r
WHERE r.nombre = 'admin'
ON CONFLICT (email) DO NOTHING;

-- Registrar en auditoría solo si el admin fue recién insertado
INSERT INTO auditoria (usuario_id, accion, tabla_afectada, registro_id, detalle)
SELECT
    u.id,
    'SEED_INICIAL',
    'usuarios',
    u.id,
    'Usuario administrador creado durante inicialización del sistema'
FROM usuarios u
WHERE u.email = 'admin@rifas.com'
  AND NOT EXISTS (
      SELECT 1 FROM auditoria a
      WHERE a.accion = 'SEED_INICIAL'
        AND a.tabla_afectada = 'usuarios'
  );
