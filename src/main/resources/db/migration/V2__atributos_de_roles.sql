-- =====================================================================
-- V2: atributos propios del rol administrador
--
-- Motivo: en el modelo E-R la especializacion de usuario es solapada
-- (un mismo usuario puede ser huesped y anfitrion a la vez). Huesped y
-- anfitrion se justifican por sus relaciones propias: huesped realiza
-- reservas y anfitrion tiene alojamientos. Admin no tenia ni atributos
-- ni relaciones propias, asi que el subtipo no se sostenia. Estas dos
-- columnas le dan atributos propios.
--
-- Historia: SCRUM-61, escenario 3 (todo cambio de esquema se versiona
-- mediante migraciones). V1 es inmutable: ya esta aplicada en los
-- entornos locales del equipo y en el pipeline de CI.
-- =====================================================================

ALTER TABLE admin
    ADD COLUMN fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN activo           BOOLEAN     NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN admin.fecha_asignacion IS
    'Cuando se otorgo el rol administrativo. Es distinta de usuario.fecha_registro, que es cuando se creo la cuenta.';

COMMENT ON COLUMN admin.activo IS
    'Permite revocar el rol administrativo sin desactivar la cuenta de usuario.';
