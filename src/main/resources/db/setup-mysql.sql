-- Esquema actualizado para Auditorio Poli
-- Secciones: B1, B2, B3 (Completo = auditorio completo)
-- B3 ocupa todo el auditorio: no puede coincidir con B1 ni B2
-- Roles: ADMIN_AUDITORIO, OPERATIVO (con sub-rol tipoOperativo), SOLICITANTE (con tipoSolicitante)
-- tipoOperativo: ASISTENTE, TECNOLOGIA, AUDIOVISUAL, INFRAESTRUCTURA, OPERACIONES
-- tipoSolicitante: DOCENTE, ADMINISTRATIVO, EXTERNO

CREATE DATABASE IF NOT EXISTS auditorio_poli CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auditorio_poli;

CREATE TABLE IF NOT EXISTS usuarios (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre        VARCHAR(200) NOT NULL,
  documento     VARCHAR(50)  NOT NULL UNIQUE,
  correo        VARCHAR(200) NOT NULL UNIQUE,
  telefono      VARCHAR(50),
  organizacion  VARCHAR(200),
  password      VARCHAR(255) NOT NULL,
  rol           VARCHAR(30)  NOT NULL,
  tipo_solicitante VARCHAR(30),
  tipo_operativo   VARCHAR(30),
  activo        TINYINT(1)   NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tarifas (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  seccion    VARCHAR(30) NOT NULL UNIQUE,
  valor_hora DECIMAL(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS bloqueos (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  motivo      VARCHAR(300) NOT NULL,
  seccion     VARCHAR(30),
  dia_semana  VARCHAR(20),
  hora_inicio TIME,
  hora_fin    TIME,
  inicio      DATETIME,
  fin         DATETIME,
  recurrente  TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reservas (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  solicitante_id    BIGINT NOT NULL,
  inicio            DATETIME NOT NULL,
  fin               DATETIME NOT NULL,
  seccion           VARCHAR(30) NOT NULL,
  tipo_evento       VARCHAR(300) NOT NULL,
  especificaciones  TEXT,
  estado            VARCHAR(30) NOT NULL,
  costo             DECIMAL(12,2),
  observaciones     TEXT,
  motivo_rechazo    TEXT,
  motivo_cancelacion TEXT,
  creada_en         DATETIME,
  decidida_en       DATETIME,
  decidida_por      VARCHAR(200),
  recordatorio_enviado TINYINT(1) DEFAULT 0,
  FOREIGN KEY (solicitante_id) REFERENCES usuarios(id)
);

-- MIGRACION: Si ya existe una BD con registros 'COMPLETO', ejecutar:
-- UPDATE reservas SET seccion = 'B3' WHERE seccion = 'COMPLETO';
-- UPDATE bloqueos SET seccion = 'B3' WHERE seccion = 'COMPLETO';
-- UPDATE tarifas SET seccion = 'B3' WHERE seccion = 'COMPLETO';
-- DELETE FROM tarifas WHERE seccion = 'COMPLETO';
