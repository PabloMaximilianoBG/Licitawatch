package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.entity.Licitacion;
import cl.licitawatch.licitaciones.entity.Postulacion;
import cl.licitawatch.licitaciones.exception.ApiException;
import cl.licitawatch.licitaciones.security.AuthUser;

/** Validación de ownership: el backend decide, no el frontend. */
public final class Permisos {
    private Permisos() {
    }

    public static boolean esDuenoLicitacion(AuthUser u, Licitacion l) {
        return "LICITADOR".equals(u.rol()) && l.getLicitadorId().equals(u.perfilId());
    }

    public static boolean esDuenoPostulacion(AuthUser u, Postulacion p) {
        return "PYME".equals(u.rol()) && p.getPymeId().equals(u.perfilId());
    }

    public static void exigirDuenoLicitacion(AuthUser u, Licitacion l) {
        if (!esDuenoLicitacion(u, l)) {
            throw ApiException.forbidden("NO_ES_DUENO", "Solo el licitador que creó esta licitación puede modificarla");
        }
    }

    public static void exigirDuenoLicitacionOAdmin(AuthUser u, Licitacion l) {
        if (!u.esAdmin() && !esDuenoLicitacion(u, l)) {
            throw ApiException.forbidden("NO_ES_DUENO", "No tienes acceso a esta licitación");
        }
    }

    public static void exigirDuenoPostulacion(AuthUser u, Postulacion p) {
        if (!esDuenoPostulacion(u, p)) {
            throw ApiException.forbidden("NO_ES_DUENO", "Esta postulación no te pertenece");
        }
    }
}
