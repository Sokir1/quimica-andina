-- Datos ficticios exclusivos de demostración. Clave inicial: demo12345.
-- =====================================================================
-- SISTEMA DE CONTROL DE ASISTENCIA - Base de datos MariaDB
-- Caso: empresa de compra/venta de productos quimicos (25 trabajadores)
-- Integracion de Competencias II - Avance 2
-- Motor objetivo: MariaDB 10.4+ (XAMPP). Cliente sugerido: DBeaver.
-- Claves almacenadas como SHA-256 (hex). En produccion usar bcrypt/argon2.
--
-- Normalizacion (3FN):
--   * area y turno separan catalogos repetitivos (sin campos duplicados).
--   * empleado referencia area/turno por FK (relacion N:1).
--   * registro_asistencia guarda UNA fila por empleado y dia
--     (UNIQUE id_empleado+fecha): impide doble entrada a nivel de BD.
--   * Los estados de entrada/salida se fijan al registrar usando la regla
--     del caso (>09:30 atraso, <17:30 anticipada); las vistas comparan
--     ademas contra el turno propio de cada trabajador (parametrizacion).
-- =====================================================================

-- No elimina bases existentes. Importar en una base nueva para demostración.
CREATE DATABASE sistema_asistencia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sistema_asistencia;

-- ------------------------- TABLA AREA --------------------------------
CREATE TABLE area (
  id_area     INT AUTO_INCREMENT PRIMARY KEY,
  nombre_area VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ------------------------- TABLA TURNO -------------------------------
-- Parametrizacion de horarios: cada empleado tiene su propio turno.
CREATE TABLE turno (
  id_turno     INT AUTO_INCREMENT PRIMARY KEY,
  nombre_turno VARCHAR(40) NOT NULL UNIQUE,
  hora_entrada TIME NOT NULL,
  hora_salida  TIME NOT NULL,
  CHECK (hora_salida > hora_entrada)
) ENGINE=InnoDB;

-- ------------------------ TABLA EMPLEADO -----------------------------
-- 'activo' permite eliminacion logica (GU-03) preservando el historial
-- de asistencia: nunca se borra fisicamente un empleado con registros.
CREATE TABLE empleado (
  id_empleado        INT AUTO_INCREMENT PRIMARY KEY,
  run                VARCHAR(12)  NOT NULL UNIQUE COMMENT 'RUN chileno con digito verificador',
  nombres            VARCHAR(60)  NOT NULL,
  apellidos          VARCHAR(60)  NOT NULL,
  correo             VARCHAR(100) NOT NULL UNIQUE COMMENT 'correo de login',
  clave              VARCHAR(255)     NOT NULL COMMENT 'PBKDF2; admite SHA-256 histórico para migración',
  telefono           VARCHAR(20)  NULL,
  cargo              VARCHAR(50)  NOT NULL,
  fecha_contratacion DATE         NOT NULL,
  id_turno           INT          NOT NULL,
  id_area            INT          NOT NULL,
  rol                ENUM('ADMINISTRADOR','EMPLEADO') NOT NULL DEFAULT 'EMPLEADO',
  activo             TINYINT(1)   NOT NULL DEFAULT 1,
  CONSTRAINT fk_emp_turno FOREIGN KEY (id_turno) REFERENCES turno (id_turno),
  CONSTRAINT fk_emp_area  FOREIGN KEY (id_area)  REFERENCES area  (id_area)
) ENGINE=InnoDB;

-- ------------------- TABLA REGISTRO_ASISTENCIA -----------------------
-- Transaccional: una fila por empleado y dia. hora_salida NULL = aun
-- no marca su salida (o la olvido). UNIQUE impide doble marca diaria.
CREATE TABLE registro_asistencia (
  id_registro    INT AUTO_INCREMENT PRIMARY KEY,
  id_empleado    INT         NOT NULL,
  fecha          DATE        NOT NULL,
  hora_entrada   TIME        NOT NULL,
  hora_salida    TIME        NULL,
  estado_entrada ENUM('PUNTUAL','ATRASADA')   NOT NULL,
  estado_salida  ENUM('NORMAL','ANTICIPADA')  NULL,
  UNIQUE KEY uk_emp_fecha (id_empleado, fecha),
  CONSTRAINT fk_reg_emp FOREIGN KEY (id_empleado)
    REFERENCES empleado (id_empleado) ON DELETE RESTRICT ON UPDATE CASCADE,
  CHECK (hora_salida IS NULL OR hora_salida > hora_entrada)
) ENGINE=InnoDB;

-- ======================= DATOS SEMILLA ===============================
INSERT INTO area (id_area, nombre_area) VALUES
  (1, 'Ventas'),
  (2, 'Bodega'),
  (3, 'Administracion');

INSERT INTO turno (id_turno, nombre_turno, hora_entrada, hora_salida) VALUES
  (1, 'Manana Temprano', '07:00:00', '16:00:00'),
  (2, 'Manana Estandar', '08:30:00', '17:30:00'),
  (3, 'Medio Dia', '09:00:00', '18:00:00'),
  (4, 'Tarde', '12:00:00', '20:00:00');

INSERT INTO empleado (run, nombres, apellidos, correo, clave, telefono, cargo, fecha_contratacion, id_turno, id_area, rol, activo) VALUES
  ('DEMO-001', 'Usuario', 'Demo 01', 'admin@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Administrador del Sistema', '2019-04-22', 2, 3, 'ADMINISTRADOR', 1),
  ('DEMO-002', 'Usuario', 'Demo 02', 'empleado01@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Vendedor', '2020-06-27', 1, 1, 'EMPLEADO', 1),
  ('DEMO-003', 'Usuario', 'Demo 03', 'empleado02@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Vendedor', '2021-01-08', 2, 1, 'EMPLEADO', 1),
  ('DEMO-004', 'Usuario', 'Demo 04', 'empleado03@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Supervisor de Ventas', '2022-01-26', 2, 1, 'EMPLEADO', 1),
  ('DEMO-005', 'Usuario', 'Demo 05', 'empleado04@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Vendedor', '2023-06-13', 3, 1, 'EMPLEADO', 1),
  ('DEMO-006', 'Usuario', 'Demo 06', 'empleado05@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Vendedor', '2024-05-03', 1, 1, 'EMPLEADO', 1),
  ('DEMO-007', 'Usuario', 'Demo 07', 'empleado06@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Supervisor de Ventas', '2019-04-19', 2, 1, 'EMPLEADO', 1),
  ('DEMO-008', 'Usuario', 'Demo 08', 'empleado07@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Supervisor de Ventas', '2020-12-11', 2, 1, 'EMPLEADO', 1),
  ('DEMO-009', 'Usuario', 'Demo 09', 'empleado08@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Vendedora', '2021-04-21', 3, 1, 'EMPLEADO', 1),
  ('DEMO-010', 'Usuario', 'Demo 10', 'empleado09@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Vendedor', '2022-08-13', 1, 1, 'EMPLEADO', 1),
  ('DEMO-011', 'Usuario', 'Demo 11', 'empleado10@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Operario de Bodega', '2023-11-15', 1, 2, 'EMPLEADO', 1),
  ('DEMO-012', 'Usuario', 'Demo 12', 'empleado11@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Operario de Bodega', '2024-03-09', 2, 2, 'EMPLEADO', 1),
  ('DEMO-013', 'Usuario', 'Demo 13', 'empleado12@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Operaria de Bodega', '2019-03-08', 2, 2, 'EMPLEADO', 1),
  ('DEMO-014', 'Usuario', 'Demo 14', 'empleado13@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Operario de Bodega', '2020-12-18', 3, 2, 'EMPLEADO', 1),
  ('DEMO-015', 'Usuario', 'Demo 15', 'empleado14@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Operaria de Bodega', '2021-09-09', 1, 2, 'EMPLEADO', 1),
  ('DEMO-016', 'Usuario', 'Demo 16', 'empleado15@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Jefe de Bodega', '2022-12-19', 2, 2, 'EMPLEADO', 1),
  ('DEMO-017', 'Usuario', 'Demo 17', 'empleado16@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Jefe de Bodega', '2023-07-19', 2, 2, 'EMPLEADO', 1),
  ('DEMO-018', 'Usuario', 'Demo 18', 'empleado17@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Operario de Bodega', '2024-07-12', 3, 2, 'EMPLEADO', 1),
  ('DEMO-019', 'Usuario', 'Demo 19', 'empleado18@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Analista Administrativo', '2019-04-05', 1, 3, 'EMPLEADO', 1),
  ('DEMO-020', 'Usuario', 'Demo 20', 'empleado19@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Secretaria Administrativa', '2020-09-16', 2, 3, 'EMPLEADO', 1),
  ('DEMO-021', 'Usuario', 'Demo 21', 'empleado20@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Asistente de Contabilidad', '2021-02-25', 2, 3, 'EMPLEADO', 1),
  ('DEMO-022', 'Usuario', 'Demo 22', 'empleado21@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Secretaria Administrativa', '2022-01-28', 3, 3, 'EMPLEADO', 1),
  ('DEMO-023', 'Usuario', 'Demo 23', 'empleado22@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Analista Administrativo', '2023-02-05', 1, 3, 'EMPLEADO', 1),
  ('DEMO-024', 'Usuario', 'Demo 24', 'empleado23@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Analista Administrativo', '2024-11-06', 4, 3, 'EMPLEADO', 1),
  ('DEMO-025', 'Usuario', 'Demo 25', 'empleado24@example.com', '51459c23ca91ebce271449dd8b5c26751c99039c2ae4c628067898ca0e104039', NULL, 'Secretaria Administrativa', '2019-11-14', 4, 3, 'EMPLEADO', 1);

INSERT INTO registro_asistencia (id_empleado, fecha, hora_entrada, hora_salida, estado_entrada, estado_salida) VALUES
  (1, '2026-08-06', '08:34:00', '16:50:00', 'PUNTUAL', 'ANTICIPADA'),
  (1, '2026-08-10', '08:28:00', '17:29:00', 'PUNTUAL', 'ANTICIPADA'),
  (1, '2026-08-11', '08:28:00', '16:27:00', 'PUNTUAL', 'ANTICIPADA'),
  (1, '2026-08-12', '08:45:00', '17:26:00', 'PUNTUAL', 'ANTICIPADA'),
  (1, '2026-08-13', '08:53:00', '16:53:00', 'PUNTUAL', 'ANTICIPADA'),
  (1, '2026-08-14', '08:20:00', '17:45:00', 'PUNTUAL', 'NORMAL'),
  (1, '2026-08-17', '08:22:00', '17:22:00', 'PUNTUAL', 'ANTICIPADA'),
  (1, '2026-08-19', '08:35:00', '17:36:00', 'PUNTUAL', 'NORMAL'),
  (1, '2026-08-20', '08:26:00', '16:39:00', 'PUNTUAL', 'ANTICIPADA'),
  (1, '2026-08-21', '08:31:00', '17:23:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-06', '06:50:00', '15:57:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-07', '07:07:00', '15:20:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-10', '06:58:00', '15:54:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-11', '07:24:00', '15:16:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-12', '07:03:00', '16:13:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-13', '06:53:00', '16:15:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-14', '06:56:00', '15:55:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-17', '06:52:00', '15:00:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-18', '06:50:00', '15:19:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-19', '07:05:00', '15:51:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-20', '07:02:00', '14:54:00', 'PUNTUAL', 'ANTICIPADA'),
  (2, '2026-08-21', '07:05:00', '15:51:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-06', '08:30:00', '17:36:00', 'PUNTUAL', 'NORMAL'),
  (3, '2026-08-07', '08:21:00', '17:25:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-10', '08:27:00', '16:31:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-11', '08:22:00', '17:36:00', 'PUNTUAL', 'NORMAL'),
  (3, '2026-08-12', '08:30:00', '17:41:00', 'PUNTUAL', 'NORMAL'),
  (3, '2026-08-13', '08:22:00', NULL, 'PUNTUAL', NULL),
  (3, '2026-08-14', '08:23:00', '17:28:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-17', '08:22:00', '17:25:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-18', '08:29:00', '16:36:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-19', '08:29:00', '17:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-20', '08:53:00', '17:26:00', 'PUNTUAL', 'ANTICIPADA'),
  (3, '2026-08-21', '08:28:00', '17:21:00', 'PUNTUAL', 'ANTICIPADA'),
  (4, '2026-08-06', '08:28:00', '17:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (4, '2026-08-07', '08:34:00', '17:20:00', 'PUNTUAL', 'ANTICIPADA'),
  (4, '2026-08-10', '09:44:00', '17:31:00', 'ATRASADA', 'NORMAL'),
  (4, '2026-08-11', '08:24:00', '17:45:00', 'PUNTUAL', 'NORMAL'),
  (4, '2026-08-13', '08:27:00', '17:37:00', 'PUNTUAL', 'NORMAL'),
  (4, '2026-08-14', '08:24:00', '17:25:00', 'PUNTUAL', 'ANTICIPADA'),
  (4, '2026-08-18', '08:36:00', '16:45:00', 'PUNTUAL', 'ANTICIPADA'),
  (4, '2026-08-19', '08:43:00', '16:01:00', 'PUNTUAL', 'ANTICIPADA'),
  (4, '2026-08-20', '08:35:00', '16:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (4, '2026-08-21', '08:43:00', '17:32:00', 'PUNTUAL', 'NORMAL'),
  (5, '2026-08-07', '09:14:00', '18:11:00', 'PUNTUAL', 'NORMAL'),
  (5, '2026-08-12', '09:37:00', '18:08:00', 'ATRASADA', 'NORMAL'),
  (5, '2026-08-17', '09:03:00', '16:52:00', 'PUNTUAL', 'ANTICIPADA'),
  (5, '2026-08-18', '09:02:00', '17:51:00', 'PUNTUAL', 'NORMAL'),
  (5, '2026-08-20', '08:56:00', '18:11:00', 'PUNTUAL', 'NORMAL'),
  (5, '2026-08-21', '08:53:00', '17:59:00', 'PUNTUAL', 'NORMAL'),
  (6, '2026-08-06', '06:59:00', '16:11:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-10', '08:09:00', '15:50:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-11', '07:00:00', '15:56:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-12', '07:11:00', '16:11:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-13', '06:57:00', '15:56:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-14', '07:05:00', '14:54:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-17', '06:56:00', '16:02:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-18', '06:50:00', '15:21:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-19', '06:55:00', '16:04:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-20', '06:53:00', '15:07:00', 'PUNTUAL', 'ANTICIPADA'),
  (6, '2026-08-21', '07:00:00', '15:04:00', 'PUNTUAL', 'ANTICIPADA'),
  (7, '2026-08-06', '08:49:00', '17:25:00', 'PUNTUAL', 'ANTICIPADA'),
  (7, '2026-08-07', '08:28:00', '16:12:00', 'PUNTUAL', 'ANTICIPADA'),
  (7, '2026-08-10', '08:27:00', '17:29:00', 'PUNTUAL', 'ANTICIPADA'),
  (7, '2026-08-11', '08:22:00', '17:42:00', 'PUNTUAL', 'NORMAL'),
  (7, '2026-08-12', '08:33:00', '17:33:00', 'PUNTUAL', 'NORMAL'),
  (7, '2026-08-13', '08:48:00', '17:42:00', 'PUNTUAL', 'NORMAL'),
  (7, '2026-08-14', '08:54:00', '17:31:00', 'PUNTUAL', 'NORMAL'),
  (7, '2026-08-17', '08:33:00', '17:45:00', 'PUNTUAL', 'NORMAL'),
  (7, '2026-08-18', '08:27:00', '17:32:00', 'PUNTUAL', 'NORMAL'),
  (7, '2026-08-19', '08:32:00', '16:53:00', 'PUNTUAL', 'ANTICIPADA'),
  (7, '2026-08-20', '08:20:00', '17:41:00', 'PUNTUAL', 'NORMAL'),
  (7, '2026-08-21', '08:24:00', '17:28:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-06', '08:30:00', '16:12:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-07', '08:44:00', '17:43:00', 'PUNTUAL', 'NORMAL'),
  (8, '2026-08-10', '09:22:00', '17:40:00', 'PUNTUAL', 'NORMAL'),
  (8, '2026-08-11', '08:27:00', '17:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-12', '08:23:00', '17:42:00', 'PUNTUAL', 'NORMAL'),
  (8, '2026-08-13', '08:23:00', '17:29:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-14', '08:29:00', '16:19:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-17', '08:38:00', '16:10:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-18', '08:55:00', '16:31:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-19', '08:33:00', '17:40:00', 'PUNTUAL', 'NORMAL'),
  (8, '2026-08-20', '08:51:00', '16:35:00', 'PUNTUAL', 'ANTICIPADA'),
  (8, '2026-08-21', '08:24:00', '17:40:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-06', '09:23:00', '18:13:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-07', '08:57:00', '18:04:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-10', '09:02:00', '18:00:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-11', '08:58:00', '17:09:00', 'PUNTUAL', 'ANTICIPADA'),
  (9, '2026-08-12', '08:56:00', '18:05:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-13', '09:05:00', '18:15:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-14', '09:02:00', '18:08:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-17', '09:01:00', '18:00:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-18', '08:59:00', '17:56:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-19', '09:11:00', '17:58:00', 'PUNTUAL', 'NORMAL'),
  (9, '2026-08-20', '10:03:00', '17:18:00', 'ATRASADA', 'ANTICIPADA'),
  (9, '2026-08-21', '08:55:00', '17:54:00', 'PUNTUAL', 'NORMAL'),
  (10, '2026-08-06', '08:05:00', '15:05:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-07', '06:50:00', '16:04:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-10', '07:46:00', '15:52:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-11', '06:51:00', '15:25:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-12', '07:37:00', '16:09:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-13', '07:02:00', '16:08:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-17', '06:53:00', '15:56:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-18', '06:57:00', '15:50:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-19', '07:05:00', '16:12:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-20', '07:08:00', '15:15:00', 'PUNTUAL', 'ANTICIPADA'),
  (10, '2026-08-21', '06:56:00', '16:10:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-06', '06:54:00', '15:59:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-07', '07:15:00', '15:59:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-10', '08:02:00', '16:09:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-11', '07:00:00', '15:57:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-13', '06:50:00', '15:58:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-14', '07:11:00', '15:58:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-17', '07:05:00', '16:03:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-18', '06:55:00', '15:59:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-19', '06:51:00', '16:00:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-20', '07:22:00', '16:07:00', 'PUNTUAL', 'ANTICIPADA'),
  (11, '2026-08-21', '07:01:00', '16:04:00', 'PUNTUAL', 'ANTICIPADA'),
  (12, '2026-08-06', '08:24:00', '17:42:00', 'PUNTUAL', 'NORMAL'),
  (12, '2026-08-07', '08:27:00', '17:20:00', 'PUNTUAL', 'ANTICIPADA'),
  (12, '2026-08-10', '08:23:00', '17:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (12, '2026-08-11', '08:52:00', '17:35:00', 'PUNTUAL', 'NORMAL'),
  (12, '2026-08-13', '08:32:00', '17:43:00', 'PUNTUAL', 'NORMAL'),
  (12, '2026-08-14', '08:44:00', '16:16:00', 'PUNTUAL', 'ANTICIPADA'),
  (12, '2026-08-17', '08:20:00', '17:38:00', 'PUNTUAL', 'NORMAL'),
  (12, '2026-08-18', '08:24:00', '17:30:00', 'PUNTUAL', 'NORMAL'),
  (12, '2026-08-19', '08:34:00', '16:39:00', 'PUNTUAL', 'ANTICIPADA'),
  (12, '2026-08-20', '08:33:00', '17:42:00', 'PUNTUAL', 'NORMAL'),
  (12, '2026-08-21', '08:24:00', '17:36:00', 'PUNTUAL', 'NORMAL'),
  (13, '2026-08-07', '08:34:00', '15:55:00', 'PUNTUAL', 'ANTICIPADA'),
  (13, '2026-08-10', '08:24:00', '16:11:00', 'PUNTUAL', 'ANTICIPADA'),
  (13, '2026-08-11', '08:22:00', '16:49:00', 'PUNTUAL', 'ANTICIPADA'),
  (13, '2026-08-12', '09:40:00', '16:50:00', 'ATRASADA', 'ANTICIPADA'),
  (13, '2026-08-13', '08:27:00', '16:09:00', 'PUNTUAL', 'ANTICIPADA'),
  (13, '2026-08-14', '08:23:00', '17:23:00', 'PUNTUAL', 'ANTICIPADA'),
  (13, '2026-08-17', '08:20:00', '16:13:00', 'PUNTUAL', 'ANTICIPADA'),
  (13, '2026-08-18', '08:27:00', '17:45:00', 'PUNTUAL', 'NORMAL'),
  (13, '2026-08-19', '08:32:00', '17:38:00', 'PUNTUAL', 'NORMAL'),
  (13, '2026-08-20', '08:28:00', '17:45:00', 'PUNTUAL', 'NORMAL'),
  (13, '2026-08-21', '08:25:00', '16:32:00', 'PUNTUAL', 'ANTICIPADA'),
  (14, '2026-08-07', '10:14:00', '16:37:00', 'ATRASADA', 'ANTICIPADA'),
  (14, '2026-08-12', '09:02:00', '18:12:00', 'PUNTUAL', 'NORMAL'),
  (14, '2026-08-13', '10:12:00', '18:11:00', 'ATRASADA', 'NORMAL'),
  (14, '2026-08-17', '10:08:00', '17:22:00', 'ATRASADA', 'ANTICIPADA'),
  (14, '2026-08-18', '09:25:00', '17:56:00', 'PUNTUAL', 'NORMAL'),
  (14, '2026-08-20', '08:54:00', '17:06:00', 'PUNTUAL', 'ANTICIPADA'),
  (14, '2026-08-21', '09:07:00', '17:52:00', 'PUNTUAL', 'NORMAL'),
  (15, '2026-08-06', '07:03:00', '16:07:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-07', '06:58:00', '16:05:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-11', '07:09:00', '16:14:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-13', '07:18:00', '14:48:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-14', '06:58:00', '16:11:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-17', '07:02:00', '14:31:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-18', '07:00:00', '15:52:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-19', '06:59:00', '15:51:00', 'PUNTUAL', 'ANTICIPADA'),
  (15, '2026-08-21', '06:53:00', '16:02:00', 'PUNTUAL', 'ANTICIPADA'),
  (16, '2026-08-06', '08:33:00', '17:33:00', 'PUNTUAL', 'NORMAL'),
  (16, '2026-08-07', '08:55:00', '15:57:00', 'PUNTUAL', 'ANTICIPADA'),
  (16, '2026-08-10', '08:34:00', '16:01:00', 'PUNTUAL', 'ANTICIPADA'),
  (16, '2026-08-12', '09:41:00', '17:26:00', 'ATRASADA', 'ANTICIPADA'),
  (16, '2026-08-17', '08:26:00', '17:20:00', 'PUNTUAL', 'ANTICIPADA'),
  (16, '2026-08-18', '08:24:00', '17:44:00', 'PUNTUAL', 'NORMAL'),
  (16, '2026-08-19', '08:27:00', '17:39:00', 'PUNTUAL', 'NORMAL'),
  (16, '2026-08-20', '08:40:00', '17:45:00', 'PUNTUAL', 'NORMAL'),
  (16, '2026-08-21', '08:20:00', '17:45:00', 'PUNTUAL', 'NORMAL'),
  (17, '2026-08-06', '08:25:00', '17:33:00', 'PUNTUAL', 'NORMAL'),
  (17, '2026-08-07', '08:51:00', '17:38:00', 'PUNTUAL', 'NORMAL'),
  (17, '2026-08-10', '08:21:00', '16:37:00', 'PUNTUAL', 'ANTICIPADA'),
  (17, '2026-08-11', '08:20:00', '17:32:00', 'PUNTUAL', 'NORMAL'),
  (17, '2026-08-12', '08:34:00', '17:39:00', 'PUNTUAL', 'NORMAL'),
  (17, '2026-08-13', '08:30:00', '17:29:00', 'PUNTUAL', 'ANTICIPADA'),
  (17, '2026-08-14', '08:23:00', '17:40:00', 'PUNTUAL', 'NORMAL'),
  (17, '2026-08-17', '08:42:00', '16:21:00', 'PUNTUAL', 'ANTICIPADA'),
  (17, '2026-08-18', '08:33:00', '17:30:00', 'PUNTUAL', 'NORMAL'),
  (17, '2026-08-19', '08:24:00', '17:22:00', 'PUNTUAL', 'ANTICIPADA'),
  (17, '2026-08-20', '08:23:00', '17:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (17, '2026-08-21', '08:30:00', '17:41:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-07', '09:33:00', '18:01:00', 'ATRASADA', 'NORMAL'),
  (18, '2026-08-10', '08:54:00', '17:53:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-11', '08:53:00', '18:15:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-12', '08:50:00', '18:12:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-13', '08:59:00', '17:55:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-14', '09:02:00', '17:50:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-17', '09:02:00', '18:01:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-18', '08:52:00', '18:14:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-19', '08:58:00', '18:09:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-20', '08:53:00', '18:11:00', 'PUNTUAL', 'NORMAL'),
  (18, '2026-08-21', '08:59:00', '18:09:00', 'PUNTUAL', 'NORMAL'),
  (19, '2026-08-06', '06:56:00', '14:43:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-07', '07:03:00', '16:12:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-10', '07:01:00', '14:27:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-11', '06:52:00', '16:08:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-12', '06:59:00', '16:00:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-13', '07:59:00', '14:30:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-14', '07:03:00', '16:02:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-17', '07:13:00', '16:06:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-18', '06:56:00', '16:13:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-20', '06:53:00', '16:09:00', 'PUNTUAL', 'ANTICIPADA'),
  (19, '2026-08-21', '07:08:00', '14:56:00', 'PUNTUAL', 'ANTICIPADA'),
  (20, '2026-08-06', '08:34:00', '16:36:00', 'PUNTUAL', 'ANTICIPADA'),
  (20, '2026-08-07', '08:30:00', '17:34:00', 'PUNTUAL', 'NORMAL'),
  (20, '2026-08-10', '08:54:00', '17:29:00', 'PUNTUAL', 'ANTICIPADA'),
  (20, '2026-08-11', '08:31:00', '17:22:00', 'PUNTUAL', 'ANTICIPADA'),
  (20, '2026-08-12', '08:34:00', '16:42:00', 'PUNTUAL', 'ANTICIPADA'),
  (20, '2026-08-13', '08:26:00', '17:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (20, '2026-08-17', '08:46:00', '17:40:00', 'PUNTUAL', 'NORMAL'),
  (20, '2026-08-18', '08:34:00', '17:39:00', 'PUNTUAL', 'NORMAL'),
  (20, '2026-08-19', '08:29:00', '16:38:00', 'PUNTUAL', 'ANTICIPADA'),
  (20, '2026-08-20', '08:46:00', '17:41:00', 'PUNTUAL', 'NORMAL'),
  (20, '2026-08-21', '08:24:00', '17:24:00', 'PUNTUAL', 'ANTICIPADA'),
  (21, '2026-08-06', '08:32:00', '17:22:00', 'PUNTUAL', 'ANTICIPADA'),
  (21, '2026-08-07', '08:30:00', '17:43:00', 'PUNTUAL', 'NORMAL'),
  (21, '2026-08-11', '08:33:00', '17:29:00', 'PUNTUAL', 'ANTICIPADA'),
  (21, '2026-08-12', '08:35:00', '16:04:00', 'PUNTUAL', 'ANTICIPADA'),
  (21, '2026-08-13', '08:39:00', '17:43:00', 'PUNTUAL', 'NORMAL'),
  (21, '2026-08-14', '08:30:00', '17:39:00', 'PUNTUAL', 'NORMAL'),
  (21, '2026-08-17', '08:25:00', '17:31:00', 'PUNTUAL', 'NORMAL'),
  (21, '2026-08-18', '08:34:00', '16:45:00', 'PUNTUAL', 'ANTICIPADA'),
  (21, '2026-08-19', '08:29:00', '16:52:00', 'PUNTUAL', 'ANTICIPADA'),
  (21, '2026-08-20', '08:34:00', '17:32:00', 'PUNTUAL', 'NORMAL'),
  (21, '2026-08-21', '08:25:00', '16:03:00', 'PUNTUAL', 'ANTICIPADA'),
  (22, '2026-08-06', '09:05:00', '17:53:00', 'PUNTUAL', 'NORMAL'),
  (22, '2026-08-07', '08:53:00', '16:42:00', 'PUNTUAL', 'ANTICIPADA'),
  (22, '2026-08-10', '09:03:00', '17:17:00', 'PUNTUAL', 'ANTICIPADA'),
  (22, '2026-08-11', '08:50:00', '18:01:00', 'PUNTUAL', 'NORMAL'),
  (22, '2026-08-12', '09:53:00', '17:53:00', 'ATRASADA', 'NORMAL'),
  (22, '2026-08-13', '08:54:00', '18:05:00', 'PUNTUAL', 'NORMAL'),
  (22, '2026-08-14', '09:05:00', '17:52:00', 'PUNTUAL', 'NORMAL'),
  (22, '2026-08-17', '08:54:00', '18:06:00', 'PUNTUAL', 'NORMAL'),
  (22, '2026-08-18', '09:13:00', '18:03:00', 'PUNTUAL', 'NORMAL'),
  (22, '2026-08-19', '08:52:00', '17:03:00', 'PUNTUAL', 'ANTICIPADA'),
  (22, '2026-08-20', '08:57:00', '17:50:00', 'PUNTUAL', 'NORMAL'),
  (22, '2026-08-21', '09:03:00', '18:06:00', 'PUNTUAL', 'NORMAL'),
  (23, '2026-08-10', '06:50:00', '16:10:00', 'PUNTUAL', 'ANTICIPADA'),
  (23, '2026-08-12', '06:50:00', '15:57:00', 'PUNTUAL', 'ANTICIPADA'),
  (23, '2026-08-13', '07:00:00', '16:00:00', 'PUNTUAL', 'ANTICIPADA'),
  (23, '2026-08-17', '07:04:00', '15:55:00', 'PUNTUAL', 'ANTICIPADA'),
  (23, '2026-08-18', '07:04:00', '16:15:00', 'PUNTUAL', 'ANTICIPADA'),
  (23, '2026-08-20', '06:59:00', '14:59:00', 'PUNTUAL', 'ANTICIPADA'),
  (23, '2026-08-21', '06:56:00', '14:28:00', 'PUNTUAL', 'ANTICIPADA'),
  (24, '2026-08-06', '11:53:00', '20:02:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-07', '12:18:00', '20:12:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-10', '13:03:00', '20:03:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-11', '11:55:00', '19:52:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-12', '12:04:00', '20:12:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-13', '12:01:00', '20:10:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-18', '12:01:00', '19:55:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-20', '12:04:00', '20:14:00', 'ATRASADA', 'NORMAL'),
  (24, '2026-08-21', '11:51:00', '20:01:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-06', '12:24:00', '20:00:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-07', '11:53:00', NULL, 'ATRASADA', NULL),
  (25, '2026-08-10', '11:53:00', '20:04:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-11', '12:05:00', '19:52:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-12', '12:20:00', '19:19:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-14', '12:23:00', '19:19:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-17', '12:11:00', '19:53:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-19', '12:05:00', NULL, 'ATRASADA', NULL),
  (25, '2026-08-20', '11:54:00', '19:59:00', 'ATRASADA', 'NORMAL'),
  (25, '2026-08-21', '12:21:00', '19:17:00', 'ATRASADA', 'NORMAL');

-- ========================= VISTAS (reportes) =========================
-- Reporte de entradas atrasadas (regla fija del caso: despues de 09:30)
CREATE OR REPLACE VIEW vw_reporte_atrasos AS
SELECT e.id_empleado, e.run, CONCAT(e.nombres, ' ', e.apellidos) AS empleado,
       r.fecha, r.hora_entrada, t.nombre_turno, t.hora_entrada AS hora_turno
FROM registro_asistencia r
JOIN empleado e ON e.id_empleado = r.id_empleado
JOIN turno t    ON t.id_turno    = e.id_turno
WHERE r.estado_entrada = 'ATRASADA' AND e.activo = 1;

-- Reporte de salidas anticipadas (regla fija del caso: antes de 17:30)
CREATE OR REPLACE VIEW vw_reporte_salidas_anticipadas AS
SELECT e.id_empleado, e.run, CONCAT(e.nombres, ' ', e.apellidos) AS empleado,
       r.fecha, r.hora_salida, t.nombre_turno, t.hora_salida AS hora_turno
FROM registro_asistencia r
JOIN empleado e ON e.id_empleado = r.id_empleado
JOIN turno t    ON t.id_turno    = e.id_turno
WHERE r.estado_salida = 'ANTICIPADA' AND e.activo = 1;

-- Resumen por trabajador: dias registrados, atrasos, anticipadas, olvidos
CREATE OR REPLACE VIEW vw_resumen_asistencia AS
SELECT e.id_empleado, e.run, CONCAT(e.nombres, ' ', e.apellidos) AS empleado,
       a.nombre_area, t.nombre_turno,
       COUNT(r.id_registro)                        AS dias_registrados,
       SUM(r.estado_entrada = 'ATRASADA')          AS entradas_atrasadas,
       SUM(r.estado_salida  = 'ANTICIPADA')        AS salidas_anticipadas,
       SUM(r.hora_salida IS NULL)                  AS salidas_sin_marcar
FROM empleado e
LEFT JOIN registro_asistencia r ON r.id_empleado = e.id_empleado
LEFT JOIN area  a ON a.id_area  = e.id_area
LEFT JOIN turno t ON t.id_turno = e.id_turno
WHERE e.activo = 1
GROUP BY e.id_empleado, e.run, empleado, a.nombre_area, t.nombre_turno;
