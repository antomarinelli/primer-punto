package primerpunto.vista;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.Inscripcion;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.Sistema;

/**
 * Ficha completa de un torneo: sus datos generales siempre visibles y tres
 * secciones que se despliegan al tocar su botón: participantes, formato y partidos.
 *
 * En la ventana Torneos la ficha permite operar (registrar pagos, dar de baja,
 * cerrar la inscripción o finalizar el torneo); en el Historial es solo de consulta.
 */
public class FichaTorneo extends VBox {

    private static final NumberFormat PESOS = NumberFormat.getNumberInstance(Locale.of("es", "AR"));

    private final VistaBase vista;
    private final Sistema sistema;
    private final Torneo torneo;
    private final boolean permiteOperar;
    private final Runnable alCambiar;
    private TableView<Inscripcion> tablaInscriptos;

    /**
     * @param permiteOperar  true en Torneos (muestra las acciones); false en el Historial
     * @param desplegada     si las secciones se muestran abiertas al principio
     * @param alCambiar      acción a ejecutar después de una operación (para refrescar la ventana)
     */
    public FichaTorneo(VistaBase vista, Sistema sistema, Torneo torneo, boolean permiteOperar, boolean desplegada,
                       Runnable alCambiar) {
        super(20);
        this.vista = vista;
        this.sistema = sistema;
        this.torneo = torneo;
        this.permiteOperar = permiteOperar;
        this.alCambiar = alCambiar;
        getStyleClass().add("ficha");

        getChildren().addAll(crearEncabezado(), crearDatos());

        // Secciones desplegables
        Node participantes = crearParticipantes();
        Node formato = new PanelFormato(vista, sistema.getGestionCuadrosZonas(), torneo);
        Node partidos = crearPartidos();
        boolean tieneFormato = !torneo.getFormatos().isEmpty();
        Button btnParticipantes = vista.botonSecundario("Participantes (" + torneo.getInscripcionesActivas().size() + ")", null);
        Button btnFormato = vista.botonSecundario("Formato", null);
        Button btnPartidos = vista.botonSecundario("Partidos (" + torneo.getPartidos().size() + ")", null);
        for (Button boton : List.of(btnParticipantes, btnFormato, btnPartidos)) {
            boton.getStyleClass().add("boton-seccion");
        }
        vista.alternarAlTocar(btnParticipantes, participantes, desplegada);
        vista.alternarAlTocar(btnFormato, formato, desplegada && tieneFormato);
        vista.alternarAlTocar(btnPartidos, partidos, desplegada && tieneFormato);

        HBox botones = new HBox(10, btnParticipantes, btnFormato, btnPartidos);
        if (permiteOperar) {
            botones.getChildren().addAll(vista.espacio(), crearAcciones());
        }
        botones.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(botones, participantes, formato, partidos);
    }

    // ------------------------------------------------------------ datos generales

    private HBox crearEncabezado() {
        Label nombre = vista.etiquetaClase(torneo.getNombre(), "ficha-titulo");
        Label estado = new Label(torneo.getEstado().toString());
        estado.getStyleClass().addAll("chip", torneo.getEstado() == EstadoTorneo.FINALIZADO
                || torneo.getEstado() == EstadoTorneo.INSCRIPCION_CERRADA ? "chip-gris" : "chip-verde");
        Label modalidad = new Label(torneo.getModalidad().toString());
        modalidad.getStyleClass().addAll("chip", "chip-gris");
        HBox encabezado = new HBox(12, nombre, estado, modalidad);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        return encabezado;
    }

    private FlowPane crearDatos() {
        FlowPane datos = new FlowPane(36, 14);
        datos.getChildren().addAll(
                dato("Lugar", torneo.getLugar() == null || torneo.getLugar().isBlank() ? "—" : torneo.getLugar()),
                dato("Costo de inscripción", "$ " + PESOS.format(torneo.getCosto())),
                dato("Fecha de inicio", vista.formatear(torneo.getFechaInicio())),
                dato("Fecha de fin", vista.formatear(torneo.getFechaFin())),
                dato("Plazo de inscripción", vista.formatear(torneo.getPlazoInscripcion())),
                dato("Duración del partido", VistaBase.formatearDuracion(torneo.getDuracionPartidoMinutos())),
                dato("Formato de resultado", torneo.getFormatoResultado().toString()),
                dato("Inscriptos", String.valueOf(torneo.getInscripcionesActivas().size())));
        Cuadro cuadro = sistema.getGestionCuadrosZonas().obtenerCuadro(torneo);
        if (cuadro != null && cuadro.getCampeon() != null) {
            datos.getChildren().add(dato("Campeón", cuadro.getCampeon().obtenerNombreCompleto()));
        }
        return datos;
    }

    private VBox dato(String titulo, String valor) {
        return new VBox(2, vista.etiquetaClase(titulo.toUpperCase(), "etiqueta"), vista.etiquetaClase(valor, "dato-ficha"));
    }

    /** Acciones según el estado del torneo. */
    private HBox crearAcciones() {
        HBox acciones = new HBox(10);
        if (torneo.getEstado() == EstadoTorneo.INSCRIPCION_ABIERTA) {
            acciones.getChildren().add(vista.botonSecundario("Cerrar inscripción", e -> cerrarInscripcion()));
        }
        if (torneo.getEstado() == EstadoTorneo.EN_CURSO) {
            acciones.getChildren().add(vista.botonPeligro("Finalizar torneo", e -> finalizarTorneo()));
        }
        return acciones;
    }

    // ------------------------------------------------------------ secciones

    private VBox crearParticipantes() {
        tablaInscriptos = vista.tabla("El torneo todavía no tiene inscriptos.");
        tablaInscriptos.getColumns().addAll(List.of(
                vista.columna("N.º", i -> String.valueOf(i.getIdInscripcion()), 50),
                vista.columna("Participante", i -> i.getParticipante().obtenerNombreCompleto(), 230),
                vista.columna("Fecha de inscripción", i -> vista.formatear(i.getFecha()), 140),
                vista.columnaChip("Pago", i -> i.isEstadoPago() ? "Abonado" : "Pendiente",
                        i -> i.isEstadoPago() ? "chip-verde" : (i.tieneDeuda() ? "chip-rojo" : "chip-gris"), 110),
                vista.columnaChip("Estado", i -> i.getEstadoInscripcion().toString(),
                        i -> i.estaHabilitado() ? "chip-verde" : "chip-gris", 130),
                vista.columna("Motivo de baja", i -> i.getMotivoBaja() == null ? "—" : i.getMotivoBaja(), 200)));
        tablaInscriptos.setItems(FXCollections.observableArrayList(torneo.getInscripciones()));
        tablaInscriptos.setPrefHeight(Math.min(560, 50 + 40 * Math.max(2, torneo.getInscripciones().size())));
        VBox seccion = new VBox(12, vista.etiquetaClase("Participantes inscriptos", "subtitulo-seccion"), tablaInscriptos);
        boolean inscripcionEnCurso = torneo.getEstado() != EstadoTorneo.FINALIZADO;
        if (permiteOperar && inscripcionEnCurso) {
            seccion.getChildren().add(new HBox(10,
                    vista.botonSecundario("Registrar pago", e -> registrarPago()),
                    vista.botonPeligro("Dar de baja", e -> darDeBaja())));
        }
        return seccion;
    }

    private VBox crearPartidos() {
        TableView<Partido> tabla = vista.tabla("Este torneo todavía no tiene partidos.");
        tabla.getColumns().addAll(List.of(
                vista.columna("Fecha", p -> vista.formatear(p.getFecha()), 95),
                vista.columna("Horario", p -> p.getHorario() == null ? "—" : p.getHorario().toString(), 70),
                vista.columna("Cancha", p -> p.getCancha() == null ? "—" : p.getCancha().toString(), 85),
                vista.columna("Instancia", Partido::getInstancia, 125),
                vista.columna("Partido", Partido::toString, 300),
                vista.columna("Resultado", p -> p.getResultado() == null ? "—" : p.getResultado().getMarcador()
                        + (p.getResultado().isEsRetiro() ? " (ret.)" : ""), 110),
                vista.columnaChip("Estado", p -> p.getEstado().toString(),
                        p -> p.estaFinalizado() ? "chip-gris" : (p.estaProgramado() ? "chip-verde" : "chip-rojo"), 120)));
        List<Partido> partidos = new java.util.ArrayList<>(torneo.getPartidos());
        partidos.sort((a, b) -> {
            if (a.getFecha() == null || b.getFecha() == null) {
                return a.getFecha() == null ? (b.getFecha() == null ? 0 : 1) : -1;
            }
            int porFecha = a.getFecha().atTime(a.getHorario()).compareTo(b.getFecha().atTime(b.getHorario()));
            return porFecha;
        });
        tabla.setItems(FXCollections.observableArrayList(partidos));
        tabla.setPrefHeight(Math.min(560, 50 + 40 * Math.max(2, partidos.size())));
        return new VBox(12, vista.etiquetaClase("Partidos y programación", "subtitulo-seccion"), tabla);
    }

    // ------------------------------------------------------------ operaciones

    private void cerrarInscripcion() {
        try {
            sistema.getGestionTorneos().cerrarInscripcion(torneo);
            alCambiar.run();
        } catch (PrimerPuntoException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** CU15: finaliza el torneo si ya se disputaron todos sus partidos. */
    private void finalizarTorneo() {
        if (!vista.confirmar("Finalizar torneo", "¿Finalizar " + torneo.getNombre() + "?")) {
            return;
        }
        try {
            sistema.getGestionTorneos().finalizarTorneo(torneo,
                    sistema.getGestionCuadrosZonas().todosLosPartidosJugados(torneo));
            vista.mostrarInfo("Torneo finalizado", torneo.getNombre() + " pasó al historial.");
            alCambiar.run();
        } catch (PrimerPuntoException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** CU06: registra el pago de una inscripción que había quedado pendiente. */
    private void registrarPago() {
        Inscripcion inscripcion = tablaInscriptos.getSelectionModel().getSelectedItem();
        if (inscripcion == null) {
            vista.mostrarError("Seleccioná una inscripción de la lista.");
            return;
        }
        try {
            sistema.getGestionParticipantes().registrarPago(inscripcion);
            alCambiar.run();
        } catch (PrimerPuntoException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /**
     * CU09: dentro del plazo se pide solo confirmación; fuera del plazo se exige un
     * motivo y el organizador decide si lo acepta o lo rechaza.
     */
    private void darDeBaja() {
        Inscripcion inscripcion = tablaInscriptos.getSelectionModel().getSelectedItem();
        if (inscripcion == null) {
            vista.mostrarError("Seleccioná una inscripción de la lista.");
            return;
        }
        GestionParticipantes gestion = sistema.getGestionParticipantes();
        String nombre = inscripcion.getParticipante().obtenerNombreCompleto();
        try {
            if (!torneo.verificarPlazoVencido()) {
                if (vista.confirmar("Dar de baja", "¿Confirmás la baja de " + nombre + "?")) {
                    gestion.procesarBaja(torneo, inscripcion, null, false);
                    vista.mostrarInfo("Baja registrada", nombre + ".");
                }
            } else {
                Optional<Object[]> decision = pedirMotivoBaja(nombre);
                if (decision.isEmpty()) {
                    return;
                }
                boolean dadoDeBaja = gestion.procesarBaja(torneo, inscripcion, (String) decision.get()[0],
                        (Boolean) decision.get()[1]);
                if (dadoDeBaja) {
                    vista.mostrarInfo("Baja registrada", nombre + ". Queda una vacante disponible.");
                } else {
                    vista.mostrarInfo("Baja rechazada", nombre + " sigue inscripto"
                            + (inscripcion.tieneDeuda() ? ", con el pago pendiente." : "."));
                }
            }
            alCambiar.run();
        } catch (PrimerPuntoException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** Diálogo de baja fuera de plazo: devuelve el motivo y si el organizador lo aceptó. */
    private Optional<Object[]> pedirMotivoBaja(String nombre) {
        Dialog<Object[]> dialogo = new Dialog<>();
        dialogo.setTitle("Primer Punto");
        dialogo.setHeaderText("Baja fuera de plazo: " + nombre);
        TextArea txtMotivo = new TextArea();
        
        txtMotivo.setPrefRowCount(3);
        txtMotivo.setWrapText(true);
        VBox contenido = new VBox(10, new Label("Motivo"), txtMotivo);
        contenido.setPadding(new Insets(10, 0, 0, 0));
        dialogo.getDialogPane().setContent(contenido);
        ButtonType aceptar = new ButtonType("Aceptar baja", ButtonBar.ButtonData.OK_DONE);
        ButtonType rechazar = new ButtonType("Rechazar baja", ButtonBar.ButtonData.OTHER);
        dialogo.getDialogPane().getButtonTypes().addAll(aceptar, rechazar, ButtonType.CANCEL);
        dialogo.setResultConverter(boton -> {
            if (boton == aceptar) {
                return new Object[] {txtMotivo.getText(), true};
            }
            if (boton == rechazar) {
                return new Object[] {txtMotivo.getText(), false};
            }
            return null;
        });
        vista.prepararDialogo(dialogo);
        return dialogo.showAndWait();
    }
}
