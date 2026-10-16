package primerpunto.vista;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.Inscripcion;
import primerpunto.modelo.Jugador;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionParticipantes;
import primerpunto.negocio.Sistema;

/**
 * Ventana del módulo Participantes: registrar jugadores (CU01) e inscribir un jugador
 * o una pareja en un torneo con su estado de pago (CU05 y CU06). Cada operación se
 * despliega al tocar su botón. Abajo se muestra el padrón de jugadores del club.
 */
public class VentanaParticipantes extends VistaBase {

    private final GestionParticipantes gestion;

    // Registro de jugador
    private final TextField txtNombre = texto("");
    private final TextField txtApellido = texto("");
    private final TextField txtDni = texto("Sin puntos");
    private final TextField txtCelular = texto("");
    private final TextField txtEmail = texto("");
    private final VBox formRegistro = new VBox(18);

    // Inscripción
    private final ComboBox<Torneo> cmbTorneo = combo("Seleccionar");
    private final ComboBox<Jugador> cmbJugador1 = combo("Seleccionar");
    private final ComboBox<Jugador> cmbJugador2 = combo("Seleccionar");
    private final VBox campoJugador2 = campo("Jugador 2", cmbJugador2);
    private final CheckBox chkPagado = new CheckBox("Pago abonado");
    private final Label lblInfoTorneo = etiquetaClase("", "dato-aclaracion");
    private final VBox formInscripcion = new VBox(18);

    // Padrón de jugadores
    private final TextField txtBuscar = texto("Buscar jugador o DNI");
    private final TableView<Jugador> tablaJugadores = tabla("No hay jugadores registrados.");

    public VentanaParticipantes(Sistema sistema) {
        super(sistema, "Participantes");
        this.gestion = sistema.getGestionParticipantes();
        getChildren().addAll(crearAcciones(), crearPadron());
    }

    // ------------------------------------------------------------ armado de la pantalla

    /** Dos botones grandes: cada uno despliega su formulario (solo uno abierto a la vez). */
    private VBox crearAcciones() {
        crearFormRegistro();
        crearFormInscripcion();
        VBox botonRegistro = botonAccion("+", "Registrar jugador");
        VBox botonInscripcion = botonAccion("»", "Inscribir participante");
        HBox fila = new HBox(22, botonRegistro, botonInscripcion);

        mostrarNodo(formRegistro, false);
        mostrarNodo(formInscripcion, false);
        botonRegistro.setOnMouseClicked(e -> abrir(formRegistro, botonRegistro, formInscripcion, botonInscripcion));
        botonInscripcion.setOnMouseClicked(e -> abrir(formInscripcion, botonInscripcion, formRegistro, botonRegistro));
        return new VBox(18, fila, formRegistro, formInscripcion);
    }

    private VBox botonAccion(String icono, String titulo) {
        Label lblIcono = etiquetaClase(icono, "accion-icono");
        HBox contenido = new HBox(18, lblIcono, etiquetaClase(titulo, "tarjeta-titulo"));
        contenido.setAlignment(Pos.CENTER_LEFT);
        VBox boton = new VBox(contenido);
        boton.getStyleClass().add("boton-accion");
        HBox.setHgrow(boton, Priority.ALWAYS);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setPrefWidth(10);
        return boton;
    }

    /** Abre un formulario y cierra el otro; si ya estaba abierto, lo cierra. */
    private void abrir(Node formulario, Node boton, Node otroFormulario, Node otroBoton) {
        boolean mostrar = !formulario.isVisible();
        mostrarNodo(formulario, mostrar);
        marcarActivo(boton, mostrar);
        mostrarNodo(otroFormulario, false);
        marcarActivo(otroBoton, false);
    }

    private void crearFormRegistro() {
        formRegistro.getStyleClass().add("tarjeta");
        formRegistro.getChildren().addAll(
                etiquetaClase("Registrar jugador", "tarjeta-titulo"),
                filaCampos(campo("Nombre", txtNombre), campo("Apellido", txtApellido), campo("DNI", txtDni)),
                filaCampos(campo("Celular", txtCelular), campo("Email", txtEmail), new VBox()),
                botonPrimario("Registrar jugador", e -> registrarJugador()));
    }

    private void crearFormInscripcion() {
        cmbTorneo.setOnAction(e -> alCambiarTorneo());
        formInscripcion.getStyleClass().add("tarjeta");
        formInscripcion.getChildren().addAll(
                etiquetaClase("Inscribir participante", "tarjeta-titulo"),
                campo("Torneo", cmbTorneo),
                lblInfoTorneo,
                filaCampos(campo("Jugador 1", cmbJugador1), campoJugador2),
                chkPagado,
                botonPrimario("Confirmar inscripción", e -> inscribir()));
    }

    private VBox crearPadron() {
        tablaJugadores.getColumns().addAll(List.of(
                columna("Jugador", Jugador::obtenerNombreCompleto, 200),
                columna("DNI", Jugador::getDni, 110),
                columna("Celular", j -> j.getCelular().isEmpty() ? "—" : j.getCelular(), 140),
                columna("Email", j -> j.getEmail().isEmpty() ? "—" : j.getEmail(), 220),
                columna("Torneos activos", j -> String.valueOf(torneosActivos(j)), 120)));
        tablaJugadores.setPrefHeight(420);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> cargarJugadores());
        txtBuscar.setMaxWidth(420);
        VBox tarjeta = tarjeta("Jugadores registrados");
        tarjeta.getChildren().addAll(txtBuscar, tablaJugadores);
        return tarjeta;
    }

    // ------------------------------------------------------------ acciones

    /** CU01: registra un jugador nuevo. */
    private void registrarJugador() {
        try {
            Jugador jugador = gestion.registrarJugador(txtNombre.getText(), txtApellido.getText(),
                    txtDni.getText().trim(), txtCelular.getText(), txtEmail.getText());
            mostrarInfo("Jugador registrado", jugador.obtenerNombreCompleto() + ".");
            for (TextField campo : List.of(txtNombre, txtApellido, txtDni, txtCelular, txtEmail)) {
                campo.clear();
            }
            actualizar();
            tablaJugadores.getSelectionModel().select(jugador);
            tablaJugadores.scrollTo(jugador);
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    /** CU05 y CU06: inscribe al jugador (o a la pareja) y registra el estado de pago. */
    private void inscribir() {
        Torneo torneo = cmbTorneo.getValue();
        try {
            if (torneo == null || cmbJugador1.getValue() == null) {
                mostrarError("Seleccioná el torneo y el participante a inscribir.");
                return;
            }
            Participante participante = cmbJugador1.getValue();
            if (torneo.getModalidad().requierePareja()) {
                if (cmbJugador2.getValue() == null) {
                    mostrarError("La modalidad " + torneo.getModalidad() + " requiere elegir a los dos jugadores de la pareja.");
                    return;
                }
                participante = gestion.asociarPareja(cmbJugador1.getValue(), cmbJugador2.getValue());
            }
            Inscripcion inscripcion = gestion.inscribirParticipante(torneo, participante, chkPagado.isSelected());
            mostrarInfo("Inscripción registrada", participante.obtenerNombreCompleto() + " en " + torneo.getNombre()
                    + (inscripcion.isEstadoPago() ? "." : ". Pago pendiente."));
            cmbJugador1.setValue(null);
            cmbJugador2.setValue(null);
            chkPagado.setSelected(false);
            actualizar();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    // ------------------------------------------------------------ datos en pantalla

    @Override
    public void actualizar() {
        // Solo se ofrecen los torneos que todavía admiten inscripciones
        Torneo seleccionado = cmbTorneo.getValue();
        List<Torneo> torneos = new ArrayList<>();
        for (Torneo torneo : sistema.getGestionTorneos().listarTorneos(null)) {
            boolean abierto = torneo.getEstado() == EstadoTorneo.INSCRIPCION_ABIERTA;
            boolean conVacante = torneo.getEstado() == EstadoTorneo.INSCRIPCION_CERRADA
                    && gestion.vacantesDisponibles(torneo) > 0;
            if (abierto || conVacante) {
                torneos.add(torneo);
            }
        }
        cmbTorneo.setItems(FXCollections.observableArrayList(torneos));
        if (seleccionado != null && torneos.contains(seleccionado)) {
            cmbTorneo.setValue(seleccionado);
        } else {
            cmbTorneo.getSelectionModel().selectFirst();
        }
        cmbJugador1.setItems(FXCollections.observableArrayList(gestion.listarJugadores()));
        cmbJugador2.setItems(FXCollections.observableArrayList(gestion.listarJugadores()));
        alCambiarTorneo();
        cargarJugadores();
    }

    /** Muestra el segundo jugador solo en las modalidades de pareja, y los datos del plazo. */
    private void alCambiarTorneo() {
        Torneo torneo = cmbTorneo.getValue();
        boolean pareja = torneo != null && torneo.getModalidad().requierePareja();
        mostrarNodo(campoJugador2, pareja);
        if (torneo == null) {
            lblInfoTorneo.setText("No hay torneos con inscripción abierta.");
            return;
        }
        String plazo;
        if (torneo.getPlazoInscripcion() == null) {
            plazo = "sin plazo de inscripción";
        } else if (torneo.verificarPlazoVencido()) {
            plazo = "plazo vencido · " + cantidad(gestion.vacantesDisponibles(torneo), "vacante", "vacantes");
        } else {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), torneo.getPlazoInscripcion());
            plazo = "cierra el " + formatear(torneo.getPlazoInscripcion())
                    + (dias == 0 ? " (hoy)" : " (" + cantidad(dias, "día", "días") + ")");
        }
        lblInfoTorneo.setText(torneo.getModalidad() + " · " + cantidad(torneo.getInscripcionesActivas().size(),
                "inscripto", "inscriptos") + " · " + plazo);
    }

    private void cargarJugadores() {
        String buscado = txtBuscar.getText().trim().toLowerCase();
        List<Jugador> lista = new ArrayList<>();
        for (Jugador jugador : gestion.listarJugadores()) {
            if (buscado.isEmpty() || jugador.obtenerNombreCompleto().toLowerCase().contains(buscado)
                    || jugador.getDni().contains(buscado)) {
                lista.add(jugador);
            }
        }
        tablaJugadores.setItems(FXCollections.observableArrayList(lista));
        tablaJugadores.setPrefHeight(Math.min(680, 50 + 40 * Math.max(3, lista.size())));
        tablaJugadores.refresh();
    }

    /** Cantidad de torneos no finalizados en los que el jugador está inscripto (solo o en pareja). */
    private long torneosActivos(Jugador jugador) {
        long cantidad = 0;
        for (Torneo torneo : sistema.getGestionTorneos().listarTorneos(null)) {
            if (torneo.getEstado() == EstadoTorneo.FINALIZADO) {
                continue;
            }
            for (Inscripcion inscripcion : torneo.getInscripcionesActivas()) {
                Participante p = inscripcion.getParticipante();
                if (p.equals(jugador) || (p instanceof primerpunto.modelo.Pareja pareja && pareja.contieneJugador(jugador))) {
                    cantidad++;
                }
            }
        }
        return cantidad;
    }
}
