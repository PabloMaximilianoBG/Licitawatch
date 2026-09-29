-- Tabla de SOPORTE 1:1 con plan_suscripcion: precio, límites y beneficios (PPT diap. 7).
-- Límite NULL = ilimitado.
CREATE TABLE plan_detalle (
    plan_id                  BIGINT         PRIMARY KEY REFERENCES plan_suscripcion (id),
    precio                   NUMERIC(12, 2) NOT NULL,
    descripcion              TEXT           NOT NULL,
    limite_publicaciones_mes INTEGER,
    limite_postulaciones_mes INTEGER,
    limite_busquedas_mes     INTEGER,
    acceso_licitasist        BOOLEAN        NOT NULL,
    prioridad_visibilidad    BOOLEAN        NOT NULL,
    soporte                  VARCHAR(30)    NOT NULL,
    duracion_dias            INTEGER,
    beneficios               TEXT           NOT NULL
);

INSERT INTO plan_detalle (plan_id, precio, descripcion, limite_publicaciones_mes, limite_postulaciones_mes,
                          limite_busquedas_mes, acceso_licitasist, prioridad_visibilidad, soporte, duracion_dias, beneficios)
SELECT id, 0, 'Acceso funcional para comenzar a publicar o postular.', 3, 5, 50, FALSE, FALSE, 'ESTANDAR', NULL,
       'Publicación / búsqueda con límite mensual|Postulaciones limitadas por mes|Soporte estándar'
FROM plan_suscripcion WHERE nombre = 'ESTANDAR';

INSERT INTO plan_detalle (plan_id, precio, descripcion, limite_publicaciones_mes, limite_postulaciones_mes,
                          limite_busquedas_mes, acceso_licitasist, prioridad_visibilidad, soporte, duracion_dias, beneficios)
SELECT id, 24990, 'Acceso ampliado para mayor volumen de operación.', NULL, NULL, NULL, TRUE, TRUE, 'PRIORITARIO', 30,
       'Publicaciones y postulaciones ilimitadas|Búsquedas ilimitadas|Prioridad de visibilidad en resultados|Soporte prioritario|Acceso a LicitAsist (asistente con IA)'
FROM plan_suscripcion WHERE nombre = 'PREMIUM';
