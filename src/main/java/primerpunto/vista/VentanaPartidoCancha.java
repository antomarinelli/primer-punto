package primerpunto.vista;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import primerpunto.excepciones.DescansoInsuficienteException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cancha;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionPartidosCanchas;
import primerpunto.negocio.Sistema;

/**
 * Ventana de Programación: un calendario por día con las canchas en columnas y los
 * horarios en filas. Se elige un partido (de la lista de pendientes o del calendario)
 * y se toca un horario libre para asignarlo (CU11). Desde acá también se cargan los
 * resultados (CU13) y se administran las canchas del club (CU02).
 */
public class VentanaPartidoCancha extends VistaBase {

    private static final double ALTO_HORA = 72;
    private static final double ANCHO_HORAS = 64;
    private static final double ANCHO_CANCHA = 185;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy",
            Locale.of("es", "AR"));

    private final GestionPartidosCanchas gestion;

    // Barra superior
    private final ComboBox<Torneo> cmbTorneo = combo("Seleccionar");
    private final Label lblDia = etiquetaClase("", "dia-calendario");
    private final DatePicker dpDia = fecha();
    private LocalDate dia = LocalDate.now();
    private final javafx.scene.layout.FlowPane diasTorneo = new javafx.scene.layout.FlowPane(8, 8);
    private final Label lblPeriodo = etiquetaClase("", "dato-aclaracion");
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("EEE d/MM", Locale.of("es", "AR"));

    // Partidos sin programar y partido elegido
    private final ListView<Partido> listaPendientes = new ListView<>();
    private final VBox panelSeleccion = new VBox(10);
    private Partido seleccionado;

    // Calendario
    private final Pane calendario = new Pane();
    private final HBox encabezadoCanchas = new HBox();

    // Canchas
    private final TableView<Cancha> tablaCanchas = tabla("No hay canchas registradas.");
    private final TextField txtNumeroCancha = texto("");
    private final ComboBox<LocalTime> cmbApertura = combo("Apertura");
    private final ComboBox<LocalTime> cmbCierre = combo("Cierre");

    public VentanaPartidoCancha(Sistema sistema) {
        super(sistema, "Programación");
        this.gestion = sistema.getGestionPartidosCanchas();
        HBox cuerpo = new HBox(22, crearPanelIzquierdo(), crearCalendario());
        getChildren().addAll(crearBarra(), cuerpo, crearCanchas());
    }

    // ------------------------------------------------------------ armado de la pantalla

    private VBox crearBarra() {
        cmbTorneo.setPrefWidth(280);
        cmbTorneo.setOnAction(e -> {
            seleccionado = null;
            dia = diaSugerido(cmbTorneo.getValue()); // salta al día del próximo partido del torneo
            cargarPendientes();
            mostrarSeleccion();
            dibujarCalendario();
        });
        dpDia.setPrefWidth(170);
        dpDia.setOnAction(e -> {
            if (dpDia.getValue() != null) {
                irA(dpDia.getValue());
            }
        });
        HBox navegacion = new HBox(8,
                botonSecundario("<", e -> irA(dia.minusDays(1))),
                botonSecundario("Hoy", e -> irA(LocalDate.now())),
                botonSecundario(">", e -> irA(dia.plusDays(1))),
                lblDia);
        navegacion.setAlignment(Pos.CENTER_LEFT);
        VBox campoTorneo = campo("Torneo", cmbTorneo);
        VBox campoFecha = campo("Ir a la fecha", dpDia);
        HBox.setHgrow(campoTorneo, Priority.NEVER);
        HBox.setHgrow(campoFecha, Priority.NEVER);
        HBox fila = new HBox(16, campoTorneo, campoFecha, espacio(),
                botonPrimario("Programar automáticamente", e -> programarAutomatico()));
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox dias = new VBox(8, lblPeriodo, diasTorneo);
        VBox barra = new VBox(14, fila, navegacion, dias);
        barra.getStyleClass().add("barra");
        return barra;
    }

    private VBox crearPanelIzquierdo() {
        listaPendientes.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Partido partido, boolean vacio) {
                super.updateItem(partido, vacio);
                if (vacio || partido == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VBox caja = new VBox(1, etiquetaClase(partido.getInstancia().toUpperCase(), "etiqueta"),
                            etiquetaClase(partido.getNombreParticipante1(), "texto-partido"),
                            etiquetaClase("vs. " + partido.getNombreParticipante2(), "texto-partido"));
                    setGraphic(caja);
                    setText(null);
                }
            }
        });
        listaPendientes.getStyleClass().add("lista-pendientes");
        listaPendientes.setPrefHeight(300);
        listaPendientes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, partido) -> {
            if (partido != null) {
                seleccionado = partido;
                mostrarSeleccion();
                dibujarCalendario();
            }
        });
        VBox pendientes = tarjeta("Sin programar");
        pendientes.getChildren().add(listaPendientes);

        panelSeleccion.getStyleClass().add("tarjeta-verde");
        VBox panel = new VBox(18, pendientes, panelSeleccion);
        panel.setPrefWidth(285);
        panel.setMinWidth(285);
        panel.setMaxWidth(285);
        return panel;
    }

    private VBox crearCalendario() {
        ScrollPane desplazable = new ScrollPane(new VBox(encabezadoCanchas, calendario));
        desplazable.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazable.setFitToHeight(true);
        desplazable.getStyleClass().add("transparente");
        VBox tarjeta = tarjeta(null);
        tarjeta.getChildren().add(desplazable);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private VBox crearCanchas() {
        tablaCanchas.getColumns().addAll(List.of(
                columna("Cancha", c -> "Cancha " + c.getNumero(), 120),
                columna("Apertura", c -> c.getHoraApertura().toString(), 100),
                columna("Cierre", c -> c.getHoraCierre().toString(), 100),
                columnaChip("Disponibilidad", c -> c.getDisponibilidad() ? "Habilitada" : "Inhabilitada",
                        c -> c.getDisponibilidad() ? "chip-verde" : "chip-gris", 140)));
        tablaCanchas.setPrefHeight(200);
        tablaCanchas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, cancha) -> {
            if (cancha != null) {
                txtNumeroCancha.setText(String.valueOf(cancha.getNumero()));
                cmbApertura.setValue(cancha.getHoraApertura());
                cmbCierre.setValue(cancha.getHoraCierre());
            }
        });
        List<LocalTime> horarios = new ArrayList<>();
        for (int hora = 6; hora <= 23; hora++) {
            horarios.add(LocalTime.of(hora, 0));
        }
        cmbApertura.setItems(FXCollections.observableArrayList(horarios));
        cmbCierre.setItems(FXCollections.observableArrayList(horarios));
        HBox formulario = new HBox(22, campo("Número", txtNumeroCancha), campo("Apertura", cmbApertura),
                campo("Cierre", cmbCierre));
        formulario.setMaxWidth(620);
        HBox botones = new HBox(12, botonPrimario("Registrar cancha", e -> registrarCancha()),
                botonSecundario("Modificar cancha", e -> modificarCancha()),
                botonSecundario("Habilitar / inhabilitar", e -> cambiarDisponibilidad()));
        VBox contenido = new VBox(16, tablaCanchas, formulario, botones);

        Button btnCanchas = botonSecundario("Canchas del club", null);
        btnCanchas.getStyleClass().add("boton-seccion");
        alternarAlTocar(btnCanchas, contenido, false);
        VBox tarjeta = tarjeta(null);
        tarjeta.getChildren().addAll(new HBox(12, btnCanchas), contenido);
        ((HBox) tarjeta.getChildren().get(0)).setAlignment(Pos.CENTER_LEFT);
        return tarjeta;
    }

    // ------------------------------------------------------------ calendario

    private void irA(LocalDate nuevoDia) {
        dia = nuevoDia;
        dibujarCalendario();
    }

    /**
     * Dibuja el día elegido: una columna por cancha y una fila por hora. Cada partido
     * ocupa la duración definida para su torneo; las horas en que la cancha está
     * cerrada se ven sombreadas.
     */
    private void dibujarCalendario() {
        lblDia.setText(primeraMayuscula(dia.format(DIA)));
        actualizarDiasTorneo();
        calendario.getChildren().clear();
        encabezadoCanchas.getChildren().clear();
        List<Cancha> canchas = gestion.listarCanchas();
        if (canchas.isEmpty()) {
            calendario.getChildren().add(etiquetaClase("No hay canchas registradas.", "mensaje-vacio"));
            return;
        }
        int primeraHora = 23;
        int ultimaHora = 0;
        for (Cancha cancha : canchas) {
            primeraHora = Math.min(primeraHora, cancha.getHoraApertura().getHour());
            ultimaHora = Math.max(ultimaHora, cancha.getHoraCierre().getHour());
        }
        int horas = ultimaHora - primeraHora;

        // Encabezado con el nombre y el horario de cada cancha
        Region esquina = new Region();
        esquina.setMinWidth(ANCHO_HORAS);
        encabezadoCanchas.getChildren().add(esquina);
        for (Cancha cancha : canchas) {
            VBox titulo = new VBox(0, etiquetaClase(cancha.toString(), "cancha-titulo"),
                    etiquetaClase(cancha.getDisponibilidad() ? cancha.getHoraApertura() + " a " + cancha.getHoraCierre()
                            : "Inhabilitada", "dato-aclaracion"));
            titulo.setAlignment(Pos.CENTER);
            titulo.setMinWidth(ANCHO_CANCHA);
            titulo.setPrefWidth(ANCHO_CANCHA);
            titulo.getStyleClass().add("cancha-encabezado");
            encabezadoCanchas.getChildren().add(titulo);
        }

        // Grilla: horas a la izquierda y una celda por cancha y hora
        for (int h = 0; h <= horas; h++) {
            Label hora = etiquetaClase(String.format("%02d:00", primeraHora + h), "hora-calendario");
            hora.relocate(4, h * ALTO_HORA - 8);
            calendario.getChildren().add(hora);
        }
        // Una celda por cancha y por media hora: así se pueden programar partidos a las y media
        for (int c = 0; c < canchas.size(); c++) {
            Cancha cancha = canchas.get(c);
            for (int m = 0; m < horas * 2; m++) {
                LocalTime inicio = LocalTime.of(primeraHora, 0).plusMinutes(30L * m);
                boolean abierta = cancha.getDisponibilidad() && !inicio.isBefore(cancha.getHoraApertura())
                        && inicio.isBefore(cancha.getHoraCierre());
                Region celda = new Region();
                celda.getStyleClass().add(abierta ? "celda" : "celda-cerrada");
                if (m % 2 == 1) {
                    celda.getStyleClass().add("celda-media-hora");
                }
                celda.setPrefSize(ANCHO_CANCHA, ALTO_HORA / 2);
                celda.relocate(ANCHO_HORAS + c * ANCHO_CANCHA, m * ALTO_HORA / 2);
                if (abierta) {
                    celda.setOnMouseClicked(e -> asignar(cancha, inicio));
                    Tooltip.install(celda, new Tooltip(cancha + " · " + inicio + " hs"));
                }
                calendario.getChildren().add(celda);
            }
        }

        // Partidos del día (de todos los torneos, porque las canchas son compartidas)
        Torneo torneoElegido = cmbTorneo.getValue();
        for (Partido partido : gestion.consultarPartidosDelDia(dia)) {
            int c = canchas.indexOf(partido.getCancha());
            if (c < 0) {
                continue;
            }
            Torneo torneo = gestion.buscarTorneo(partido);
            double y = (partido.getHorario().getHour() - primeraHora + partido.getHorario().getMinute() / 60.0) * ALTO_HORA;
            int duracion = gestion.duracionDe(partido);
            double alto = duracion / 60.0 * ALTO_HORA - 6;
            VBox bloque = new VBox(0,
                    etiquetaClase(partido.getHorario() + " a " + partido.getHorario().plusMinutes(duracion) + " · "
                            + partido.getInstancia(), "bloque-instancia"),
                    etiquetaClase(partido.getNombreParticipante1(), "bloque-texto"),
                    etiquetaClase("vs. " + partido.getNombreParticipante2(), "bloque-texto"),
                    etiquetaClase(partido.estaFinalizado() ? "Resultado: " + partido.getResultado().getMarcador()
                            : torneo.getNombre(), "bloque-detalle"));
            bloque.getStyleClass().add("bloque");
            if (partido.estaFinalizado()) {
                bloque.getStyleClass().add("bloque-finalizado");
            }
            if (torneoElegido != null && torneo != torneoElegido) {
                bloque.getStyleClass().add("bloque-otro-torneo");
            }
            if (partido == seleccionado) {
                bloque.getStyleClass().add("bloque-seleccionado");
            }
            bloque.setPrefSize(ANCHO_CANCHA - 8, alto);
            bloque.setMaxSize(ANCHO_CANCHA - 8, alto);
            bloque.setMinHeight(alto);
            bloque.relocate(ANCHO_HORAS + c * ANCHO_CANCHA + 4, y + 3);
            bloque.setOnMouseClicked(e -> {
                seleccionado = partido;
                if (torneo != cmbTorneo.getValue()) {
                    cmbTorneo.setValue(torneo); // también recarga la lista de pendientes
                    seleccionado = partido;
                }
                listaPendientes.getSelectionModel().clearSelection();
                mostrarSeleccion();
                dibujarCalendario();
            });
            calendario.getChildren().add(bloque);
        }
        calendario.setPrefSize(ANCHO_HORAS + canchas.size() * ANCHO_CANCHA, horas * ALTO_HORA + 10);
        calendario.setMinSize(ANCHO_HORAS + canchas.size() * ANCHO_CANCHA, horas * ALTO_HORA + 10);
    }

    /**
     * Tira con todos los días del torneo elegido: cada día indica cuántos partidos
     * tiene programados y, al tocarlo, el calendario va a ese día.
     */
    private void actualizarDiasTorneo() {
        diasTorneo.getChildren().clear();
        Torneo torneo = cmbTorneo.getValue();
        if (torneo == null) {
            lblPeriodo.setText("");
            return;
        }
        int totalProgramados = 0;
        for (LocalDate d = torneo.getFechaInicio(); !d.isAfter(torneo.getFechaFin()); d = d.plusDays(1)) {
            LocalDate diaBoton = d;
            long partidos = torneo.getPartidos().stream()
                    .filter(p -> p.estaProgramado() && p.getFecha().equals(diaBoton)).count();
            totalProgramados += partidos;
            VBox boton = new VBox(0,
                    etiquetaClase(primeraMayuscula(d.format(DIA_CORTO).replace(".", "")), "dia-chip-fecha"),
                    etiquetaClase(partidos == 0 ? "-" : cantidad(partidos, "partido", "partidos"), "dia-chip-cantidad"));
            boton.setAlignment(Pos.CENTER);
            boton.getStyleClass().add("dia-chip");
            if (partidos > 0) {
                boton.getStyleClass().add("dia-chip-con-partidos");
            }
            if (d.equals(dia)) {
                boton.getStyleClass().add("dia-chip-activo");
            }
            boton.setOnMouseClicked(e -> irA(diaBoton));
            diasTorneo.getChildren().add(boton);
        }
        lblPeriodo.setText(formatear(torneo.getFechaInicio()) + " al " + formatear(torneo.getFechaFin()) + " · "
                + cantidad(totalProgramados, "partido programado", "partidos programados"));
    }

    /** Día que conviene mostrar para un torneo: el de su próximo partido sin jugar. */
    private LocalDate diaSugerido(Torneo torneo) {
        if (torneo == null) {
            return LocalDate.now();
        }
        List<Partido> programados = gestion.consultarProgramacion(torneo);
        for (Partido partido : programados) {
            if (!partido.estaFinalizado()) {
                return partido.getFecha();
            }
        }
        if (!programados.isEmpty()) {
            return programados.get(programados.size() - 1).getFecha();
        }
        LocalDate hoy = LocalDate.now();
        boolean hoyEnElTorneo = !hoy.isBefore(torneo.getFechaInicio()) && !hoy.isAfter(torneo.getFechaFin());
        return hoyEnElTorneo ? hoy : torneo.getFechaInicio();
    }

    // ------------------------------------------------------------ acciones

    /** CU11: asigna al partido elegido la cancha y el horario tocados en el calendario. */
    private void asignar(Cancha cancha, LocalTime horario) {
        if (seleccionado == null) {
            mostrarInfo("Programación", "Seleccioná un partido.");
            return;
        }
        if (seleccionado.estaFinalizado()) {
            mostrarError("El partido elegido ya se jugó: no se puede reprogramar.");
            return;
        }
        Torneo torneo = gestion.buscarTorneo(seleccionado);
        try {
            try {
                gestion.programarPartido(torneo, seleccionado, cancha, dia, horario);
            } catch (DescansoInsuficienteException e) {
                // Falta descanso: el organizador puede decidir programarlo igual (por ejemplo, a pedido del jugador)
                if (!confirmar("Descanso insuficiente", e.getMessage() + "\n\n¿Programar igual?")) {
                    return;
                }
                gestion.programarPartido(torneo, seleccionado, cancha, dia, horario, true);
            }
            cargarPendientes();
            mostrarSeleccion();
            dibujarCalendario();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    /** CU11: programa todos los partidos pendientes del torneo elegido. */
    private void programarAutomatico() {
        Torneo torneo = cmbTorneo.getValue();
        if (torneo == null) {
            mostrarError("Seleccioná un torneo.");
            return;
        }
        try {
            int cantidad = gestion.programarPartidos(torneo);
            if (cantidad == 0) {
                mostrarInfo("Programación", "No hay partidos pendientes.");
            } else {
                mostrarInfo("Programación generada", cantidad(cantidad, "partido programado", "partidos programados") + ".");
                Partido primero = gestion.consultarProgramacion(torneo).stream()
                        .filter(p -> !p.estaFinalizado()).findFirst().orElse(null);
                if (primero != null) {
                    dia = primero.getFecha();
                }
            }
            cargarPendientes();
            dibujarCalendario();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
            cargarPendientes();
            dibujarCalendario();
        }
    }

    private void quitarProgramacion() {
        try {
            gestion.quitarProgramacion(seleccionado);
            cargarPendientes();
            mostrarSeleccion();
            dibujarCalendario();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    /**
     * CU13: carga el resultado del partido elegido (o lo corrige). Si los datos no
     * son válidos se informa el error y el formulario vuelve a mostrarse con lo ingresado.
     */
    private void cargarResultado(boolean modificar) {
        Partido partido = seleccionado;
        Torneo torneo = gestion.buscarTorneo(partido);
        TextField txtMarcador = texto(torneo.getFormatoResultado().getEjemplo());
        CheckBox chkWO = new CheckBox("W.O.");
        CheckBox chkRetiro = new CheckBox("Retiro");
        ComboBox<Participante> cmbGanador = combo("Seleccionar");
        cmbGanador.setItems(FXCollections.observableArrayList(partido.getParticipante1(), partido.getParticipante2()));
        if (modificar && partido.getResultado() != null) {
            txtMarcador.setText(partido.getResultado().isEsWO() ? "" : partido.getResultado().getMarcador());
            chkWO.setSelected(partido.getResultado().isEsWO());
            chkRetiro.setSelected(partido.getResultado().isEsRetiro());
            cmbGanador.setValue(partido.getResultado().getGanador());
        }
        txtMarcador.disableProperty().bind(chkWO.selectedProperty());

        boolean listo = false;
        while (!listo) {
            Dialog<ButtonType> dialogo = new Dialog<>();
            dialogo.setTitle("Primer Punto");
            dialogo.setHeaderText((modificar ? "Modificar resultado: " : "Cargar resultado: ") + partido);
            Label ayuda = etiquetaClase(torneo.getFormatoResultado() + " · marcador de "
                    + partido.getNombreParticipante1(), "dato-aclaracion");
            VBox contenido = new VBox(14, ayuda, campo("Marcador", txtMarcador), chkWO, chkRetiro,
                    campo("Ganador (W.O. o retiro)", cmbGanador));
            contenido.setPadding(new Insets(10, 0, 0, 0));
            dialogo.getDialogPane().setContent(contenido);
            dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            prepararDialogo(dialogo);
            Optional<ButtonType> respuesta = dialogo.showAndWait();
            if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
                return;
            }
            try {
                if (modificar) {
                    gestion.modificarResultado(torneo, partido, txtMarcador.getText(), cmbGanador.getValue(),
                            chkWO.isSelected(), chkRetiro.isSelected());
                } else {
                    gestion.registrarResultado(torneo, partido, txtMarcador.getText(), cmbGanador.getValue(),
                            chkWO.isSelected(), chkRetiro.isSelected());
                }
                listo = true;
                mostrarInfo("Resultado registrado", "Ganó " + partido.getGanador().obtenerNombreCompleto() + " ("
                        + partido.getResultado().getMarcador() + ").");
            } catch (PrimerPuntoException e) {
                mostrarError(e.getMessage()); // se vuelve a mostrar el formulario con los datos cargados
            }
        }
        mostrarSeleccion();
        dibujarCalendario();
    }

    /** CU02: registra una cancha nueva. */
    private void registrarCancha() {
        try {
            int numero = leerEntero(txtNumeroCancha, "Número");
            gestion.registrarCancha(numero, cmbApertura.getValue(), cmbCierre.getValue());
            txtNumeroCancha.clear();
            cargarCanchas();
            dibujarCalendario();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    /** CU02, flujo alternativo 1: modifica la cancha seleccionada. */
    private void modificarCancha() {
        Cancha cancha = tablaCanchas.getSelectionModel().getSelectedItem();
        if (cancha == null) {
            mostrarError("Seleccioná una cancha de la lista.");
            return;
        }
        try {
            gestion.modificarCancha(cancha, leerEntero(txtNumeroCancha, "Número"), cmbApertura.getValue(),
                    cmbCierre.getValue());
            mostrarInfo("Cancha modificada", "Se actualizaron los datos de la " + cancha + ".");
            cargarCanchas();
            dibujarCalendario();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cambiarDisponibilidad() {
        Cancha cancha = tablaCanchas.getSelectionModel().getSelectedItem();
        if (cancha == null) {
            mostrarError("Seleccioná una cancha de la lista.");
            return;
        }
        cancha.setDisponibilidad(!cancha.getDisponibilidad());
        cargarCanchas();
        dibujarCalendario();
    }

    // ------------------------------------------------------------ datos en pantalla

    @Override
    public void actualizar() {
        Torneo elegido = cmbTorneo.getValue();
        List<Torneo> conPartidos = new ArrayList<>();
        for (Torneo torneo : sistema.getGestionTorneos().listarTorneos(null)) {
            if (!torneo.getPartidos().isEmpty() && torneo.getEstado() != EstadoTorneo.FINALIZADO) {
                conPartidos.add(torneo);
            }
        }
        cmbTorneo.setItems(FXCollections.observableArrayList(conPartidos));
        if (elegido != null && conPartidos.contains(elegido)) {
            cmbTorneo.setValue(elegido);
        } else {
            cmbTorneo.getSelectionModel().selectFirst();
        }
        cargarCanchas();
        cargarPendientes();
        mostrarSeleccion();
        dibujarCalendario();
    }

    private void cargarCanchas() {
        Cancha elegida = tablaCanchas.getSelectionModel().getSelectedItem();
        tablaCanchas.setItems(FXCollections.observableArrayList(gestion.listarCanchas()));
        tablaCanchas.refresh();
        if (elegida != null) {
            tablaCanchas.getSelectionModel().select(elegida);
        }
    }

    private void cargarPendientes() {
        Torneo torneo = cmbTorneo.getValue();
        List<Partido> pendientes = new ArrayList<>();
        if (torneo != null) {
            for (Partido partido : torneo.getPartidos()) {
                if (!partido.estaProgramado() && !partido.estaFinalizado()) {
                    pendientes.add(partido);
                }
            }
        }
        listaPendientes.setItems(FXCollections.observableArrayList(pendientes));
        listaPendientes.setPlaceholder(etiquetaClase(torneo == null ? "No hay torneos con partidos."
                : "Sin partidos pendientes", "mensaje-vacio"));
    }

    /** Panel verde con el partido elegido y las acciones que admite según su situación. */
    private void mostrarSeleccion() {
        panelSeleccion.getChildren().clear();
        panelSeleccion.getChildren().add(etiquetaClase("Partido seleccionado", "tarjeta-titulo"));
        if (seleccionado == null) {
            panelSeleccion.getChildren().add(etiquetaClase("Ninguno", "dato-aclaracion"));
            return;
        }
        Partido p = seleccionado;
        panelSeleccion.getChildren().addAll(
                etiquetaClase(p.getInstancia(), "dato-aclaracion"),
                etiquetaClase(p.getNombreParticipante1(), "dato-seleccion"),
                etiquetaClase("vs. " + p.getNombreParticipante2(), "dato-seleccion"),
                etiquetaClase(p.estaProgramado() ? p.getCancha() + " · " + formatear(p.getFecha()) + " · "
                        + p.getHorario() + " hs" : "Sin programar", "dato-aclaracion"));
        if (p.estaFinalizado()) {
            panelSeleccion.getChildren().addAll(etiquetaClase("Resultado: " + p.getResultado().getMarcador(),
                    "dato-seleccion"), botonBlanco("Modificar resultado", () -> cargarResultado(true)));
        } else if (p.estaProgramado()) {
            if (p.estaDefinido()) {
                panelSeleccion.getChildren().add(botonBlanco("Cargar resultado", () -> cargarResultado(false)));
            }
            panelSeleccion.getChildren().add(botonBlanco("Quitar programación", this::quitarProgramacion));
        }
    }

    private Button botonBlanco(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("boton-blanco");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(e -> accion.run());
        return boton;
    }

    private String primeraMayuscula(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
