-- SCRUM-55: PagoService.procesarEvento registra TipoEventoAuditoria.PAGO.
-- El CHECK de V1 no lo incluia; se reemplaza por uno que si lo tiene.
ALTER TABLE log_auditoria DROP CONSTRAINT ck_log_tipo_evento;

ALTER TABLE log_auditoria
    ADD CONSTRAINT ck_log_tipo_evento
    CHECK (tipo_evento IN ('AUTENTICACION', 'PUBLICACION', 'RESERVA', 'PAGO',
                           'CANCELACION', 'RESENA', 'EXPORTACION',
                           'ACCION_ADMIN', 'ERROR'));
