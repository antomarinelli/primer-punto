# Primer Punto

Sistema de gestión y organización de torneos de tenis para el club **Los Cuervos Tenis**.

Proyecto de la materia **Seminario de Práctica** – Licenciatura en Informática, Universidad Siglo 21.

## Funcionalidades

- **Torneos:** alta de torneos (modalidad, costo, fechas, plazo de inscripción, duración de los partidos y formato de resultado), consulta, filtro por estado, ordenamiento y búsqueda por fecha, cierre de inscripción y finalización.
- **Participantes:** registro de jugadores, inscripción de jugadores o parejas con control de plazo, duplicados y pago; registro de pagos y bajas dentro y fuera de plazo.
- **Formato:** generación de cuadros de eliminación directa (con cálculo de byes) y zonas de todos contra todos, por sorteo o armados a mano; tabla de posiciones.
- **Programación:** calendario de canchas por día, programación manual y automática respetando horarios de las canchas, descanso mínimo entre partidos y máximo de partidos por día; carga de resultados.
- **Historial** de torneos finalizados.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Interfaz gráfica | JavaFX 23 |
| Construcción del proyecto | Maven |
| Pruebas | JUnit 5 |
| Base de datos (próxima etapa) | MySQL |

En esta versión del prototipo los datos se mantienen en memoria y se cargan datos de ejemplo al iniciar. La persistencia en MySQL se incorpora en la siguiente etapa.

## Estructura

```
primer-punto/
├── pom.xml
├── primer_punto.sql                   script de la base de datos
└── src/
    ├── main/java/primerpunto/
    │   ├── App.java                   punto de entrada
    │   ├── modelo/                    entidades (Torneo, Jugador, Pareja, Cuadro, Zona, Partido...)
    │   ├── negocio/                   controladores (GestionTorneos, GestionParticipantes...)
    │   ├── excepciones/               excepciones propias del sistema
    │   └── vista/                     ventanas JavaFX
    ├── main/resources/primerpunto/vista/   estilos, logo y tipografía
    └── test/java/primerpunto/         casos de prueba automatizados
```

## Cómo ejecutarlo

Requisitos: JDK 21 o superior y Maven (incluido en NetBeans).

- **NetBeans:** abrir la carpeta del proyecto y ejecutar con *Run* (F6).
- **Consola:** `mvn clean javafx:run`

## Pruebas

Los casos de prueba de la documentación (CPF01, CPC02, CPS03, CPS04 y CPS05) y las reglas de programación están automatizados con JUnit:

- **NetBeans:** clic derecho sobre el proyecto → *Test* (Alt + F6).
- **Consola:** `mvn test`

## Autora

Antonella Marinelli
