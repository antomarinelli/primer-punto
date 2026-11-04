-- ============================================================
-- Primer Punto - Sistema de gestion de torneos de tenis
-- Base de datos: primer_punto
-- Autora: Antonella Marinelli
--
-- Crea la base vacia. La primera vez que se ejecuta el programa,
-- si la base no tiene datos, se cargan los datos de ejemplo.
-- ============================================================

CREATE DATABASE IF NOT EXISTS primer_punto
    CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
USE primer_punto;

-- Se borran en orden inverso a las dependencias, para poder volver a ejecutar el script
DROP TABLE IF EXISTS resultado;
DROP TABLE IF EXISTS partido;
DROP TABLE IF EXISTS zona;
DROP TABLE IF EXISTS cuadro;
DROP TABLE IF EXISTS formato;
DROP TABLE IF EXISTS inscripcion;
DROP TABLE IF EXISTS cancha;
DROP TABLE IF EXISTS torneo;
DROP TABLE IF EXISTS pareja;
DROP TABLE IF EXISTS jugador;
DROP TABLE IF EXISTS participante;
DROP TABLE IF EXISTS usuario;

-- ============================================================
-- 1. CREACION DE TABLAS
-- ============================================================

-- Jugador y Pareja heredan de Participante (una tabla por subclase)
CREATE TABLE participante (
    id_participante INT PRIMARY KEY AUTO_INCREMENT,
    nombre_representativo VARCHAR(100) NOT NULL
);

CREATE TABLE jugador (
    id_jugador INT PRIMARY KEY AUTO_INCREMENT,
    id_participante INT NOT NULL UNIQUE,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    dni VARCHAR(15) NOT NULL UNIQUE,
    celular VARCHAR(20),
    email VARCHAR(100),
    FOREIGN KEY (id_participante) REFERENCES participante(id_participante)
);

CREATE TABLE pareja (
    id_pareja INT PRIMARY KEY AUTO_INCREMENT,
    id_participante INT NOT NULL UNIQUE,
    id_jugador1 INT NOT NULL,
    id_jugador2 INT NOT NULL,
    FOREIGN KEY (id_participante) REFERENCES participante(id_participante),
    FOREIGN KEY (id_jugador1) REFERENCES jugador(id_jugador),
    FOREIGN KEY (id_jugador2) REFERENCES jugador(id_jugador)
);

-- modalidad, estado y formato_resultado guardan el nombre del valor del enum de Java
-- (por ejemplo 'DOBLES', 'INSCRIPCION_ABIERTA' o 'DOS_SETS_Y_SUPER_TIEBREAK')
CREATE TABLE torneo (
    id_torneo INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    modalidad VARCHAR(20) NOT NULL,
    costo DECIMAL(10,2) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    plazo_inscripcion DATE,
    estado VARCHAR(25) NOT NULL,
    lugar VARCHAR(100),
    duracion_partido INT NOT NULL,
    formato_resultado VARCHAR(30) NOT NULL
);

CREATE TABLE cancha (
    id_cancha INT PRIMARY KEY AUTO_INCREMENT,
    numero INT NOT NULL UNIQUE,
    disponibilidad BOOLEAN NOT NULL,
    hora_apertura TIME NOT NULL,
    hora_cierre TIME NOT NULL
);

CREATE TABLE inscripcion (
    id_inscripcion INT PRIMARY KEY AUTO_INCREMENT,
    id_torneo INT NOT NULL,
    id_participante INT NOT NULL,
    fecha DATE NOT NULL,
    estado_pago BOOLEAN NOT NULL,
    estado_inscripcion VARCHAR(20) NOT NULL,
    motivo_baja VARCHAR(200),
    FOREIGN KEY (id_torneo) REFERENCES torneo(id_torneo),
    FOREIGN KEY (id_participante) REFERENCES participante(id_participante)
);

-- Cuadro y Zona heredan de Formato (una tabla por subclase, igual que Participante)
CREATE TABLE formato (
    id_formato INT PRIMARY KEY AUTO_INCREMENT,
    id_torneo INT NOT NULL,
    FOREIGN KEY (id_torneo) REFERENCES torneo(id_torneo)
);

CREATE TABLE cuadro (
    id_cuadro INT PRIMARY KEY AUTO_INCREMENT,
    id_formato INT NOT NULL UNIQUE,
    tipo_cuadro VARCHAR(30) NOT NULL,
    tamanio INT NOT NULL,
    cantidad_byes INT NOT NULL,
    FOREIGN KEY (id_formato) REFERENCES formato(id_formato)
);

CREATE TABLE zona (
    id_zona INT PRIMARY KEY AUTO_INCREMENT,
    id_formato INT NOT NULL UNIQUE,
    numero_zona INT NOT NULL,
    FOREIGN KEY (id_formato) REFERENCES formato(id_formato)
);

-- Los participantes quedan en NULL mientras el partido esta "a definir".
-- ronda y posicion ubican el partido dentro del cuadro (en una zona, ronda = 0).
CREATE TABLE partido (
    id_partido INT PRIMARY KEY AUTO_INCREMENT,
    id_formato INT NOT NULL,
    id_participante1 INT,
    id_participante2 INT,
    instancia VARCHAR(30) NOT NULL,
    ronda INT NOT NULL,
    posicion INT NOT NULL,
    id_cancha INT,
    fecha DATE,
    horario TIME,
    estado VARCHAR(20) NOT NULL,
    FOREIGN KEY (id_formato) REFERENCES formato(id_formato),
    FOREIGN KEY (id_participante1) REFERENCES participante(id_participante),
    FOREIGN KEY (id_participante2) REFERENCES participante(id_participante),
    FOREIGN KEY (id_cancha) REFERENCES cancha(id_cancha)
);

CREATE TABLE resultado (
    id_resultado INT PRIMARY KEY AUTO_INCREMENT,
    id_partido INT NOT NULL UNIQUE,
    marcador VARCHAR(50),
    id_ganador INT NOT NULL,
    es_wo BOOLEAN NOT NULL,
    es_retiro BOOLEAN NOT NULL,
    FOREIGN KEY (id_partido) REFERENCES partido(id_partido),
    FOREIGN KEY (id_ganador) REFERENCES participante(id_participante)
);

-- Usuario que ingresa al sistema (RNF08). No se guarda la contrasena:
-- se guarda su hash PBKDF2 y la sal aleatoria con la que se calculo, codificados en Base64.
CREATE TABLE usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    hash_contrasena VARCHAR(100) NOT NULL,
    sal VARCHAR(50) NOT NULL
);

-- ============================================================
-- 2. CONSULTAS DE VERIFICACION
-- (se ejecutan despues de usar el programa)
-- ============================================================

-- 2.1 Listado de inscriptos con estado de pago
SELECT
    T.nombre AS 'Torneo',
    P.nombre_representativo AS 'Participante',
    I.fecha AS 'Fecha de inscripcion',
    I.estado_pago AS 'Pago realizado',
    I.estado_inscripcion AS 'Estado'
FROM inscripcion I
    JOIN participante P ON I.id_participante = P.id_participante
    JOIN torneo T ON I.id_torneo = T.id_torneo
ORDER BY T.nombre, I.fecha;

-- 2.2 Inscriptos con pago pendiente
SELECT
    T.nombre AS 'Torneo',
    P.nombre_representativo AS 'Participante',
    I.fecha AS 'Fecha de inscripcion'
FROM inscripcion I
    JOIN participante P ON I.id_participante = P.id_participante
    JOIN torneo T ON I.id_torneo = T.id_torneo
WHERE I.estado_pago = FALSE
    AND I.estado_inscripcion <> 'BAJA';

-- 2.3 Cantidad de inscriptos activos por torneo
SELECT
    T.nombre AS 'Torneo',
    COUNT(I.id_inscripcion) AS 'Cantidad de inscriptos'
FROM torneo T
    LEFT JOIN inscripcion I ON T.id_torneo = I.id_torneo AND I.estado_inscripcion <> 'BAJA'
GROUP BY T.id_torneo, T.nombre;

-- 2.4 Programacion de partidos con su cancha y resultado
SELECT
    T.nombre AS 'Torneo',
    PA.instancia AS 'Instancia',
    P1.nombre_representativo AS 'Participante 1',
    P2.nombre_representativo AS 'Participante 2',
    C.numero AS 'Cancha',
    PA.fecha AS 'Fecha',
    PA.horario AS 'Horario',
    R.marcador AS 'Resultado'
FROM partido PA
    JOIN formato F ON PA.id_formato = F.id_formato
    JOIN torneo T ON F.id_torneo = T.id_torneo
    LEFT JOIN participante P1 ON PA.id_participante1 = P1.id_participante
    LEFT JOIN participante P2 ON PA.id_participante2 = P2.id_participante
    LEFT JOIN cancha C ON PA.id_cancha = C.id_cancha
    LEFT JOIN resultado R ON R.id_partido = PA.id_partido
ORDER BY PA.fecha, PA.horario, C.numero;
