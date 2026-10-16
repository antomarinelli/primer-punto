package primerpunto.vista;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.EstadoTorneo;
import primerpunto.modelo.FormatoResultado;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.GestionTorneos;
import primerpunto.negocio.Sistema;

/**
 * Ventana del módulo Torneos: configurar un torneo nuevo (CU03), consultar, filtrar,
 * ordenar y buscar torneos (CU04) y ver la ficha completa del torneo elegido, desde
 * donde se gestionan sus inscriptos (CU06, CU08 y CU09) y se finaliza (CU15).
 */
public class VentanaTorneos extends VistaBase {

    private static final String ACTIVOS = "Torneos activos";
    private static final String TODOS = "Todos los torneos";

    private final GestionTorneos gestion;

    // Indicadores
    private final HBox filaIndicadores = new HBox(18);
    private final Label lblAbiertos;
    private final Label lblPorComenzar;
    private final Label lblEnCurso;
    private final Label lblFinalizados;

    // Formulario de torneo nuevo (se despliega con el botón "Nuevo torneo")
    private final TextField txtNombre = texto("");
    private final ComboBox<Modalidad> cmbModalidad = combo("Seleccionar");
    private final TextField txtCosto = texto("$");
    private final TextField txtLugar = texto("");
    private final DatePicker dpInicio = fecha();
    private final DatePicker dpFin = fecha();
    private final DatePicker dpPlazo = fecha();
    private final ComboBox<Integer> cmbDuracion = combo("Duración");
    private final ComboBox<FormatoResultado> cmbFormatoResultado = combo("Formato de resultado");
    private final Button btnNuevo = botonPrimario("+  Nuevo torneo", null);
    private final VBox formulario = new VBox(18);

    // Listado
    private final ComboBox<String> cmbFiltro = combo("Estado");
    private final DatePicker dpBuscar = fecha();
    private final TableView<Torneo> tabla = tabla("No hay torneos para mostrar.");
    private boolean ordenarPorFecha;

    // Ficha del torneo elegido
    private final VBox contenedorFicha = tarjeta(null);

    public VentanaTorneos(Sistema sistema) {
        super(sistema, "Torneos");
        this.gestion = sistema.getGestionTorneos();
        lblAbiertos = indicador(filaIndicadores, "Inscripción abierta", true);
        lblPorComenzar = indicador(filaIndicadores, "Por comenzar", false);
        lblEnCurso = indicador(filaIndicadores, "En curso", false);
        lblFinalizados = indicador(filaIndicadores, "Finalizados", false);
        getChildren().addAll(filaIndicadores, crearListado(), contenedorFicha);
    }

    // ------------------------------------------------------------ armado de la pantalla

    private VBox crearListado() {
        cmbFiltro.setItems(FXCollections.observableArrayList(ACTIVOS, TODOS,
                EstadoTorneo.INSCRIPCION_ABIERTA.toString(), EstadoTorneo.INSCRIPCION_CERRADA.toString(),
                EstadoTorneo.EN_CURSO.toString(), EstadoTorneo.FINALIZADO.toString()));
        cmbFiltro.setValue(ACTIVOS);
        cmbFiltro.setOnAction(e -> {
            ordenarPorFecha = false;
            cargarTabla();
        });
        cmbFiltro.setPrefWidth(220);
        dpBuscar.setPrefWidth(175);

        tabla.getColumns().addAll(List.of(
                columna("Torneo", Torneo::getNombre, 220),
                columna("Modalidad", t -> t.getModalidad().toString(), 100),
                columna("Inicio", t -> formatear(t.getFechaInicio()), 100),
                columna("Fin", t -> formatear(t.getFechaFin()), 100),
                columna("Plazo de inscripción", t -> formatear(t.getPlazoInscripcion()), 140),
                columna("Inscriptos", t -> String.valueOf(t.getInscripcionesActivas().size()), 90),
                columnaChip("Estado", t -> t.getEstado().toString(), this::colorEstado, 160)));
        tabla.setPrefHeight(250);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, elegido) -> mostrarFicha(elegido));

        crearFormulario();
        HBox herramientas = new HBox(12, cmbFiltro, botonSecundario("Ordenar por fecha", e -> {
            ordenarPorFecha = true;
            cargarTabla();
        }), dpBuscar, botonSecundario("Buscar por fecha", e -> buscarPorFecha()), espacio(), btnNuevo);
        herramientas.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = tarjeta("Torneos");
        tarjeta.getChildren().addAll(herramientas, formulario, tabla);
        return tarjeta;
    }

    /** Formulario de torneo nuevo, oculto hasta que se toca "Nuevo torneo". */
    private void crearFormulario() {
        cmbModalidad.setItems(FXCollections.observableArrayList(Modalidad.values()));
        txtLugar.setText("Club Los Cuervos");
        // Duración promedio del partido: de 45 minutos a 3 horas, de a 15 minutos
        for (int minutos = 45; minutos <= 180; minutos += 15) {
            cmbDuracion.getItems().add(minutos);
        }
        cmbDuracion.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer minutos) {
                return minutos == null ? "" : formatearDuracion(minutos);
            }

            @Override
            public Integer fromString(String texto) {
                return null;
            }
        });
        cmbDuracion.setValue(90);
        cmbFormatoResultado.setItems(FXCollections.observableArrayList(FormatoResultado.values()));
        cmbFormatoResultado.setValue(FormatoResultado.MEJOR_DE_TRES);

        formulario.getStyleClass().add("formulario-desplegable");
        formulario.getChildren().addAll(
                etiquetaClase("Nuevo torneo", "subtitulo-seccion"),
                filaCampos(campo("Nombre del torneo", txtNombre), campo("Modalidad", cmbModalidad),
                        campo("Costo de inscripción ($)", txtCosto), campo("Lugar", txtLugar)),
                filaCampos(campo("Fecha de inicio", dpInicio), campo("Fecha de fin", dpFin),
                        campo("Plazo de inscripción", dpPlazo), new VBox()),
                filaCampos(campo("Duración promedio del partido", cmbDuracion),
                        campo("Formato de resultado", cmbFormatoResultado), new VBox(), new VBox()),
                new HBox(12, botonPrimario("Guardar torneo", e -> guardarTorneo()),
                        botonSecundario("Cancelar", e -> btnNuevo.fire())));
        alternarAlTocar(btnNuevo, formulario, false);
    }

    // ------------------------------------------------------------ acciones

    /** CU03: crea el torneo con los datos del formulario. */
    private void guardarTorneo() {
        try {
            double costo = leerNumero(txtCosto, "Costo de inscripción");
            if (cmbDuracion.getValue() == null) {
                mostrarError("Elegí la duración promedio de los partidos.");
                return;
            }
            Torneo torneo = gestion.crearTorneo(txtNombre.getText(), cmbModalidad.getValue(), costo,
                    dpInicio.getValue(), dpFin.getValue(), dpPlazo.getValue(), txtLugar.getText(),
                    cmbDuracion.getValue(), cmbFormatoResultado.getValue());
            mostrarInfo("Torneo creado", torneo.getNombre() + ". Inscripción abierta hasta el "
                    + formatear(torneo.getPlazoInscripcion()) + ".");
            limpiarFormulario();
            btnNuevo.fire(); // se cierra el formulario
            cmbFiltro.setValue(ACTIVOS);
            actualizar();
            tabla.getSelectionModel().select(torneo);
        } catch (PrimerPuntoException e) {
            mostrarError(e.getMessage());
        }
    }

    /** RF35: búsqueda binaria de los torneos que comienzan en la fecha elegida. */
    private void buscarPorFecha() {
        if (dpBuscar.getValue() == null) {
            mostrarError("Elegí una fecha para buscar.");
            return;
        }
        List<Torneo> encontrados = gestion.buscarPorFecha(dpBuscar.getValue());
        tabla.setItems(FXCollections.observableArrayList(encontrados));
        if (encontrados.isEmpty()) {
            mostrarInfo("Sin resultados", "No hay torneos que comiencen el " + formatear(dpBuscar.getValue()) + ".");
        } else {
            tabla.getSelectionModel().selectFirst();
        }
    }

    // ------------------------------------------------------------ datos en pantalla

    @Override
    public void actualizar() {
        List<Torneo> todos = gestion.listarTorneos(null);
        lblAbiertos.setText(contar(todos, EstadoTorneo.INSCRIPCION_ABIERTA));
        lblPorComenzar.setText(contar(todos, EstadoTorneo.INSCRIPCION_CERRADA));
        lblEnCurso.setText(contar(todos, EstadoTorneo.EN_CURSO));
        lblFinalizados.setText(contar(todos, EstadoTorneo.FINALIZADO));
        cargarTabla();
        if (tabla.getSelectionModel().getSelectedItem() == null && !tabla.getItems().isEmpty()) {
            tabla.getSelectionModel().selectFirst();
        }
        mostrarFicha(tabla.getSelectionModel().getSelectedItem());
    }

    private String contar(List<Torneo> torneos, EstadoTorneo estado) {
        return String.valueOf(torneos.stream().filter(t -> t.getEstado() == estado).count());
    }

    /** RF28 y RF34: lista los torneos filtrados por estado y, si se pidió, ordenados por fecha. */
    private void cargarTabla() {
        String filtro = cmbFiltro.getValue();
        List<Torneo> lista = new ArrayList<>();
        for (Torneo torneo : gestion.listarTorneos(null)) {
            boolean incluir = TODOS.equals(filtro)
                    || (ACTIVOS.equals(filtro) && torneo.getEstado() != EstadoTorneo.FINALIZADO)
                    || torneo.getEstado().toString().equals(filtro);
            if (incluir) {
                lista.add(torneo);
            }
        }
        if (ordenarPorFecha) {
            lista = gestion.ordenarPorFecha(lista);
        }
        Torneo seleccionado = tabla.getSelectionModel().getSelectedItem();
        tabla.setItems(FXCollections.observableArrayList(lista));
        if (seleccionado != null && lista.contains(seleccionado)) {
            tabla.getSelectionModel().select(seleccionado);
        }
        tabla.refresh();
    }

    /** Muestra la ficha completa del torneo elegido, con secciones según su estado. */
    private void mostrarFicha(Torneo torneo) {
        contenedorFicha.getChildren().clear();
        if (torneo == null) {
            mostrarNodo(contenedorFicha, false);
            return;
        }
        mostrarNodo(contenedorFicha, true);
        contenedorFicha.getChildren().add(new FichaTorneo(this, sistema, torneo, true, true, this::actualizar));
    }

    private String colorEstado(Torneo torneo) {
        switch (torneo.getEstado()) {
            case INSCRIPCION_ABIERTA:
            case EN_CURSO:
                return "chip-verde";
            default:
                return "chip-gris";
        }
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtCosto.clear();
        cmbModalidad.setValue(null);
        dpInicio.setValue(null);
        dpFin.setValue(null);
        dpPlazo.setValue(null);
    }
}
