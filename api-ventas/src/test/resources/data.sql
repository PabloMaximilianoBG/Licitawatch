INSERT INTO plan_suscripcion (nombre) VALUES ('ESTANDAR'), ('PREMIUM');
INSERT INTO plan_detalle (plan_id, precio, descripcion, limite_publicaciones_mes, limite_postulaciones_mes, limite_busquedas_mes, acceso_licitasist, prioridad_visibilidad, soporte, duracion_dias, beneficios)
VALUES (1, 0, 'Estandar', 3, 5, 50, FALSE, FALSE, 'ESTANDAR', NULL, 'A|B');
INSERT INTO plan_detalle (plan_id, precio, descripcion, limite_publicaciones_mes, limite_postulaciones_mes, limite_busquedas_mes, acceso_licitasist, prioridad_visibilidad, soporte, duracion_dias, beneficios)
VALUES (2, 24990, 'Premium', NULL, NULL, NULL, TRUE, TRUE, 'PRIORITARIO', 30, 'A|B|C');
