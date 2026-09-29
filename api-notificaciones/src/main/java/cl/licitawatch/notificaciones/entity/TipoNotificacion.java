package cl.licitawatch.notificaciones.entity;

/** ENUM PostgreSQL notificacion_tipo: eventos que disparan un correo (PPT diap. 15 + eventos necesarios). */
public enum TipoNotificacion {
    CONFIRMACION_REGISTRO,
    LICITACION_PUBLICADA,
    POSTULACION_RECIBIDA,
    POSTULACION_ENVIADA,
    RESULTADO_POSTULACION,
    LICITACION_ADJUDICADA,
    PAGO_CONFIRMADO,
    PAGO_RECHAZADO,
    GENERAL
}
