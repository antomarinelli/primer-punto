# Primer Punto

Sistema de gestión y organización de torneos de tenis para el club **Los Cuervos Tenis**.

Proyecto de la materia **Seminario de Práctica** – Licenciatura en Informática, Universidad Siglo 21.

## Funcionalidades

- **Ingreso:** usuario y contraseña del organizador; la contraseña se guarda como hash (PBKDF2 con sal).
- **Torneos:** alta de torneos (modalidad, costo, fechas, plazo de inscripción, duración de los partidos y formato de resultado), consulta, filtro por estado, ordenamiento y búsqueda por fecha, cierre de inscripción y finalización.
- **Participantes:** registro de jugadores, inscripción de jugadores o parejas con control de plazo, duplicados y pago; registro de pagos y bajas dentro y fuera de plazo; exportación de inscriptos a CSV.
- **Formato:** generación de cuadros de eliminación directa (con cálculo de byes) y zonas de todos contra todos, por sorteo o armados a mano; tabla de posiciones y campeón.
- **Programación:** calendario de canchas por día, programación manual y automática respetando horarios de las canchas, descanso mínimo entre partidos y máximo de partidos por día; carga de resultados; exportación del día a CSV.
- **Historial** de torneos finalizados.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Interfaz gráfica | JavaFX 23 |
| Base de datos | MySQL, con JDBC (driver Connector/J), sin frameworks de persistencia |
| Construcción del proyecto | Maven |
| Pruebas | JUnit 5 |

El código sigue el patrón modelo-vista-controlador: `vista` (ventanas), `negocio` (controladores) y `modelo` + `datos` (entidades y su persistencia en MySQL).

## Estructura

```
primer-punto/
├── pom.xml
├── primer_punto.sql                   script de creación de la base de datos
├── config.properties.ejemplo          modelo del archivo de conexión a MySQL
└── src/
    ├── main/java/primerpunto/
    │   ├── App.java                   punto de entrada
    │   ├── modelo/                    entidades (Torneo, Jugador, Pareja, Cuadro, Zona, Partido, Usuario...)
    │   ├── negocio/                   controladores (GestionTorneos, GestionParticipantes, GestionUsuarios...)
    │   ├── datos/                     acceso a MySQL con JDBC (IAccesoDatos, DAO) y exportación a CSV
    │   ├── excepciones/               excepciones propias del sistema
    │   └── vista/                     ventanas JavaFX
    ├── main/resources/primerpunto/vista/   estilos, logo y tipografía
    └── test/java/primerpunto/         pruebas automatizadas
```

## Cómo ejecutarlo

Requisitos: JDK 21 o superior, Maven (incluido en NetBeans) y un servidor MySQL.

1. Ejecutar `primer_punto.sql` en MySQL: crea la base `primer_punto` con sus 12 tablas.
2. Copiar `config.properties.ejemplo` como `config.properties` (en la misma carpeta) y completar el usuario y la contraseña de MySQL.
3. Ejecutar:
   - **NetBeans:** abrir la carpeta del proyecto y ejecutar con *Run* (F6).
   - **Consola:** `mvn clean javafx:run`

La primera vez se crea la cuenta del organizador y, como la base está vacía, se cargan datos de ejemplo.

## Pruebas

Los casos de prueba de la documentación (CPF01, CPC02, CPS03, CPS04 y CPS05) y las reglas del sistema están automatizados con JUnit (50 pruebas). No necesitan la base de datos:

- **NetBeans:** clic derecho sobre el proyecto → *Test* (Alt + F6).
- **Consola:** `mvn test`

## Autora

Antonella Marinelli
