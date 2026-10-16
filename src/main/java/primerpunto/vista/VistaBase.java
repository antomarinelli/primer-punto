package primerpunto.vista;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;
import primerpunto.excepciones.DatoInvalidoException;
import primerpunto.negocio.Sistema;

/**
 * Base de todas las ventanas del sistema. Arma el encabezado común (título y saludo
 * al organizador) y los componentes con el estilo de los prototipos de interfaz:
 * tarjetas, campos, botones, tablas y mensajes. Cada ventana define su contenido y
 * cómo se actualiza al volver a entrar (actualizar()).
 */
public abstract class VistaBase extends VBox {

    protected static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    protected final Sistema sistema;

    protected VistaBase(Sistema sistema, String titulo) {
        this.sistema = sistema;
        getStyleClass().add("contenido");
        setSpacing(30);
        getChildren().add(crearEncabezado(titulo));
    }

    /** Vuelve a leer los datos de la capa de negocio. Se llama cada vez que se entra a la ventana. */
    public abstract void actualizar();

    // ------------------------------------------------------------ encabezado

    private HBox crearEncabezado(String titulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo");
        VBox textos = new VBox(2, lblTitulo);

        Circle avatar = new Circle(21);
        avatar.getStyleClass().add("avatar");
        Label hola = new Label("Hola,");
        hola.getStyleClass().add("saludo");
        Label organizador = new Label("Organizador");
        organizador.getStyleClass().add("saludo-nombre");
        VBox textosSaludo = new VBox(-2, hola, organizador);
        textosSaludo.setAlignment(Pos.CENTER_LEFT);
        HBox saludo = new HBox(14, avatar, textosSaludo);
        saludo.setAlignment(Pos.CENTER_LEFT);
        saludo.setFillHeight(false);

        HBox encabezado = new HBox(40, textos, espacio(), saludo);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        return encabezado;
    }

    // ------------------------------------------------------------ contenedores

    protected VBox tarjeta(String titulo) {
        VBox tarjeta = new VBox();
        tarjeta.getStyleClass().add("tarjeta");
        if (titulo != null) {
            tarjeta.getChildren().add(etiquetaClase(titulo, "tarjeta-titulo"));
        }
        return tarjeta;
    }

    protected VBox tarjetaVerde(String titulo) {
        VBox tarjeta = new VBox();
        tarjeta.getStyleClass().add("tarjeta-verde");
        tarjeta.getChildren().add(etiquetaClase(titulo, "tarjeta-titulo"));
        return tarjeta;
    }

    /**
     * Indicador: tarjeta chica con un número destacado (por ejemplo, "3 torneos en curso").
     * Devuelve la etiqueta del número para poder actualizarla después.
     */
    protected Label indicador(HBox fila, String titulo, boolean verde) {
        Label valor = etiquetaClase("0", "indicador-valor");
        VBox tarjeta = new VBox(2, etiquetaClase(titulo.toUpperCase(), "indicador-titulo"), valor);
        tarjeta.getStyleClass().add(verde ? "indicador-verde" : "indicador");
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setPrefWidth(10);
        fila.getChildren().add(tarjeta);
        return valor;
    }

    /**
     * Botón que muestra u oculta un contenido (se usa para los formularios y las
     * secciones que se despliegan al tocarlas).
     */
    protected void alternarAlTocar(javafx.scene.control.ButtonBase boton, Node contenido, boolean visibleAlInicio) {
        mostrarNodo(contenido, visibleAlInicio);
        marcarActivo(boton, visibleAlInicio);
        boton.setOnAction(e -> {
            boolean mostrar = !contenido.isVisible();
            mostrarNodo(contenido, mostrar);
            marcarActivo(boton, mostrar);
        });
    }

    protected void mostrarNodo(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    protected void marcarActivo(Node boton, boolean activo) {
        boton.getStyleClass().remove("activo");
        if (activo) {
            boton.getStyleClass().add("activo");
        }
    }

    /** Fila que reparte el espacio: el primer nodo crece y el segundo mantiene el ancho indicado. */
    protected HBox filaPrincipal(Node izquierda, Node derecha, double anchoDerecha) {
        HBox.setHgrow(izquierda, Priority.ALWAYS);
        if (derecha instanceof Region region) {
            region.setPrefWidth(anchoDerecha);
            region.setMinWidth(anchoDerecha);
            region.setMaxWidth(anchoDerecha);
        }
        return new HBox(28, izquierda, derecha);
    }

    protected Region espacio() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }

    protected Region separador() {
        Region linea = new Region();
        linea.getStyleClass().add("separador-tarjeta");
        return linea;
    }

    protected Label etiquetaClase(String texto, String clase) {
        Label label = new Label(texto);
        label.getStyleClass().add(clase);
        label.setWrapText(true);
        return label;
    }

    // ------------------------------------------------------------ campos

    /** Campo de formulario con su etiqueta en mayúsculas arriba, como en los prototipos. */
    protected VBox campo(String etiqueta, Node control) {
        Label label = new Label(etiqueta.toUpperCase());
        label.getStyleClass().add("etiqueta");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        VBox caja = new VBox(7, label, control);
        HBox.setHgrow(caja, Priority.ALWAYS);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    protected HBox filaCampos(Node... campos) {
        HBox fila = new HBox(22, campos);
        for (Node nodo : campos) {
            HBox.setHgrow(nodo, Priority.ALWAYS);
            if (nodo instanceof Region region) {
                region.setPrefWidth(10);
            }
        }
        return fila;
    }

    protected TextField texto(String sugerencia) {
        TextField campo = new TextField();
        campo.setPromptText(sugerencia);
        campo.getStyleClass().add("campo");
        return campo;
    }

    protected <T> ComboBox<T> combo(String sugerencia) {
        ComboBox<T> combo = new ComboBox<>();
        combo.setPromptText(sugerencia);
        combo.getStyleClass().add("campo");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setVisibleRowCount(10);
        return combo;
    }

    /** Selector de fecha con formato dd/MM/yyyy. */
    protected DatePicker fecha() {
        DatePicker selector = new DatePicker();
        selector.getStyleClass().add("campo-fecha");
        selector.setMinWidth(170);
        selector.setPromptText("dd/mm/aaaa");
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : fecha.format(FORMATO_FECHA);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(texto.trim(), FORMATO_FECHA);
                } catch (java.time.format.DateTimeParseException e) {
                    return null; // fecha mal escrita: se trata como campo vacío
                }
            }
        });
        return selector;
    }

    /**
     * Convierte el texto de un campo numérico. Si tiene letras o símbolos, se informa
     * qué campo está mal en lugar de cortar la operación.
     */
    protected double leerNumero(TextField campo, String nombreCampo) throws DatoInvalidoException {
        String texto = campo.getText().trim().replace("$", "").replace(".", "").replace(",", ".").trim();
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El campo «" + nombreCampo + "» debe ser un número. Ingresaste: «"
                    + campo.getText() + "».", e);
        }
    }

    protected int leerEntero(TextField campo, String nombreCampo) throws DatoInvalidoException {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El campo «" + nombreCampo + "» debe ser un número entero. Ingresaste: «"
                    + campo.getText() + "».", e);
        }
    }

    protected String formatear(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FORMATO_FECHA);
    }

    /** Cantidad con la palabra en singular o plural, por ejemplo "1 partido" o "3 partidos". */
    protected static String cantidad(long numero, String singular, String plural) {
        return numero + " " + (numero == 1 ? singular : plural);
    }

    /** Muestra una duración en minutos de forma legible, por ejemplo "1 h 30 min". */
    protected static String formatearDuracion(int minutos) {
        int horas = minutos / 60;
        int resto = minutos % 60;
        if (horas == 0) {
            return resto + " min";
        }
        return horas + " h" + (resto == 0 ? "" : " " + resto + " min");
    }

    // ------------------------------------------------------------ botones

    protected Button botonPrimario(String texto, EventHandler<ActionEvent> accion) {
        return boton(texto, "boton-primario", accion);
    }

    protected Button botonSecundario(String texto, EventHandler<ActionEvent> accion) {
        return boton(texto, "boton-secundario", accion);
    }

    protected Button botonPeligro(String texto, EventHandler<ActionEvent> accion) {
        return boton(texto, "boton-peligro", accion);
    }

    private Button boton(String texto, String clase, EventHandler<ActionEvent> accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(clase);
        boton.setOnAction(accion);
        boton.setMinWidth(Region.USE_PREF_SIZE); // el texto del botón nunca se corta
        return boton;
    }

    // ------------------------------------------------------------ tablas

    protected <S> TableView<S> tabla(String mensajeVacio) {
        TableView<S> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(etiquetaClase(mensajeVacio, "mensaje-vacio"));
        tabla.setPrefHeight(280);
        return tabla;
    }

    /** Columna de texto: recibe una función que obtiene el valor a mostrar de cada fila. */
    protected <S> TableColumn<S, String> columna(String titulo, Function<S, String> valor, double ancho) {
        TableColumn<S, String> columna = new TableColumn<>(titulo.toUpperCase());
        columna.setCellValueFactory(fila -> new SimpleStringProperty(valor.apply(fila.getValue())));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        return columna;
    }

    /** Columna que muestra el valor como una "pastilla" de color (por ejemplo, el estado). */
    protected <S> TableColumn<S, String> columnaChip(String titulo, Function<S, String> valor,
                                                     Function<S, String> claseColor, double ancho) {
        TableColumn<S, String> columna = columna(titulo, valor, ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Label chip = new Label(item);
                    chip.getStyleClass().addAll("chip", claseColor.apply(getTableRow().getItem()));
                    setGraphic(chip);
                }
                setText(null);
            }
        });
        return columna;
    }

    // ------------------------------------------------------------ mensajes

    protected void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        alerta.setTitle("Primer Punto");
        alerta.setHeaderText("No se pudo completar la operación");
        prepararDialogo(alerta);
        alerta.showAndWait();
    }

    protected void mostrarInfo(String encabezado, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setTitle("Primer Punto");
        alerta.setHeaderText(encabezado);
        prepararDialogo(alerta);
        alerta.showAndWait();
    }

    protected boolean confirmar(String encabezado, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        alerta.setTitle("Primer Punto");
        alerta.setHeaderText(encabezado);
        prepararDialogo(alerta);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }

    /** Aplica a los cuadros de diálogo la misma hoja de estilos que la ventana principal. */
    protected void prepararDialogo(Dialog<?> dialogo) {
        if (getScene() != null) {
            dialogo.initOwner(getScene().getWindow());
            dialogo.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
        dialogo.getDialogPane().setMinWidth(460);
    }
}
