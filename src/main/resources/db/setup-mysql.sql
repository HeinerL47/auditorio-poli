-- ============================================================
--  Script para crear la base de datos en MySQL
--  Ejecutar UNA SOLA VEZ antes de arrancar la aplicacion
-- ============================================================

CREATE DATABASE IF NOT EXISTS auditorio_poli
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- (Opcional) usuario dedicado distinto a root
-- CREATE USER 'auditorio'@'localhost' IDENTIFIED BY 'auditorio123';
-- GRANT ALL PRIVILEGES ON auditorio_poli.* TO 'auditorio'@'localhost';
-- FLUSH PRIVILEGES;

-- Las tablas (usuarios, reservas, bloqueos, tarifas) las crea
-- Hibernate automaticamente al iniciar la aplicacion gracias a
-- spring.jpa.hibernate.ddl-auto=update
--
-- Los usuarios y datos iniciales los inserta DataSeeder.java al
-- detectar que la tabla esta vacia. Despues, los nuevos usuarios
-- solo se crean a traves del registro publico (/registro).

USE auditorio_poli;

-- Verificar despues de la primera ejecucion:
-- SELECT id, nombre, correo, rol, tipo_solicitante FROM usuarios;
-- SELECT * FROM reservas;
-- SELECT * FROM bloqueos;
-- SELECT * FROM tarifas;
