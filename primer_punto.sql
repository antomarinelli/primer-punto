-- ============================================================
-- Primer Punto - Sistema de gestion de torneos de tenis
-- Base de datos: primer_punto
-- Autora: Antonella Marinelli
-- ============================================================

CREATE DATABASE IF NOT EXISTS primer_punto;
USE primer_punto;

-- ============================================================
-- 1. CREACION DE TABLAS
-- ============================================================

CREATE TABLE participante (
    id_participante INT PRIMARY KEY AUTO_INCREMENT,
    nombre_representativo VARCHAR(100)
);

CREATE TABLE jugador (
    id_jugador INT PRIMARY KEY AUTO_INCREMENT,
    id_participante INT,
    nombre VARCHAR(50),
    apellido VARCHAR(50),
    dni VARCHAR(15),
    contacto VARCHAR(50),
    FOREIGN KEY (id_participante) REFERENCES participante(id_participante)
);

CREATE TABLE pareja (
    id_pareja INT PRIMARY KEY AUTO_INCREMENT,
    id_participante INT,
    id_jugador1 INT,
    id_jugador2 INT,
    FOREIGN KEY (id_participante) REFERENCES participante(id_participante),
    FOREIGN KEY (id_jugador1) REFERENCES jugador(id_jugador),
    FOREIGN KEY (id_jugador2) REFERENCES jugador(id_jugador)
);

CREATE TABLE torneo (
    id_torneo INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100),
    modalidad VARCHAR(20),
    costo DECIMAL(10,2),
    fecha_inicio DATE,
    fecha_fin DATE,
    plazo_inscripcion DATE,
    estado VARCHAR(20),
    lugar VARCHAR(100)
);

CREATE TABLE cancha (
    id_cancha INT PRIMARY KEY AUTO_INCREMENT,
    numero INT,
    disponibilidad BOOLEAN,
    hora_apertura TIME,
    hora_cierre TIME
);

CREATE TABLE inscripcion (
    id_inscripcion INT PRIMARY KEY AUTO_INCREMENT,
    id_torneo INT,
    id_participante INT,
    fecha DATE,
    estado_pago BOOLEAN,
    estado_inscripcion VARCHAR(20),
    motivo_baja VARCHAR(200),
    FOREIGN KEY (id_torneo) REFERENCES torneo(id_torneo),
    FOREIGN KEY (id_participante) REFERENCES participante(id_participante)
);

CREATE TABLE formato (
    id_formato INT PRIMARY KEY AUTO_INCREMENT,
    id_torneo INT,
    FOREIGN KEY (id_torneo) REFERENCES torneo(id_torneo)
);

CREATE TABLE cuadro (
    id_cuadro INT PRIMARY KEY AUTO_INCREMENT,
    id_formato INT,
    tipo_cuadro VARCHAR(30),
    tamanio INT,
    cantidad_byes INT,
    FOREIGN KEY (id_formato) REFERENCES formato(id_formato)
);

CREATE TABLE zona (
    id_zona INT PRIMARY KEY AUTO_INCREMENT,
    id_formato INT,
    numero_zona INT,
    FOREIGN KEY (id_formato) REFERENCES formato(id_formato)
);

CREATE TABLE partido (
    id_partido INT PRIMARY KEY AUTO_INCREMENT,
    id_formato INT,
    id_participante1 INT,
    id_participante2 INT,
    id_cancha INT,
    fecha DATE,
    horario TIME,
    estado VARCHAR(20),
    FOREIGN KEY (id_formato) REFERENCES formato(id_formato),
    FOREIGN KEY (id_participante1) REFERENCES participante(id_participante),
    FOREIGN KEY (id_participante2) REFERENCES participante(id_participante),
    FOREIGN KEY (id_cancha) REFERENCES cancha(id_cancha)
);

CREATE TABLE resultado (
    id_resultado INT PRIMARY KEY AUTO_INCREMENT,
    id_partido INT,
    marcador VARCHAR(50),
    id_ganador INT,
    es_wo BOOLEAN,
    es_retiro BOOLEAN,
    FOREIGN KEY (id_partido) REFERENCES partido(id_partido),
    FOREIGN KEY (id_ganador) REFERENCES participante(id_participante)
);

-- ============================================================
-- 2. INSERCION DE DATOS DE PRUEBA
-- ============================================================

INSERT INTO participante (id_participante, nombre_representativo) VALUES
(1, 'Antonella Marinelli'),
(2, 'Marcos Diaz'),
(3, 'Lucas Gomez'),
(4, 'Sofia Perez');

INSERT INTO jugador (id_jugador, id_participante, nombre, apellido, dni, contacto) VALUES
(1, 1, 'Antonella', 'Marinelli', '46512132', 'antonella@email.com'),
(2, 2, 'Marcos', 'Diaz', '39876543', 'marcos@email.com'),
(3, 3, 'Lucas', 'Gomez', '38765432', 'lucas@email.com'),
(4, 4, 'Sofia', 'Perez', '41234567', 'sofia@email.com');

INSERT INTO torneo (id_torneo, nombre, modalidad, costo, fecha_inicio, fecha_fin, plazo_inscripcion, estado, lugar) VALUES
(1, 'Torneo Apertura 2026', 'Dobles', 8500.00, '2026-10-05', '2026-10-12', '2026-09-30', 'Inscripcion abierta', 'Club Los Cuervos');

INSERT INTO cancha (id_cancha, numero, disponibilidad, hora_apertura, hora_cierre) VALUES
(1, 1, TRUE, '08:00:00', '22:00:00'),
(2, 2, TRUE, '08:00:00', '22:00:00');

INSERT INTO inscripcion (id_inscripcion, id_torneo, id_participante, fecha, estado_pago, estado_inscripcion) VALUES
(1, 1, 1, '2026-09-20', TRUE, 'Confirmada'),
(2, 1, 3, '2026-09-19', TRUE, 'Confirmada'),
(3, 1, 4, '2026-09-23', FALSE, 'Pendiente');

-- ============================================================
-- 3. CONSULTA DE DATOS DE PRUEBA
-- ============================================================

-- 3.1 Listado de inscriptos con estado de pago
SELECT
    J.nombre AS 'Nombre',
    J.apellido AS 'Apellido',
    T.nombre AS 'Torneo',
    I.fecha AS 'Fecha de inscripcion',
    I.estado_pago AS 'Pago realizado',
    I.estado_inscripcion AS 'Estado'
FROM
    inscripcion I,
    participante P,
    jugador J,
    torneo T
WHERE
    I.id_participante = P.id_participante
    AND P.id_participante = J.id_participante
    AND I.id_torneo = T.id_torneo;

-- 3.2 Inscriptos con pago pendiente
SELECT
    J.nombre AS 'Nombre',
    J.apellido AS 'Apellido',
    T.nombre AS 'Torneo',
    I.fecha AS 'Fecha de inscripcion'
FROM
    inscripcion I,
    participante P,
    jugador J,
    torneo T
WHERE
    I.id_participante = P.id_participante
    AND P.id_participante = J.id_participante
    AND I.id_torneo = T.id_torneo
    AND I.estado_pago = FALSE;

-- 3.3 Cantidad de inscriptos por torneo
SELECT
    T.nombre AS 'Torneo',
    COUNT(I.id_inscripcion) AS 'Cantidad de inscriptos'
FROM
    torneo T
LEFT JOIN
    inscripcion I ON T.id_torneo = I.id_torneo
GROUP BY
    T.nombre;

-- ============================================================
-- 4. BORRADO DE DATOS DE PRUEBA Y VERIFICACION
-- ============================================================

DELETE FROM inscripcion;
DELETE FROM jugador;
DELETE FROM participante;
DELETE FROM torneo;
DELETE FROM cancha;

SELECT * FROM inscripcion;
SELECT * FROM jugador;
SELECT * FROM participante;
SELECT * FROM torneo;
SELECT * FROM cancha;
