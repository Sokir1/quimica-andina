-- Ejecutar una sola vez antes de usar la versión nueva con una base histórica.
-- Conserva todos los datos; luego el login migra cada SHA-256 a PBKDF2.
USE sistema_asistencia;
ALTER TABLE empleado MODIFY clave VARCHAR(255) NOT NULL;
