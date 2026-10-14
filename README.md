# Primer Punto

Sistema de gestión y organización de torneos de tenis para el club **Los Cuervos Tenis**.

Proyecto desarrollado para la materia **Seminario de Práctica** de la Licenciatura en Informática (Universidad Siglo 21).

## Descripción

Primer Punto centraliza en una misma herramienta todas las etapas de la organización de un torneo de tenis:

- Configuración de torneos (modalidad, costo, fechas, lugar y plazo de inscripción).
- Inscripción de jugadores o parejas y registro del estado de pago.
- Generación automática de cuadros de eliminación directa y zonas de todos contra todos.
- Programación de partidos según la disponibilidad de canchas y horarios.
- Registro de resultados, tablas de posiciones e historial de torneos.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java |
| Interfaz gráfica | JavaFX |
| Base de datos | MySQL |
| Conexión a la base de datos | JDBC (MySQL Connector/J) |

La aplicación se ejecuta en una PC del club, con la base de datos MySQL instalada en el mismo equipo (`localhost:3306`).

## Estructura del repositorio

```
primer-punto/
├── README.md
└── base_datos/
    └── primer_punto.sql   → creación de la base y las tablas, datos de prueba, consultas y borrado
```

## Cómo ejecutar el script de la base de datos

1. Abrir un cliente de MySQL (por ejemplo, HeidiSQL o MySQL Workbench).
2. Abrir el archivo `base_datos/primer_punto.sql`.
3. Ejecutar el script completo: crea la base `primer_punto`, sus 11 tablas, inserta datos de prueba, ejecuta las consultas y finalmente borra los datos de prueba.

## Autora

Antonella Marinelli
