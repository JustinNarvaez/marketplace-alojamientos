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
-- Publicaciones (una por estado util para probar), 11 en total en 10
-- ciudades (varias con tilde, a proposito: el buscador de Inicio.tsx
-- ahora ignora tildes ademas de mayusculas, ver V4__extension_unaccent.sql):
--   Apartamento en El Poblado      (Mario) ACTIVA     FLEXIBLE  reserva inmediata, con extras y un bloqueo
--   Casa campestre en Guatape      (Mario) ACTIVA     MODERADA  requiere aprobacion del anfitrion
--   Cabana en La Calera            (Mario) BORRADOR   ESTRICTA  (no se puede cotizar)
--   Finca en Villa de Leyva        (Mario) ACTIVA     FLEXIBLE  requiere aprobacion del anfitrion
--   Loft en Laureles (Medellin)    (Mario) ACTIVA     MODERADA  reserva inmediata
--   Habitacion en Chapinero        (Laura) ACTIVA     FLEXIBLE  reserva inmediata
--   Apartamento en Bocagrande      (Laura) PAUSADA    MODERADA  (no se puede cotizar)
--   Casa en Cali                   (Laura) ACTIVA     MODERADA  reserva inmediata
--   Apartahotel en Santa Marta     (Laura) ACTIVA     FLEXIBLE  reserva inmediata
--   Cabana en Salento              (Diego) ACTIVA     FLEXIBLE  reserva inmediata
--   Apartamento en Bogota          (Diego) ACTIVA     MODERADA  reserva inmediata
--
-- Reservas (SCRUM-38/41), una por cada estado que la app de verdad usa
-- (no COMPLETADA: nada en el codigo la asigna todavia; una CONFIRMADA
-- con fecha_salida pasada ya cuenta como estancia terminada para resenar):
--   Carlos  en El Poblado     CONFIRMADA            (+12 a +15 dias)
--   Carlos  en Guatape        PENDIENTE_APROBACION  (+30 a +33 dias, respalda la notificacion de abajo)
--   Sofia   en Chapinero      CONFIRMADA, pasada     (-20 a -18 dias) + resena
--   Diego   en El Poblado     CONFIRMADA, pasada     (-40 a -37 dias) + resena
--   Sofia   en Casa en Cali   CANCELADA              (+3 a +6 dias, devolucion parcial del 50 %)
--   Diego   en Villa de Leyva RECHAZADA              (+25 a +28 dias)
--
-- Notificaciones (SCRUM-45): Carlos tiene 3 (2 sin leer) y Mario 2 (1 sin leer).
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

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion@prueba.com', 'Finca en Villa de Leyva',
    'Finca campestre con caballos y vista a las montañas de Boyacá. El anfitrión revisa cada solicitud.',
    'FINCA', 10::SMALLINT, 'Villa de Leyva', 'Vereda Monquirá, km 2', NULL,
    280000, 70000, 'ACTIVA', 'FLEXIBLE',
    ARRAY['WiFi', 'Parqueadero'],
    ARRAY['No se permiten fiestas', 'Mascotas con aviso previo'],
    ARRAY['https://picsum.photos/seed/villadeleyva1/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion@prueba.com', 'Loft en Laureles',
    'Loft moderno de un ambiente, ideal para estadías cortas cerca al estadio.',
    'APARTAMENTO', 2::SMALLINT, 'Medellín', 'Cra 76 # 34-10', 'Laureles',
    150000, 25000, 'ACTIVA', 'MODERADA',
    ARRAY['WiFi', 'Aire acondicionado', 'Lavadora'],
    ARRAY['No fumar dentro del loft'],
    ARRAY['https://picsum.photos/seed/laureles1/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion2@prueba.com', 'Casa en Cali',
    'Casa amplia de dos pisos en barrio residencial, cerca a la Avenida Sexta.',
    'CASA', 6::SMALLINT, 'Cali', 'Av. 6 Norte # 24-18', NULL,
    250000, 50000, 'ACTIVA', 'MODERADA',
    ARRAY['WiFi', 'Parqueadero', 'Cocina equipada'],
    ARRAY['No mascotas'],
    ARRAY['https://picsum.photos/seed/cali1/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'anfitrion2@prueba.com', 'Apartahotel en Santa Marta',
    'Apartahotel a cinco minutos de la playa, con servicio de aseo incluido.',
    'APARTAMENTO', 4::SMALLINT, 'Santa Marta', 'Cra 1 # 19-20', 'El Rodadero',
    310000, 45000, 'ACTIVA', 'FLEXIBLE',
    ARRAY['WiFi', 'Piscina', 'Aire acondicionado'],
    ARRAY['Check-out antes de las 11 a. m.'],
    ARRAY['https://picsum.photos/seed/santamarta1/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'mixto@prueba.com', 'Cabaña en Salento',
    'Cabaña de madera rodeada de palmas de cera, a pocos minutos del pueblo.',
    'CABANA', 4::SMALLINT, 'Salento', 'Vía al Valle de Cocora, km 4', NULL,
    230000, 35000, 'ACTIVA', 'FLEXIBLE',
    ARRAY['WiFi', 'Parqueadero'],
    ARRAY['Silencio después de las 9 p. m.'],
    ARRAY['https://picsum.photos/seed/salento1/800/500']);

SELECT pg_temp.publicacion_de_prueba(
    'mixto@prueba.com', 'Apartamento en Bogotá',
    'Apartamento en la Zona Rosa, a pasos de centros comerciales y restaurantes.',
    'APARTAMENTO', 3::SMALLINT, 'Bogotá', 'Cl 82 # 12-15', 'Zona Rosa',
    200000, 35000, 'ACTIVA', 'MODERADA',
    ARRAY['WiFi', 'Aire acondicionado'],
    ARRAY['No fiestas', 'No mascotas'],
    ARRAY['https://picsum.photos/seed/bogota1/800/500']);

-- Casa campestre en Guatapé pasa a requerir aprobación del anfitrión,
-- para que la reserva PENDIENTE_APROBACION de más abajo (y las
-- notificaciones de ejemplo que ya traía este script) tengan sentido.
UPDATE publicacion p
SET reserva_inmediata = FALSE
FROM alojamiento a, usuario u
WHERE p.id_alojamiento = a.id_alojamiento
  AND a.id_anfitrion = u.id_usuario
  AND u.email = 'anfitrion@prueba.com'
  AND p.titulo = 'Casa campestre en Guatapé';

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

-- ---------------------------------------------------------------------
-- Reservas (SCRUM-38/41). Funcion temporal que calcula el mismo desglose
-- que congelaria ReservaService.crear() (precio y tarifa de limpieza
-- vigentes, servicio al 10 %, sin descuento: todas duran menos de 7
-- noches) y, si la reserva es CONFIRMADA, crea tambien el periodo
-- RESERVADO en disponibilidad (DisponibilidadService.confirmarReservaYBloquear
-- lo haria al aprobarla). Solo inserta si la clave de idempotencia
-- todavia no existe, para poder correr el script varias veces.
-- ---------------------------------------------------------------------
CREATE FUNCTION pg_temp.reserva_de_prueba(
    p_correo_huesped   TEXT,
    p_correo_anfitrion TEXT,
    p_titulo           TEXT,
    p_dias_llegada     INTEGER,  -- relativo a CURRENT_DATE; negativo = ya paso
    p_noches           INTEGER,
    p_huespedes        SMALLINT,
    p_estado           TEXT,
    p_clave            TEXT
) RETURNS INTEGER LANGUAGE plpgsql AS $$
DECLARE
    v_huesped     INTEGER;
    v_alojamiento INTEGER;
    v_publicacion INTEGER;
    v_precio      NUMERIC;
    v_limpieza    NUMERIC;
    v_llegada     DATE;
    v_salida      DATE;
    v_subtotal    NUMERIC;
    v_servicio    NUMERIC;
    v_total       NUMERIC;
    v_reserva     INTEGER;
    v_dias_total  SMALLINT;
    v_pct_parcial NUMERIC;
    v_pct_tardia  NUMERIC;
    v_dias_antes  INTEGER;
    v_pct         NUMERIC;
BEGIN
    SELECT id_reserva INTO v_reserva FROM reserva WHERE clave_idempotencia = p_clave;
    IF v_reserva IS NOT NULL THEN
        RETURN v_reserva;
    END IF;

    SELECT id_usuario INTO v_huesped FROM usuario WHERE email = p_correo_huesped;

    SELECT p.id_publicacion, p.id_alojamiento, p.precio_noche, p.tarifa_limpieza
    INTO v_publicacion, v_alojamiento, v_precio, v_limpieza
    FROM publicacion p
    JOIN alojamiento a ON a.id_alojamiento = p.id_alojamiento
    JOIN usuario u ON u.id_usuario = a.id_anfitrion
    WHERE u.email = p_correo_anfitrion AND p.titulo = p_titulo;

    v_llegada  := CURRENT_DATE + p_dias_llegada;
    v_salida   := v_llegada + p_noches;
    v_subtotal := v_precio * p_noches;
    v_servicio := round(v_subtotal * 0.10, 2);
    v_total    := v_subtotal + v_limpieza + v_servicio;

    INSERT INTO reserva (id_huesped, id_alojamiento, id_publicacion, fecha_llegada, fecha_salida,
                         numero_huespedes, estado, clave_idempotencia, precio_noche_aplicado,
                         numero_noches, subtotal_alojamiento, tarifa_limpieza_aplicada,
                         tarifa_servicio_aplicada, descuento, total)
    VALUES (v_huesped, v_alojamiento, v_publicacion, v_llegada, v_salida, p_huespedes, p_estado,
            p_clave, v_precio, p_noches, v_subtotal, v_limpieza, v_servicio, 0, v_total)
    RETURNING id_reserva INTO v_reserva;

    -- Solo una CONFIRMADA bloquea el periodo (igual que confirmarReservaYBloquear);
    -- una PENDIENTE_APROBACION, RECHAZADA o CANCELADA nunca lo tuvo.
    IF p_estado = 'CONFIRMADA' THEN
        INSERT INTO disponibilidad (id_alojamiento, fecha_inicio, fecha_fin, estado)
        VALUES (v_alojamiento, v_llegada, v_salida, 'RESERVADO');
    END IF;

    -- Devolucion simulada con la misma regla por tramos de PoliticaCancelacion.calcularDevolucion().
    IF p_estado = 'CANCELADA' THEN
        SELECT pc.dias_antes_reembolso_total, pc.porcentaje_devolucion_parcial, pc.porcentaje_devolucion_tardia
        INTO v_dias_total, v_pct_parcial, v_pct_tardia
        FROM politica_cancelacion pc
        JOIN publicacion p ON p.id_politica_cancelacion = pc.id_politica
        WHERE p.id_publicacion = v_publicacion;

        v_dias_antes := v_llegada - CURRENT_DATE;
        v_pct := CASE
            WHEN v_dias_antes >= v_dias_total THEN 100
            WHEN v_dias_antes >= 1 THEN v_pct_parcial
            ELSE v_pct_tardia
        END;

        INSERT INTO cancelacion (id_reserva, id_usuario, fecha_cancelacion, motivo, valor_devolucion, estado_final)
        VALUES (v_reserva, v_huesped, now(), 'Cambio de planes', round(v_total * v_pct / 100, 2), 'CANCELADA');
    END IF;

    RETURN v_reserva;
END;
$$;

SELECT pg_temp.reserva_de_prueba(
    'huesped@prueba.com', 'anfitrion@prueba.com', 'Apartamento en El Poblado',
    12, 3, 2::SMALLINT, 'CONFIRMADA', 'seed-carlos-poblado-confirmada');

SELECT pg_temp.reserva_de_prueba(
    'huesped@prueba.com', 'anfitrion@prueba.com', 'Casa campestre en Guatapé',
    30, 3, 2::SMALLINT, 'PENDIENTE_APROBACION', 'seed-carlos-guatape-pendiente');

SELECT pg_temp.reserva_de_prueba(
    'huesped2@prueba.com', 'anfitrion2@prueba.com', 'Habitación en Chapinero',
    -20, 2, 1::SMALLINT, 'CONFIRMADA', 'seed-sofia-chapinero-confirmada');

SELECT pg_temp.reserva_de_prueba(
    'mixto@prueba.com', 'anfitrion@prueba.com', 'Apartamento en El Poblado',
    -40, 3, 2::SMALLINT, 'CONFIRMADA', 'seed-diego-poblado-confirmada');

SELECT pg_temp.reserva_de_prueba(
    'huesped2@prueba.com', 'anfitrion2@prueba.com', 'Casa en Cali',
    3, 3, 2::SMALLINT, 'CANCELADA', 'seed-sofia-cali-cancelada');

SELECT pg_temp.reserva_de_prueba(
    'mixto@prueba.com', 'anfitrion@prueba.com', 'Finca en Villa de Leyva',
    25, 3, 2::SMALLINT, 'RECHAZADA', 'seed-diego-villadeleyva-rechazada');

-- ---------------------------------------------------------------------
-- Reseñas (SCRUM-46) sobre las dos estancias ya terminadas (CONFIRMADA
-- con fecha_salida pasada, ver Reserva.terminoLaEstancia()).
-- ---------------------------------------------------------------------
INSERT INTO resena (id_reserva, calificacion, comentario, fecha_publicacion, oculta)
SELECT r.id_reserva, 5,
       'Excelente atención, todo tal cual la publicación. ¡Volveríamos sin duda!',
       (r.fecha_salida + 1)::TIMESTAMPTZ, FALSE
FROM reserva r
WHERE r.clave_idempotencia = 'seed-sofia-chapinero-confirmada'
  AND NOT EXISTS (SELECT 1 FROM resena x WHERE x.id_reserva = r.id_reserva);

INSERT INTO resena (id_reserva, calificacion, comentario, fecha_publicacion, oculta)
SELECT r.id_reserva, 4,
       'Muy buena ubicación y el apartamento estaba impecable. El balcón es un plus.',
       (r.fecha_salida + 1)::TIMESTAMPTZ, FALSE
FROM reserva r
WHERE r.clave_idempotencia = 'seed-diego-poblado-confirmada'
  AND NOT EXISTS (SELECT 1 FROM resena x WHERE x.id_reserva = r.id_reserva);

-- ---------------------------------------------------------------------
-- Notificaciones (SCRUM-45) para probar el centro de notificaciones
-- mientras SCRUM-44 no las genere solas. Carlos y Mario tienen leidas y
-- sin leer; los demas no tienen ninguna (para ver "solo veo las mias").
-- No se duplican: se busca por usuario y titulo.
-- ---------------------------------------------------------------------
INSERT INTO notificacion (id_usuario, tipo_evento, titulo, mensaje, leida, fecha_creacion)
SELECT u.id_usuario, n.tipo, n.titulo, n.mensaje, n.leida, now() - n.hace
FROM usuario u
JOIN (VALUES
    ('huesped@prueba.com',   'RESERVA',     'Bienvenido a la plataforma',
     'Ya puedes buscar alojamientos y reservar.',                                          TRUE,  INTERVAL '3 days'),
    ('huesped@prueba.com',   'RESERVA',     'Tu reserva está pendiente',
     'El anfitrión de "Casa campestre en Guatape" debe aprobar tu solicitud.',             FALSE, INTERVAL '5 hours'),
    ('huesped@prueba.com',   'RESERVA',     'Reserva confirmada',
     'Tu reserva en "Apartamento en El Poblado" quedó confirmada. ¡Buen viaje!',           FALSE, INTERVAL '10 minutes'),
    ('anfitrion@prueba.com', 'PUBLICACION', 'Publicación activada',
     'Tu publicación "Casa campestre en Guatape" ya es visible para los huéspedes.',       TRUE,  INTERVAL '2 days'),
    ('anfitrion@prueba.com', 'RESERVA',     'Nueva solicitud de reserva',
     'Carlos quiere reservar "Casa campestre en Guatape". Revisa la solicitud.',           FALSE, INTERVAL '5 hours')
) AS n(email, tipo, titulo, mensaje, leida, hace) ON n.email = u.email
WHERE NOT EXISTS (
    SELECT 1 FROM notificacion x WHERE x.id_usuario = u.id_usuario AND x.titulo = n.titulo
);

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
