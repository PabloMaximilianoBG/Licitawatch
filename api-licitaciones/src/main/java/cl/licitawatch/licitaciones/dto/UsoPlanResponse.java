package cl.licitawatch.licitaciones.dto;

/** Uso del mes vs límites del plan vigente (null = ilimitado). */
public record UsoPlanResponse(
        String plan,
        long publicacionesMes,
        Integer limitePublicacionesMes,
        long postulacionesMes,
        Integer limitePostulacionesMes,
        long busquedasMes,
        Integer limiteBusquedasMes
) {
}
