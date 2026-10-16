package primerpunto.vista;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Partido;
import primerpunto.modelo.TipoFormato;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;
import primerpunto.negocio.GestionCuadrosZonas;
import primerpunto.negocio.Sistema;

/**
 * Ventana del módulo Formato (cuadros y zonas): generar el formato de competencia
 * de un torneo (CU10), modificarlo mientras el torneo no haya comenzado, y ver el
 * cuadro de eliminación y las tablas de posiciones de las zonas (CU12 y CU14).
 */
public class VentanaCuadroZona extends VistaBase {

    private final GestionCuadrosZonas gestion;

    private final ComboBox<Torneo> cmbTorneo = combo("Seleccionar");
    private final ComboBox<TipoFormato> cmbFormato = combo("Formato");
    private final ComboBox<Integer> cmbZonas = combo("Zonas");
    private final VBox campoZonas = campo("Zonas", cmbZonas);
    private final CheckBox chkSortear = new CheckBox("Sorteo");
    private final Button btnGenerar = botonPrimario("Generar formato", e -> generar());
    private final Button btnModificar = botonSecundario("Modificar formato", e -> modificar());
    private final Button btnCuadroFinal = botonSecundario("Cuadro final desde las zonas", e -> generarCuadroFinal());
    private final HBox resumen = new HBox(10);
    private final VBox areaFormato = tarjeta(null);

    public VentanaCuadroZona(Sistema sistema) {
        super(sistema, "Formato");
        this.gestion = sistema.getGestionCuadrosZonas();
        getChildren().addAll(crearBarra(), areaFormato);
    }

    // ------------------------------------------------------------ armado de la pantalla

    /** Barra compacta: torneo, formato y acciones en una sola franja, con un resumen debajo. */
    private VBox crearBarra() {
        cmbTorneo.setOnAction(e -> mostrarTorneo());
        cmbTorneo.setPrefWidth(280);
        cmbFormato.setItems(FXCollections.observableArrayList(TipoFormato.values()));
        cmbFormato.getSelectionModel().selectFirst();
        cmbFormato.setPrefWidth(330);
        cmbFormato.setOnAction(e -> mostrarNodo(campoZonas, cmbFormato.getValue() != TipoFormato.CUADRO));
        cmbZonas.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
        cmbZonas.setValue(2);
        cmbZonas.setPrefWidth(110);
        mostrarNodo(campoZonas, false);
        chkSortear.setSelected(true);

        VBox campoTorneo = campo("Torneo", cmbTorneo);
        VBox campoFormato = campo("Formato", cmbFormato);
        for (VBox caja : List.of(campoTorneo, campoFormato, campoZonas)) {
            HBox.setHgrow(caja, javafx.scene.layout.Priority.NEVER);
        }
        HBox controles = new HBox(16, campoTorneo, campoFormato, campoZonas, chkSortear, espacio(),
                btnGenerar, btnModificar, btnCuadroFinal);
        controles.setAlignment(Pos.BOTTOM_LEFT);
        chkSortear.setPadding(new Insets(0, 0, 14, 0));
        resumen.setAlignment(Pos.CENTER_LEFT);

        VBox barra = new VBox(14, controles, resumen);
        barra.getStyleClass().add("barra");
        return barra;
    }

    // ------------------------------------------------------------ acciones

    /** CU10: genera el cuadro, las zonas o ambos, según la opción elegida. */
    private void generar() {
        Torneo torneo = cmbTorneo.getValue();
        if (torneo == null) {
            mostrarError("Seleccioná un torneo.");
            return;
        }
        try {
            if (!chkSortear.isSelected()) {
                generarAMano(torneo);
                return;
            }
            if (cmbFormato.getValue() == TipoFormato.CUADRO) {
                List<Participante> conBye = new ArrayList<>();
                if (gestion.tieneCantidadImpar(torneo) && torneo.getFormatos().isEmpty()) {
                    // CU10, flujo alternativo 1: cantidad impar de inscriptos
                    Optional<List<Participante>> eleccion = elegirByes(torneo);
                    if (eleccion.isEmpty()) {
                        return;
                    }
                    conBye = eleccion.get();
                }
                Cuadro cuadro = gestion.generarCuadro(torneo, conBye, chkSortear.isSelected());
                mostrarInfo("Formato generado", "Cuadro de " + cuadro.getTamanio() + ", "
                        + cantidad(cuadro.getCantidadByes(), "bye", "byes") + ", "
                        + cantidad(cuadro.getPartidos().size(), "partido", "partidos") + ".");
            } else {
                List<Zona> zonas = gestion.generarZonas(torneo, cmbZonas.getValue(), chkSortear.isSelected());
                mostrarInfo("Formato generado", cantidad(zonas.size(), "zona", "zonas") + " de todos contra todos.");
            }
            mostrarTorneo();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    /** Sin sorteo: el organizador arma a mano los cruces del cuadro o el reparto de las zonas. */
    private void generarAMano(Torneo torneo) throws PrimerPuntoException {
        if (torneo.getParticipantesHabilitados().size() < 2) {
            throw new primerpunto.excepciones.DatoInvalidoException("No hay inscriptos suficientes para generar el formato.");
        }
        if (cmbFormato.getValue() == TipoFormato.CUADRO) {
            Optional<List<Participante>> orden = armarCuadroAMano(torneo);
            if (orden.isPresent()) {
                Cuadro cuadro = gestion.generarCuadroManual(torneo, orden.get());
                mostrarInfo("Formato generado", "Cuadro de " + cuadro.getTamanio() + ".");
            }
        } else {
            Optional<List<List<Participante>>> reparto = armarZonasAMano(torneo, cmbZonas.getValue());
            if (reparto.isPresent()) {
                List<Zona> zonas = gestion.generarZonasManual(torneo, reparto.get());
                mostrarInfo("Formato generado", cantidad(zonas.size(), "zona", "zonas") + ".");
            }
        }
        mostrarTorneo();
    }

    /**
     * Ventana para ordenar a mano los lugares del cuadro: con «Subir» y «Bajar» se cambia
     * el orden, y a la derecha se ve en vivo cómo quedan los cruces y quién recibe bye.
     */
    private Optional<List<Participante>> armarCuadroAMano(Torneo torneo) {
        List<Participante> habilitados = torneo.getParticipantesHabilitados();
        int byes = new Cuadro(0).calcularByes(habilitados.size());
        ListView<Participante> lista = new ListView<>(FXCollections.observableArrayList(habilitados));
        lista.setPrefSize(300, 340);
        VBox vistaPrevia = new VBox(6);
        Runnable actualizarVista = () -> {
            vistaPrevia.getChildren().clear();
            List<Participante> orden = lista.getItems();
            int siguiente = 0;
            int llaves = (orden.size() + byes) / 2;
            for (int i = 0; i < llaves; i++) {
                String texto = i < byes
                        ? (i + 1) + ".  " + orden.get(siguiente++).obtenerNombreCompleto() + "  (bye)"
                        : (i + 1) + ".  " + orden.get(siguiente++).obtenerNombreCompleto() + "  vs.  "
                        + orden.get(siguiente++).obtenerNombreCompleto();
                vistaPrevia.getChildren().add(etiquetaClase(texto, "texto-partido"));
            }
        };
        actualizarVista.run();
        Button subir = botonSecundario("Subir", e -> mover(lista, -1, actualizarVista));
        Button bajar = botonSecundario("Bajar", e -> mover(lista, 1, actualizarVista));

        VBox izquierda = new VBox(10, etiquetaClase("ORDEN", "etiqueta"), lista, new HBox(10, subir, bajar));
        VBox derecha = new VBox(10, etiquetaClase("PRIMERA RONDA", "etiqueta"), vistaPrevia);
        derecha.setPrefWidth(380);
        Label ayuda = etiquetaClase(byes > 0 ? "Los primeros " + byes + " de la lista pasan con bye." : "",
                "dato-aclaracion");
        VBox contenido = new VBox(16, ayuda, new HBox(26, izquierda, derecha));
        contenido.setPadding(new Insets(10, 0, 0, 0));

        Dialog<List<Participante>> dialogo = new Dialog<>();
        dialogo.setTitle("Primer Punto");
        dialogo.setHeaderText("Armar cuadro");
        dialogo.getDialogPane().setContent(contenido);
        ButtonType generar = new ButtonType("Generar cuadro", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(generar, ButtonType.CANCEL);
        dialogo.setResultConverter(boton -> boton == generar ? new ArrayList<>(lista.getItems()) : null);
        prepararDialogo(dialogo);
        return dialogo.showAndWait();
    }

    private void mover(ListView<Participante> lista, int desplazamiento, Runnable alMover) {
        int i = lista.getSelectionModel().getSelectedIndex();
        int destino = i + desplazamiento;
        if (i < 0 || destino < 0 || destino >= lista.getItems().size()) {
            return;
        }
        Participante elegido = lista.getItems().remove(i);
        lista.getItems().add(destino, elegido);
        lista.getSelectionModel().select(destino);
        alMover.run();
    }

    /** Ventana para elegir en qué zona juega cada participante. */
    private Optional<List<List<Participante>>> armarZonasAMano(Torneo torneo, int cantidadZonas) {
        List<Participante> habilitados = torneo.getParticipantesHabilitados();
        javafx.scene.layout.GridPane grilla = new javafx.scene.layout.GridPane();
        grilla.setHgap(18);
        grilla.setVgap(8);
        List<ComboBox<Integer>> zonasElegidas = new ArrayList<>();
        Label resumenZonas = etiquetaClase("", "dato-aclaracion");
        Runnable actualizarResumen = () -> {
            StringBuilder texto = new StringBuilder();
            for (int z = 1; z <= cantidadZonas; z++) {
                final int zona = z;
                long cantidad = zonasElegidas.stream().filter(c -> c.getValue() == zona).count();
                texto.append(z > 1 ? "   ·   " : "").append("Zona ").append(z).append(": ").append(cantidad);
            }
            resumenZonas.setText(texto.toString());
        };
        for (int i = 0; i < habilitados.size(); i++) {
            ComboBox<Integer> cmbZona = new ComboBox<>();
            for (int z = 1; z <= cantidadZonas; z++) {
                cmbZona.getItems().add(z);
            }
            cmbZona.setValue(i % cantidadZonas + 1); // propuesta inicial: repartidos de a uno
            cmbZona.setOnAction(e -> actualizarResumen.run());
            zonasElegidas.add(cmbZona);
            grilla.addRow(i, etiquetaClase(habilitados.get(i).obtenerNombreCompleto(), "texto-partido"), cmbZona);
        }
        actualizarResumen.run();
        javafx.scene.control.ScrollPane desplazable = new javafx.scene.control.ScrollPane(grilla);
        desplazable.setPrefHeight(Math.min(380, 40 + 38 * habilitados.size()));
        desplazable.setFitToWidth(true);
        VBox contenido = new VBox(14, desplazable, resumenZonas);
        contenido.setPadding(new Insets(10, 0, 0, 0));

        Dialog<List<List<Participante>>> dialogo = new Dialog<>();
        dialogo.setTitle("Primer Punto");
        dialogo.setHeaderText("Armar zonas");
        dialogo.getDialogPane().setContent(contenido);
        ButtonType generar = new ButtonType("Generar zonas", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(generar, ButtonType.CANCEL);
        dialogo.setResultConverter(boton -> {
            if (boton != generar) {
                return null;
            }
            List<List<Participante>> reparto = new ArrayList<>();
            for (int z = 0; z < cantidadZonas; z++) {
                reparto.add(new ArrayList<>());
            }
            for (int i = 0; i < habilitados.size(); i++) {
                reparto.get(zonasElegidas.get(i).getValue() - 1).add(habilitados.get(i));
            }
            return reparto;
        });
        prepararDialogo(dialogo);
        return dialogo.showAndWait();
    }

    /** Descarta el formato actual para generar otro (solo si el torneo no comenzó). */
    private void modificar() {
        Torneo torneo = cmbTorneo.getValue();
        if (torneo == null) {
            return;
        }
        if (!confirmar("Modificar formato", "Se elimina el formato actual de " + torneo.getNombre()
                + " y la programación de sus partidos. ¿Continuar?")) {
            return;
        }
        try {
            gestion.eliminarFormato(torneo);
            mostrarTorneo();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    /**
     * Con cantidad impar, el organizador elige quiénes pasan directo a la segunda
     * ronda (bye) o deja que se defina por sorteo.
     */
    private Optional<List<Participante>> elegirByes(Torneo torneo) {
        List<Participante> habilitados = torneo.getParticipantesHabilitados();
        int byes = new Cuadro(0).calcularByes(habilitados.size());
        ListView<Participante> lista = new ListView<>(FXCollections.observableArrayList(habilitados));
        lista.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lista.setPrefHeight(220);
        Label aviso = new Label(habilitados.size() + " inscriptos: el cuadro lleva " + cantidad(byes, "bye", "byes")
                + ". Elegí quién pasa directo a la segunda ronda o usá el sorteo.");
        aviso.setWrapText(true);

        Dialog<List<Participante>> dialogo = new Dialog<>();
        dialogo.setTitle("Primer Punto");
        dialogo.setHeaderText("Cantidad impar de inscriptos");
        VBox contenido = new VBox(12, aviso, lista);
        contenido.setPadding(new Insets(10, 0, 0, 0));
        dialogo.getDialogPane().setContent(contenido);
        ButtonType elegidos = new ButtonType("Confirmar");
        ButtonType sorteo = new ButtonType("Sortear");
        dialogo.getDialogPane().getButtonTypes().addAll(elegidos, sorteo, ButtonType.CANCEL);
        dialogo.setResultConverter(boton -> {
            if (boton == elegidos) {
                return new ArrayList<>(lista.getSelectionModel().getSelectedItems());
            }
            if (boton == sorteo) {
                return new ArrayList<>();
            }
            return null;
        });
        prepararDialogo(dialogo);
        return dialogo.showAndWait();
    }

    /** Para el formato "zonas y luego cuadro": arma el cuadro con los clasificados de cada zona. */
    private void generarCuadroFinal() {
        Torneo torneo = cmbTorneo.getValue();
        if (torneo == null) {
            mostrarError("Seleccioná un torneo.");
            return;
        }
        ComboBox<Integer> cmbClasificados = combo("Clasificados");
        cmbClasificados.setItems(FXCollections.observableArrayList(1, 2));
        cmbClasificados.setValue(2);
        Dialog<Integer> dialogo = new Dialog<>();
        dialogo.setTitle("Primer Punto");
        dialogo.setHeaderText("Generar cuadro final");
        dialogo.getDialogPane().setContent(campo("Clasificados por zona", cmbClasificados));
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.setResultConverter(boton -> boton == ButtonType.OK ? cmbClasificados.getValue() : null);
        prepararDialogo(dialogo);
        Optional<Integer> clasificados = dialogo.showAndWait();
        if (clasificados.isEmpty()) {
            return;
        }
        try {
            Cuadro cuadro = gestion.generarCuadroDesdeZonas(torneo, clasificados.get());
            mostrarInfo("Cuadro final generado", "Cuadro de " + cuadro.getTamanio() + ".");
            mostrarTorneo();
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    // ------------------------------------------------------------ datos en pantalla

    @Override
    public void actualizar() {
        // Los torneos finalizados se consultan en el Historial
        Torneo seleccionado = cmbTorneo.getValue();
        List<Torneo> torneos = new ArrayList<>();
        for (Torneo torneo : sistema.getGestionTorneos().listarTorneos(null)) {
            if (torneo.getEstado() != EstadoTorneo.FINALIZADO) {
                torneos.add(torneo);
            }
        }
        cmbTorneo.setItems(FXCollections.observableArrayList(torneos));
        if (seleccionado != null && torneos.contains(seleccionado)) {
            cmbTorneo.setValue(seleccionado);
        } else {
            torneos.stream().filter(t -> gestion.obtenerCuadro(t) != null).findFirst()
                    .ifPresentOrElse(cmbTorneo::setValue, () -> cmbTorneo.getSelectionModel().selectFirst());
        }
        mostrarTorneo();
    }

    private void mostrarTorneo() {
        Torneo torneo = cmbTorneo.getValue();
        areaFormato.getChildren().clear();
        resumen.getChildren().clear();
        if (torneo == null) {
            areaFormato.getChildren().add(etiquetaClase("No hay torneos activos.", "mensaje-vacio"));
            return;
        }
        Cuadro cuadro = gestion.obtenerCuadro(torneo);
        List<Zona> zonas = gestion.obtenerZonas(torneo);
        boolean tieneFormato = cuadro != null || !zonas.isEmpty();
        boolean comenzo = torneo.getPartidos().stream().anyMatch(Partido::estaFinalizado);
        boolean zonasTerminadas = !zonas.isEmpty() && zonas.stream().allMatch(Zona::estaCompleto);

        // Botones según la situación del torneo
        mostrarNodo(btnGenerar, !tieneFormato);
        mostrarNodo(btnModificar, tieneFormato && !comenzo);
        mostrarNodo(btnCuadroFinal, !zonas.isEmpty() && cuadro == null);
        btnCuadroFinal.setDisable(!zonasTerminadas);

        // Resumen en una línea de "pastillas"
        resumen.getChildren().addAll(
                chip(cantidad(torneo.getParticipantesHabilitados().size(), "participante", "participantes"), "chip-verde"),
                chip(torneo.getEstado().toString(), "chip-gris"));
        if (cuadro != null) {
            resumen.getChildren().addAll(chip(cuadro.getDescripcion(), "chip-gris"),
                    chip(cantidad(cuadro.getCantidadRondas(), "ronda", "rondas"), "chip-gris"),
                    chip(cantidad(cuadro.getCantidadByes(), "bye", "byes"), "chip-gris"));
            if (cuadro.getCampeon() != null) {
                resumen.getChildren().add(chip("Campeón: " + cuadro.getCampeon().obtenerNombreCompleto(), "chip-verde"));
            }
        }
        if (!zonas.isEmpty()) {
            resumen.getChildren().add(chip(cantidad(zonas.size(), "zona", "zonas"), "chip-gris"));
        }

        areaFormato.getChildren().add(etiquetaClase(torneo.getNombre(), "tarjeta-titulo"));
        if (!tieneFormato) {
            areaFormato.getChildren().add(etiquetaClase(torneo.getEstado() == EstadoTorneo.INSCRIPCION_ABIERTA
                    ? "Inscripción abierta." : "Sin formato generado.", "mensaje-vacio"));
        } else {
            areaFormato.getChildren().add(new PanelFormato(this, gestion, torneo));
        }
    }

    private Label chip(String texto, String color) {
        Label chip = new Label(texto);
        chip.getStyleClass().addAll("chip", "chip-grande", color);
        return chip;
    }
}
