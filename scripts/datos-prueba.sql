-- =====================================================================
-- Datos de prueba para DESARROLLO LOCAL y la sustentacion.
--
-- NO es una migracion: no va en db/migration, a proposito. Si lo fuera,
-- estos usuarios con clave conocida entrarian tambien en CI y en
-- cualquier despliegue. Cada integrante lo corre a mano en su base local.
--
-- Como correrlo (con Docker arriba y la app arrancada al menos una vez,
-- para que Flyway ya haya creado las tablas), desde la raiz del repo:
--
--   docker exec -i marketplace-db psql -U marketplace -d marketplace -v ON_ERROR_STOP=1 < scripts/datos-prueba.sql
--
-- Se puede correr las veces que se quiera:
--   * No duplica nada: cada insercion revisa si el dato ya existe.
--   * Los usuarios de prueba se "reparan": vuelven a clave Prueba123 y
--     cuenta ACTIVA aunque alguien los haya desactivado o les cambio la
--     clave probando SCRUM-24/26.
--   * No toca ningun otro usuario ni publicacion.
--
-- Todas las cuentas usan la clave: Prueba123
--
--   admin@prueba.com       ADMINISTRADOR
--   anfitrion@prueba.com   ANFITRION (Mario)  - 3 publicaciones
--   anfitrion2@prueba.com  ANFITRION (Laura)  - 2 publicaciones (para probar el 403 entre anfitriones)
--   huesped@prueba.com     HUESPED   (Carlos)
--   huesped2@prueba.com    HUESPED   (Sofia)
--   mixto@prueba.com       HUESPED y ANFITRION a la vez (jerarquia solapada)
--
-- Publicaciones (una por estado util para probar):
--   Apartamento en El Poblado   (Mario) ACTIVA     FLEXIBLE  con extras y un bloqueo
--   Casa campestre en Guatape   (Mario) ACTIVA     MODERADA
--   Cabana en La Calera         (Mario) BORRADOR   ESTRICTA  (no se puede cotizar)
--   Habitacion en Chapinero     (Laura) ACTIVA     FLEXIBLE
--   Apartamento en Bocagrande   (Laura) PAUSADA    MODERADA  (no se puede cotizar)
--
-- No crea reservas ni pagos: dependen de SCRUM-38 y SCRUM-55. Cuando
-- existan, se agregan aqui.
-- =====================================================================

SET client_encoding = 'UTF8';

BEGIN;

-- ---------------------------------------------------------------------
-- Usuarios. Hash BCrypt (costo 10) de "Prueba123", el mismo formato que
-- genera BCryptPasswordEncoder de Spring Security.
-- ---------------------------------------------------------------------
INSERT INTO usuario (nombre, apellido, email, contrasena_hash, telefono, estado_cuenta)
VALUES
    ('Ana',    'Admin',    'admin@prueba.com',      '$2a$10$B1Su2ewD9ZTnwNYTXsEkMuDqGje083vvaM1XoAV0Mp0qSvdVGyJGy', '3000000001', 'ACTIVA'),
    ('Mario',  'Gomez',    'anfitrion@prueba.com',  '$2a$10$B1Su2ewD9ZTnwNYTXsEkMuDqGje083vvaM1XoAV0Mp0qSvdVGyJGy', '3000000002', 'ACTIVA'),
    ('Laura',  'Ruiz',     'anfitrion2@prueba.com', '$2a$10$B1Su2ewD9ZTnwNYTXsEkMuDqGje083vvaM1XoAV0Mp0qSvdVGyJGy', '3000000003', 'ACTIVA'),
    ('Carlos', 'Perez',    'huesped@prueba.com',    '$2a$10$B1Su2ewD9ZTnwNYTXsEkMuDqGje083vvaM1XoAV0Mp0qSvdVGyJGy', '3000000004', 'ACTIVA'),
    ('Sofia',  'Martinez', 'huesped2@prueba.com',   '$2a$10$B1Su2ewD9ZTnwNYTXsEkMuDqGje083vvaM1XoAV0Mp0qSvdVGyJGy', '3000000005', 'ACTIVA'),
    ('Diego',  'Lopez',    'mixto@prueba.com',      '$2a$10$B1Su2ewD9ZTnwNYTXsEkMuDqGje083vvaM1XoAV0Mp0qSvdVGyJGy', '3000000006', 'ACTIVA')
ON CONFLICT (email) DO UPDATE
    SET contrasena_hash = EXCLUDED.contrasena_hash,
        estado_cuenta   = 'ACTIVA';

-- Roles: jerarquia solapada, cada rol es una fila en su tabla (PK = id_usuario).
INSERT INTO admin (id_usuario)
SELECT id_usuario FROM usuario WHERE email = 'admin@prueba.com'
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO anfitrion (id_usuario)
SELECT id_usuario FROM usuario WHERE email IN ('anfitrion@prueba.com', 'anfitrion2@prueba.com', 'mixto@prueba.com')
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO huesped (id_usuario)
SELECT id_usuario FROM usuario WHERE email IN ('huesped@prueba.com', 'huesped2@prueba.com', 'mixto@prueba.com')
ON CONFLICT (id_usuario) DO NOTHING;

-- ---------------------------------------------------------------------
-- Catalogo de servicios (amenidades).
-- ---------------------------------------------------------------------
INSERT INTO servicio (nombre, categoria)
VALUES
    ('WiFi',               'Conectividad'),
    ('Cocina equipada',    'Espacios'),
    ('Parqueadero',        'Estacionamiento'),
    ('Piscina',            'Recreacion'),
    ('Aire acondicionado', 'Clima'),
    ('Lavadora',           'Espacios')
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- Funcion temporal (pg_temp: desaparece al cerrar la sesion) que crea una
-- publicacion completa solo si ese anfitrion todavia no tiene una con el
-- mismo titulo. Asi el script se puede correr varias veces sin duplicar.
-- ---------------------------------------------------------------------
CREATE FUNCTION pg_temp.publicacion_de_prueba(
    p_correo      TEXT,
    p_titulo      TEXT,
    p_descripcion TEXT,
    p_tipo        TEXT,
    p_capacidad   SMALLINT,
    p_ciudad      TEXT,
    p_direccion   TEXT,
    p_ubicacion   TEXT,
    p_precio      NUMERIC,
    p_limpieza    NUMERIC,
    p_estado      TEXT,
    p_politica    TEXT,
    p_servicios   TEXT[],
    p_reglas      TEXT[],
    p_imagenes    TEXT[]
) RETURNS INTEGER LANGUAGE plpgsql AS $$
DECLARE
    v_anfitrion   INTEGER;
    v_alojamiento INTEGER;
    v_publicacion INTEGER;
BEGIN
    SELECT id_usuario INTO v_anfitrion FROM usuario WHERE email = p_correo;

    SELECT p.id_publicacion INTO v_publicacion
    FROM publicacion p
    JOIN alojamiento a ON a.id_alojamiento = p.id_alojamiento
    WHERE a.id_anfitrion = v_anfitrion AND p.titulo = p_titulo;

    IF v_publicacion IS NOT NULL THEN
        RETURN v_publicacion;
    END IF;

    INSERT INTO alojamiento (id_anfitrion, tipo_alojamiento, capacidad, ciudad, direccion, ubicacion)
    VALUES (v_anfitrion, p_tipo, p_capacidad, p_ciudad, p_direccion, p_ubicacion)
    RETURNING id_alojamiento INTO v_alojamiento;

    INSERT INTO publicacion (id_alojamiento, id_politica_cancelacion, titulo, descripcion,
                             precio_noche, tarifa_limpieza, reserva_inmediata, estado, fecha_publicacion)
    SELECT v_alojamiento, id_politica, p_titulo, p_descripcion,
           p_precio, p_limpieza, TRUE, p_estado,
           CASE WHEN p_estado = 'BORRADOR' THEN NULL ELSE now() END
    FROM politica_cancelacion
    WHERE nombre = p_politica
    RETURNING id_publicacion INTO v_publicacion;

    INSERT INTO publicacion_servicio (id_publicacion, id_servicio)
    SELECT v_publicacion, id_servicio FROM servicio WHERE nombre = ANY (p_servicios);

    INSERT INTO regla_alojamiento (id_publicacion, descripcion, orden)
    SELECT v_publicacion, regla, orden::SMALLINT
    FROM unnest(p_reglas) WITH ORDINALITY AS r(regla, orden);

    -- La primera imagen es la portada (uq_imagen_una_portada: solo una por publicacion).
    INSERT INTO imagen (id_publicacion, ruta, orden, es_portada)
    SELECT v_publicacion, ruta, orden::SMALLINT, orden = 1
    FROM unnest(p_imagenes) WITH ORDINALITY AS i(ruta, orden);

    RETURN v_publicacion;
END;
$$;

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion@prueba.com', 'Apartamento en El Poblado',
    'Apartamento moderno a dos cuadras del parque Lleras, con balcón y vista a la ciudad.',
    'APARTAMENTO', 4::SMALLINT, 'Medellín', 'Cra 43A # 10-20', 'El Poblado',
    180000, 40000, 'ACTIVA', 'FLEXIBLE',
    ARRAY['WiFi', 'Cocina equipada', 'Aire acondicionado'],
    ARRAY['No fumar dentro del apartamento', 'No se permiten fiestas', 'Check-in después de las 3 p. m.'],
    ARRAY['https://picsum.photos/seed/poblado1/800/500', 'https://picsum.photos/seed/poblado2/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion@prueba.com', 'Casa campestre en Guatapé',
    'Casa con piscina y vista a la represa, ideal para grupos y familias.',
    'CASA', 8::SMALLINT, 'Guatapé', 'Vereda El Roble, km 3', 'Cerca a la piedra del Peñol',
    350000, 80000, 'ACTIVA', 'MODERADA',
    ARRAY['WiFi', 'Piscina', 'Parqueadero', 'Cocina equipada', 'Lavadora'],
    ARRAY['Mascotas permitidas con aviso previo', 'Silencio después de las 10 p. m.'],
    ARRAY['https://picsum.photos/seed/guatape1/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion@prueba.com', 'Cabaña en La Calera',
    'Cabaña de madera en el bosque, con chimenea. Todavía en borrador.',
    'CABANA', 2::SMALLINT, 'La Calera', 'Vía La Calera km 8', NULL,
    220000, 30000, 'BORRADOR', 'ESTRICTA',
    ARRAY['WiFi'],
    ARRAY['Llevar ropa abrigada'],
    ARRAY[]::TEXT[]);

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion2@prueba.com', 'Habitación en Chapinero',
    'Habitación privada en apartamento compartido, cerca a la zona G.',
    'HABITACION', 1::SMALLINT, 'Bogotá', 'Calle 57 # 5-20', 'Chapinero',
    90000, 0, 'ACTIVA', 'FLEXIBLE',
    ARRAY['WiFi', 'Lavadora'],
    ARRAY['Solo una persona', 'No visitas después de las 9 p. m.'],
    ARRAY['https://picsum.photos/seed/chapinero1/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion2@prueba.com', 'Apartamento en Bocagrande',
    'Apartamento frente al mar. Pausado por temporada de mantenimiento.',
    'APARTAMENTO', 5::SMALLINT, 'Cartagena', 'Av. San Martín # 6-40', 'Bocagrande',
    420000, 60000, 'PAUSADA', 'MODERADA',
    ARRAY['WiFi', 'Piscina', 'Aire acondicionado'],
    ARRAY['No fumar'],
    ARRAY['https://picsum.photos/seed/bocagrande1/800/500']);

-- ---------------------------------------------------------------------
-- Servicios adicionales con precio (SCRUM-37) en El Poblado.
-- ---------------------------------------------------------------------
INSERT INTO servicio_adicional (id_publicacion, nombre, descripcion, precio)
SELECT p.id_publicacion, e.nombre, e.descripcion, e.precio
FROM publicacion p
JOIN alojamiento a ON a.id_alojamiento = p.id_alojamiento
JOIN usuario u ON u.id_usuario = a.id_anfitrion
CROSS JOIN (VALUES
    ('Desayuno',              'Desayuno típico para todos los huéspedes, una vez por estancia.', 20000::NUMERIC),
    ('Parqueadero',           'Un cupo en el sótano del edificio.',                               15000::NUMERIC),
    ('Traslado al aeropuerto', NULL,                                                              60000::NUMERIC)
) AS e(nombre, descripcion, precio)
WHERE u.email = 'anfitrion@prueba.com' AND p.titulo = 'Apartamento en El Poblado'
ON CONFLICT (id_publicacion, nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- Calendario (SCRUM-31) de El Poblado. Fechas RELATIVAS a hoy para que
-- sigan siendo futuras sin importar cuando se corra el script:
--   * DISPONIBLE dentro de 10 a 40 dias.
--   * BLOQUEADO  dentro de 20 a 25 dias (cotizar esas fechas da 409).
-- Solo se crean si el alojamiento todavia no tiene ningun periodo.
-- ---------------------------------------------------------------------
INSERT INTO disponibilidad (id_alojamiento, fecha_inicio, fecha_fin, estado)
SELECT a.id_alojamiento, CURRENT_DATE + d.desde, CURRENT_DATE + d.hasta, d.estado
FROM publicacion p
JOIN alojamiento a ON a.id_alojamiento = p.id_alojamiento
JOIN usuario u ON u.id_usuario = a.id_anfitrion
CROSS JOIN (VALUES (10, 20, 'DISPONIBLE'), (20, 25, 'BLOQUEADO'), (25, 40, 'DISPONIBLE')) AS d(desde, hasta, estado)
WHERE u.email = 'anfitrion@prueba.com' AND p.titulo = 'Apartamento en El Poblado'
  AND NOT EXISTS (SELECT 1 FROM disponibilidad x WHERE x.id_alojamiento = a.id_alojamiento);

COMMIT;

-- Resumen de lo que quedo cargado.
SELECT u.email, string_agg(r.rol, ' + ' ORDER BY r.rol) AS roles, u.estado_cuenta
FROM usuario u
JOIN (
    SELECT id_usuario, 'ADMINISTRADOR' AS rol FROM admin
    UNION ALL SELECT id_usuario, 'ANFITRION' FROM anfitrion
    UNION ALL SELECT id_usuario, 'HUESPED'   FROM huesped
) r ON r.id_usuario = u.id_usuario
WHERE u.email LIKE '%@prueba.com'
GROUP BY u.email, u.estado_cuenta
ORDER BY u.email;

SELECT p.id_publicacion, p.titulo, p.estado, u.email AS anfitrion
FROM publicacion p
JOIN alojamiento a ON a.id_alojamiento = p.id_alojamiento
JOIN usuario u ON u.id_usuario = a.id_anfitrion
WHERE u.email LIKE '%@prueba.com'
ORDER BY p.id_publicacion;
