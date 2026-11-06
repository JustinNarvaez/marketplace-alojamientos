-- =====================================================================
-- V3: politicas de cancelacion iniciales
--
-- Motivo: publicacion.id_politica_cancelacion es NOT NULL, pero la tabla
-- politica_cancelacion estaba vacia: no se podia crear ninguna
-- publicacion. SCRUM-27 asigna FLEXIBLE a toda publicacion nueva;
-- elegir otra politica es SCRUM-42.
--
-- Como se leen las columnas (las usara SCRUM-43, cancelar reserva):
--   dias_antes_reembolso_total     cancelando con al menos estos dias de
--                                  anticipacion a la llegada, se devuelve el 100 %
--   porcentaje_devolucion_parcial  cancelando despues de ese plazo, pero antes
--                                  del dia de llegada
--   porcentaje_devolucion_tardia   cancelando el dia de llegada o despues
--
-- Historia: SCRUM-27. V1 y V2 son inmutables: ya estan aplicadas.
-- =====================================================================

INSERT INTO politica_cancelacion
    (nombre, descripcion, dias_antes_reembolso_total,
     porcentaje_devolucion_parcial, porcentaje_devolucion_tardia)
VALUES
    ('FLEXIBLE',
     'Reembolso total si cancelas hasta 1 día antes de la llegada. Después no hay reembolso.',
     1, 0.00, 0.00),
    ('MODERADA',
     'Reembolso total si cancelas hasta 5 días antes de la llegada. Después se devuelve el 50 % hasta el día anterior a la llegada.',
     5, 50.00, 0.00),
    ('ESTRICTA',
     'Reembolso total si cancelas hasta 14 días antes de la llegada. Después se devuelve el 50 % hasta el día anterior a la llegada.',
     14, 50.00, 0.00);
