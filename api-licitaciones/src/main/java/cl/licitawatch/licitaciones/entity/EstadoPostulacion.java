package cl.licitawatch.licitaciones.entity;

/** ENUM PostgreSQL postulacion_estado. */
public enum EstadoPostulacion {
    ENVIADA, EN_REVISION, SELECCIONADA, RECHAZADA, RETIRADA;

    public boolean activa() {
        return this == ENVIADA || this == EN_REVISION;
    }
}
