-- ============================================================
-- LicitaWatch — creación de las 4 bases de datos (una por microservicio)
-- Ejecutar conectado como superusuario (postgres):
--   psql -U postgres -h localhost -f db/crear-bases.sql
-- Las TABLAS no se crean aquí: las crea Flyway al arrancar cada API.
-- API Asistente NO tiene base de datos (solo orquesta a las otras APIs).
-- ============================================================
SELECT 'CREATE DATABASE licitawatch_usuarios ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'licitawatch_usuarios')\gexec

SELECT 'CREATE DATABASE licitawatch_licitaciones ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'licitawatch_licitaciones')\gexec

SELECT 'CREATE DATABASE licitawatch_ventas ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'licitawatch_ventas')\gexec

SELECT 'CREATE DATABASE licitawatch_notificaciones ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'licitawatch_notificaciones')\gexec
