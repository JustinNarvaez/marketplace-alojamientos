-- =====================================================================
--  Marketplace Alojamientos - Inversiones LR
--  Modelo relacional / script DDL - PostgreSQL
--  Proyecto de Ingenieria de Software 1 - Universidad El Bosque
-- =====================================================================
--  Orden de creacion: las tablas referenciadas van primero.
--  Nomenclatura: snake_case, singular, sin tildes ni enies.
-- =====================================================================


-- =====================================================================
--  0. LIMPIEZA  (borra TODO el esquema y lo recrea desde cero)
-- =====================================================================
-- Necesaria para la restriccion EXCLUDE de la tabla reserva.
CREATE EXTENSION IF NOT EXISTS btree_gist;


-- =====================================================================
--  1. IDENTIDAD Y ACCESO
-- =====================================================================

CREATE TABLE usuario (
    id_usuario        SERIAL        PRIMARY KEY,
    nombre            VARCHAR(80)   NOT NULL,
    apellido          VARCHAR(80)   NOT NULL,
    email             VARCHAR(150)  NOT NULL UNIQUE,
    contrasena_hash   VARCHAR(255)  NOT NULL,
    telefono          VARCHAR(20),
    fecha_registro    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    estado_cuenta     VARCHAR(15)   NOT NULL DEFAULT 'ACTIVA',

    CONSTRAINT ck_usuario_estado
        CHECK (estado_cuenta IN ('ACTIVA', 'INACTIVA', 'RESTRINGIDA'))
);

-- Jerarquia solapada: un mismo usuario puede tener fila en varias
-- de estas tres tablas (p. ej. ser huesped y anfitrion a la vez).

CREATE TABLE huesped (
    id_usuario  INTEGER  PRIMARY KEY,

    CONSTRAINT fk_huesped_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
);

CREATE TABLE anfitrion (
    id_usuario  INTEGER  PRIMARY KEY,

    CONSTRAINT fk_anfitrion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
);

CREATE TABLE admin (
    id_usuario  INTEGER  PRIMARY KEY,

    CONSTRAINT fk_admin_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
);


-- =====================================================================
--  2. CATALOGOS INDEPENDIENTES
-- =====================================================================

CREATE TABLE servicio (
    id_servicio  SERIAL       PRIMARY KEY,
    nombre       VARCHAR(60)  NOT NULL UNIQUE,
    categoria    VARCHAR(40)
);

CREATE TABLE politica_cancelacion (
    id_politica                    SERIAL        PRIMARY KEY,
    nombre                         VARCHAR(20)   NOT NULL UNIQUE,
    descripcion                    TEXT          NOT NULL,
    dias_antes_reembolso_total     SMALLINT      NOT NULL,
    porcentaje_devolucion_parcial  NUMERIC(5,2)  NOT NULL,
    porcentaje_devolucion_tardia   NUMERIC(5,2)  NOT NULL,

    CONSTRAINT ck_politica_nombre
        CHECK (nombre IN ('FLEXIBLE', 'MODERADA', 'ESTRICTA')),
    CONSTRAINT ck_politica_dias
        CHECK (dias_antes_reembolso_total >= 0),
    CONSTRAINT ck_politica_pct_parcial
        CHECK (porcentaje_devolucion_parcial BETWEEN 0 AND 100),
    CONSTRAINT ck_politica_pct_tardia
        CHECK (porcentaje_devolucion_tardia BETWEEN 0 AND 100)
);


-- =====================================================================
--  3. OFERTA
-- =====================================================================

CREATE TABLE alojamiento (
    id_alojamiento    SERIAL        PRIMARY KEY,
    id_anfitrion      INTEGER       NOT NULL,
    tipo_alojamiento  VARCHAR(30)   NOT NULL,
    capacidad         SMALLINT      NOT NULL,
    ciudad            VARCHAR(80)   NOT NULL,
    direccion         VARCHAR(200)  NOT NULL,
    ubicacion         VARCHAR(200),
    fecha_registro    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_alojamiento_anfitrion
        FOREIGN KEY (id_anfitrion) REFERENCES anfitrion (id_usuario),
    CONSTRAINT ck_alojamiento_tipo
        CHECK (tipo_alojamiento IN
               ('CASA', 'APARTAMENTO', 'HABITACION', 'CABANA', 'FINCA')),
    CONSTRAINT ck_alojamiento_capacidad
        CHECK (capacidad > 0)
);

CREATE TABLE disponibilidad (
    id_disponibilidad  SERIAL       PRIMARY KEY,
    id_alojamiento     INTEGER      NOT NULL,
    fecha_inicio       DATE         NOT NULL,
    fecha_fin          DATE         NOT NULL,
    estado             VARCHAR(15)  NOT NULL,

    CONSTRAINT fk_disponibilidad_alojamiento
        FOREIGN KEY (id_alojamiento) REFERENCES alojamiento (id_alojamiento)
        ON DELETE CASCADE,
    CONSTRAINT ck_disponibilidad_estado
        CHECK (estado IN ('DISPONIBLE', 'RESERVADO', 'BLOQUEADO')),
    CONSTRAINT ck_disponibilidad_fechas
        CHECK (fecha_fin > fecha_inicio)
);

CREATE TABLE publicacion (
    id_publicacion           SERIAL         PRIMARY KEY,
    id_alojamiento           INTEGER        NOT NULL,
    id_politica_cancelacion  INTEGER        NOT NULL,
    titulo                   VARCHAR(150)   NOT NULL,
    descripcion              TEXT           NOT NULL,
    precio_noche             NUMERIC(12,2)  NOT NULL,
    tarifa_limpieza          NUMERIC(12,2)  NOT NULL DEFAULT 0,
    reserva_inmediata        BOOLEAN        NOT NULL DEFAULT TRUE,
    estado                   VARCHAR(20)    NOT NULL DEFAULT 'BORRADOR',
    fecha_creacion           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    fecha_publicacion        TIMESTAMPTZ,

    CONSTRAINT fk_publicacion_alojamiento
        FOREIGN KEY (id_alojamiento) REFERENCES alojamiento (id_alojamiento),
    CONSTRAINT fk_publicacion_politica
        FOREIGN KEY (id_politica_cancelacion)
        REFERENCES politica_cancelacion (id_politica),
    CONSTRAINT ck_publicacion_precio
        CHECK (precio_noche > 0),
    CONSTRAINT ck_publicacion_limpieza
        CHECK (tarifa_limpieza >= 0),
    CONSTRAINT ck_publicacion_estado
        CHECK (estado IN ('BORRADOR', 'PENDIENTE_REVISION', 'ACTIVA',
                          'PAUSADA', 'BLOQUEADA')),

    -- Redundante por si sola (id_publicacion ya es PK), pero necesaria
    -- para que reserva pueda apuntar con una FK compuesta y la base
    -- garantice que el alojamiento de la reserva es el de su publicacion.
    CONSTRAINT uq_publicacion_alojamiento
        UNIQUE (id_publicacion, id_alojamiento)
);

CREATE TABLE publicacion_servicio (
    id_publicacion  INTEGER  NOT NULL,
    id_servicio     INTEGER  NOT NULL,

    PRIMARY KEY (id_publicacion, id_servicio),

    CONSTRAINT fk_pubserv_publicacion
        FOREIGN KEY (id_publicacion) REFERENCES publicacion (id_publicacion)
        ON DELETE CASCADE,
    CONSTRAINT fk_pubserv_servicio
        FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio)
);

CREATE TABLE regla_alojamiento (
    id_regla        SERIAL        PRIMARY KEY,
    id_publicacion  INTEGER       NOT NULL,
    descripcion     VARCHAR(255)  NOT NULL,
    orden           SMALLINT      NOT NULL DEFAULT 1,

    CONSTRAINT fk_regla_publicacion
        FOREIGN KEY (id_publicacion) REFERENCES publicacion (id_publicacion)
        ON DELETE CASCADE,
    CONSTRAINT ck_regla_orden
        CHECK (orden > 0)
);

CREATE TABLE imagen (
    id_imagen       SERIAL        PRIMARY KEY,
    id_publicacion  INTEGER       NOT NULL,
    ruta            VARCHAR(500)  NOT NULL,
    orden           SMALLINT      NOT NULL DEFAULT 1,
    es_portada      BOOLEAN       NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_imagen_publicacion
        FOREIGN KEY (id_publicacion) REFERENCES publicacion (id_publicacion)
        ON DELETE CASCADE,
    CONSTRAINT ck_imagen_orden
        CHECK (orden > 0)
);

-- Indice unico PARCIAL: solo puede existir una portada por publicacion.
-- El WHERE hace que la restriccion aplique unicamente a las filas
-- con es_portada = TRUE.
CREATE UNIQUE INDEX uq_imagen_una_portada
    ON imagen (id_publicacion)
    WHERE es_portada;

CREATE TABLE servicio_adicional (
    id_servicio_adicional  SERIAL         PRIMARY KEY,
    id_publicacion         INTEGER        NOT NULL,
    nombre                 VARCHAR(100)   NOT NULL,
    descripcion            TEXT,
    precio                 NUMERIC(12,2)  NOT NULL,
    activo                 BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_servadic_publicacion
        FOREIGN KEY (id_publicacion) REFERENCES publicacion (id_publicacion)
        ON DELETE CASCADE,
    CONSTRAINT ck_servadic_precio
        CHECK (precio >= 0),
    CONSTRAINT uq_servadic_nombre
        UNIQUE (id_publicacion, nombre)
);


-- =====================================================================
--  4. RESERVAS
-- =====================================================================

CREATE TABLE reserva (
    id_reserva                SERIAL         PRIMARY KEY,
    id_huesped                INTEGER        NOT NULL,
    id_alojamiento            INTEGER        NOT NULL,
    id_publicacion            INTEGER        NOT NULL,
    fecha_llegada             DATE           NOT NULL,
    fecha_salida              DATE           NOT NULL,
    numero_huespedes          SMALLINT       NOT NULL,
    estado                    VARCHAR(25)    NOT NULL DEFAULT 'PENDIENTE_APROBACION',
    fecha_creacion            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    clave_idempotencia        VARCHAR(80)    NOT NULL UNIQUE,

    -- Snapshot: valores congelados al momento de reservar.
    -- Si el anfitrion cambia los precios despues, las reservas
    -- historicas conservan lo que realmente se cobro.
    precio_noche_aplicado     NUMERIC(12,2)  NOT NULL,
    numero_noches             SMALLINT       NOT NULL,
    subtotal_alojamiento      NUMERIC(12,2)  NOT NULL,
    tarifa_limpieza_aplicada  NUMERIC(12,2)  NOT NULL DEFAULT 0,
    tarifa_servicio_aplicada  NUMERIC(12,2)  NOT NULL DEFAULT 0,
    descuento                 NUMERIC(12,2)  NOT NULL DEFAULT 0,
    total                     NUMERIC(12,2)  NOT NULL,

    CONSTRAINT fk_reserva_huesped
        FOREIGN KEY (id_huesped) REFERENCES huesped (id_usuario),
    CONSTRAINT fk_reserva_alojamiento
        FOREIGN KEY (id_alojamiento) REFERENCES alojamiento (id_alojamiento),

    -- FK compuesta: garantiza que la publicacion de la reserva
    -- pertenece al mismo alojamiento que la reserva declara.
    CONSTRAINT fk_reserva_publicacion
        FOREIGN KEY (id_publicacion, id_alojamiento)
        REFERENCES publicacion (id_publicacion, id_alojamiento),

    CONSTRAINT ck_reserva_estado
        CHECK (estado IN ('PENDIENTE_APROBACION', 'CONFIRMADA', 'RECHAZADA',
                          'CANCELADA', 'COMPLETADA')),
    CONSTRAINT ck_reserva_fechas
        CHECK (fecha_salida > fecha_llegada),
    CONSTRAINT ck_reserva_huespedes
        CHECK (numero_huespedes > 0),
    CONSTRAINT ck_reserva_precio_noche
        CHECK (precio_noche_aplicado > 0),
    CONSTRAINT ck_reserva_noches
        CHECK (numero_noches > 0),
    CONSTRAINT ck_reserva_subtotal
        CHECK (subtotal_alojamiento >= 0),
    CONSTRAINT ck_reserva_descuento
        CHECK (descuento >= 0),
    CONSTRAINT ck_reserva_total
        CHECK (total >= 0)
);

-- Regla de negocio clave: no se pueden confirmar dos reservas
-- superpuestas para el mismo alojamiento. Se impone en la base,
-- no en el codigo. El rango '[)' incluye la llegada y excluye la
-- salida, de modo que un check-out y un check-in el mismo dia NO
-- se consideran solapados.
ALTER TABLE reserva
    ADD CONSTRAINT ex_reserva_sin_solapamiento
    EXCLUDE USING gist (
        id_alojamiento WITH =,
        daterange(fecha_llegada, fecha_salida, '[)') WITH &&
    )
    WHERE (estado = 'CONFIRMADA');

CREATE TABLE detalle_servicio_reserva (
    id_reserva             INTEGER        NOT NULL,
    id_servicio_adicional  INTEGER        NOT NULL,
    cantidad               SMALLINT       NOT NULL,
    precio_cobrado         NUMERIC(12,2)  NOT NULL,

    PRIMARY KEY (id_reserva, id_servicio_adicional),

    CONSTRAINT fk_detserv_reserva
        FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva)
        ON DELETE CASCADE,
    CONSTRAINT fk_detserv_servicio_adicional
        FOREIGN KEY (id_servicio_adicional)
        REFERENCES servicio_adicional (id_servicio_adicional),
    CONSTRAINT ck_detserv_cantidad
        CHECK (cantidad > 0),
    CONSTRAINT ck_detserv_precio
        CHECK (precio_cobrado >= 0)
);

CREATE TABLE pago (
    id_pago             SERIAL         PRIMARY KEY,
    id_reserva          INTEGER        NOT NULL,
    tipo                VARCHAR(12)    NOT NULL,
    referencia_externa  VARCHAR(100)   NOT NULL UNIQUE,
    monto               NUMERIC(12,2)  NOT NULL,
    moneda              CHAR(3)        NOT NULL DEFAULT 'COP',
    estado              VARCHAR(15)    NOT NULL,
    fecha               TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_pago_reserva
        FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva),
    CONSTRAINT ck_pago_tipo
        CHECK (tipo IN ('CARGO', 'REEMBOLSO')),
    CONSTRAINT ck_pago_estado
        CHECK (estado IN ('PENDIENTE', 'EXITOSO', 'FALLIDO')),
    CONSTRAINT ck_pago_monto
        CHECK (monto > 0)
);

CREATE TABLE cancelacion (
    id_cancelacion     SERIAL         PRIMARY KEY,
    id_reserva         INTEGER        NOT NULL UNIQUE,
    id_usuario         INTEGER        NOT NULL,
    fecha_cancelacion  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    motivo             VARCHAR(255),
    valor_devolucion   NUMERIC(12,2)  NOT NULL,
    estado_final       VARCHAR(20)    NOT NULL,

    CONSTRAINT fk_cancelacion_reserva
        FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva),
    CONSTRAINT fk_cancelacion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_cancelacion_valor
        CHECK (valor_devolucion >= 0)
);

CREATE TABLE resena (
    id_resena          SERIAL       PRIMARY KEY,
    id_reserva         INTEGER      NOT NULL UNIQUE,
    calificacion       SMALLINT     NOT NULL,
    comentario         TEXT,
    fecha_publicacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    oculta             BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_resena_reserva
        FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva),
    CONSTRAINT ck_resena_calificacion
        CHECK (calificacion BETWEEN 1 AND 5)
);


-- =====================================================================
--  5. TRANSVERSALES
-- =====================================================================

CREATE TABLE notificacion (
    id_notificacion  SERIAL        PRIMARY KEY,
    id_usuario       INTEGER       NOT NULL,
    tipo_evento      VARCHAR(30)   NOT NULL,
    titulo           VARCHAR(150)  NOT NULL,
    mensaje          TEXT          NOT NULL,
    leida            BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_creacion   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    referencia_tipo  VARCHAR(30),
    referencia_id    INTEGER,

    CONSTRAINT fk_notificacion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
);

CREATE TABLE log_auditoria (
    id_log               BIGSERIAL    PRIMARY KEY,

    -- Unico FK nullable del modelo: hay eventos sin actor humano
    -- (procesos automaticos, errores de integracion, intentos de
    -- login con un correo que no existe). ON DELETE SET NULL
    -- conserva el registro aunque se borre el usuario.
    id_usuario           INTEGER      NULL,

    tipo_evento          VARCHAR(30)  NOT NULL,
    entidad_afectada     VARCHAR(50),
    id_entidad_afectada  INTEGER,
    accion               VARCHAR(50)  NOT NULL,
    resultado            VARCHAR(20)  NOT NULL,
    detalle              JSONB,
    ip_origen            INET,
    fecha_hora           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_log_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT ck_log_tipo_evento
        CHECK (tipo_evento IN ('AUTENTICACION', 'PUBLICACION', 'RESERVA',
                               'CANCELACION', 'RESENA', 'EXPORTACION',
                               'ACCION_ADMIN', 'ERROR')),
    CONSTRAINT ck_log_resultado
        CHECK (resultado IN ('EXITO', 'FALLO'))
);


-- =====================================================================
--  6. INDICES DE APOYO A LAS CONSULTAS DEL MVP
-- =====================================================================

-- Busqueda: filtrar publicaciones activas por ciudad.
CREATE INDEX ix_alojamiento_ciudad      ON alojamiento (ciudad);
CREATE INDEX ix_publicacion_estado      ON publicacion (estado);

-- Consulta de disponibilidad y de reservas por alojamiento.
CREATE INDEX ix_disponibilidad_aloj     ON disponibilidad (id_alojamiento, fecha_inicio);
CREATE INDEX ix_reserva_alojamiento     ON reserva (id_alojamiento, fecha_llegada);
CREATE INDEX ix_reserva_huesped         ON reserva (id_huesped);

-- Historial y auditoria.
CREATE INDEX ix_log_fecha               ON log_auditoria (fecha_hora DESC);
CREATE INDEX ix_notificacion_usuario    ON notificacion (id_usuario, leida);
