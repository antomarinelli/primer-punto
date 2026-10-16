package primerpunto.vista;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.FilaPosicion;
import primerpunto.modelo.Participante;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;
import primerpunto.negocio.GestionCuadrosZonas;

/**
 * Muestra el formato de competencia de un torneo: las tablas de posiciones de sus
 * zonas y el cuadro de eliminación dibujado ronda por ronda. Se usa en las
 * ventanas Torneos, Formato e Historial.
 */
public class PanelFormato extends VBox {

    private static final double ANCHO_LLAVE = 175;
    private static final double ALTO_LLAVE = 28;
    private static final double SEPARACION_RONDAS = 215;

    private final VistaBase vista;
    private final GestionCuadrosZonas gestion;

    public PanelFormato(VistaBase vista, GestionCuadrosZonas gestion, Torneo torneo) {
        super(18);
        this.vista = vista;
        this.gestion = gestion;
        Cuadro cuadro = gestion.obtenerCuadro(torneo);
        List<Zona> zonas = gestion.obtenerZonas(torneo);
        if (zonas.isEmpty() && cuadro == null) {
            getChildren().add(vista.etiquetaClase("Este torneo todavía no tiene un formato generado.", "mensaje-vacio"));
            return;
        }
        if (!zonas.isEmpty()) {
            HBox filaZonas = new HBox(22);
            for (Zona zona : zonas) {
                filaZonas.getChildren().add(crearTablaZona(zona));
            }
            getChildren().addAll(vista.etiquetaClase("Zonas de todos contra todos", "subtitulo-seccion"), filaZonas);
        }
        if (cuadro != null) {
            ScrollPane desplazable = new ScrollPane(dibujarCuadro(cuadro));
            desplazable.setFitToHeight(true);
            desplazable.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            desplazable.getStyleClass().add("transparente");
            getChildren().addAll(vista.etiquetaClase("Cuadro de eliminación directa", "subtitulo-seccion"), desplazable);
        }
    }

    /** Tabla de posiciones de una zona (RF27). */
    private VBox crearTablaZona(Zona zona) {
        TableView<FilaPosicion> tabla = vista.tabla("Sin participantes");
        List<FilaPosicion> filas = gestion.obtenerTablaPosiciones(zona);
        tabla.getColumns().addAll(List.of(
                vista.columna("#", f -> String.valueOf(filas.indexOf(f) + 1), 30),
                vista.columna("Participante", f -> f.getParticipante().obtenerNombreCompleto(), 190),
                vista.columna("PJ", f -> String.valueOf(f.getJugados()), 40),
                vista.columna("PG", f -> String.valueOf(f.getGanados()), 40),
                vista.columna("PP", f -> String.valueOf(f.getPerdidos()), 40),
                vista.columna("Dif. sets", f -> (f.getDiferenciaSets() > 0 ? "+" : "") + f.getDiferenciaSets(), 70)));
        tabla.setItems(FXCollections.observableArrayList(filas));
        tabla.setPrefHeight(46 + 40 * filas.size());
        long jugados = zona.getPartidos().stream().filter(Partido::estaFinalizado).count();
        VBox caja = new VBox(8, vista.etiquetaClase(zona.getDescripcion().toUpperCase() + " · " + jugados + " de "
                + zona.getPartidos().size() + " partidos jugados", "etiqueta"), tabla);
        HBox.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }

    /**
     * Dibuja el cuadro: una columna por ronda, con un recuadro por lugar y líneas
     * que unen cada par de lugares con el lugar de la ronda siguiente.
     */
    private Pane dibujarCuadro(Cuadro cuadro) {
        Pane lienzo = new Pane();
        int rondas = cuadro.getCantidadRondas();
        double[][] y = new double[rondas + 1][];

        // Posición vertical de cada lugar: en la primera ronda, de a pares; en las siguientes,
        // centrado entre los dos lugares de los que proviene
        int lugaresPrimera = cuadro.getTamanio();
        y[0] = new double[lugaresPrimera];
        for (int i = 0; i < lugaresPrimera; i++) {
            y[0][i] = 30 + i * (ALTO_LLAVE + 8) + (i / 2) * 18;
        }
        for (int r = 1; r <= rondas; r++) {
            y[r] = new double[y[r - 1].length / 2];
            for (int i = 0; i < y[r].length; i++) {
                y[r][i] = (y[r - 1][2 * i] + y[r - 1][2 * i + 1]) / 2;
            }
        }

        for (int r = 0; r <= rondas; r++) {
            double x = r * SEPARACION_RONDAS;
            Participante[] lugares = cuadro.getLugaresRonda(r);
            Label tituloRonda = vista.etiquetaClase(r < rondas ? cuadro.nombreRonda(r).toUpperCase() : "CAMPEÓN",
                    "titulo-ronda");
            tituloRonda.relocate(x + 4, 0);
            lienzo.getChildren().add(tituloRonda);

            for (int i = 0; i < lugares.length; i++) {
                if (r == rondas) {
                    lienzo.getChildren().add(crearCampeon(lugares[i], x, y[r][i]));
                } else {
                    Label llave = crearLlave(cuadro, r, i, lugares);
                    llave.relocate(x, y[r][i]);
                    lienzo.getChildren().add(llave);
                }
                // Líneas hacia la ronda siguiente: desde cada par de lugares hasta su lugar de destino
                if (r < rondas && i % 2 == 1) {
                    double xFin = x + ANCHO_LLAVE;
                    double xMedio = xFin + (SEPARACION_RONDAS - ANCHO_LLAVE) / 2;
                    double yArriba = y[r][i - 1] + ALTO_LLAVE / 2;
                    double yAbajo = y[r][i] + ALTO_LLAVE / 2;
                    double yDestino = y[r + 1][i / 2] + ALTO_LLAVE / 2;
                    lienzo.getChildren().addAll(
                            linea(xFin, yArriba, xMedio, yArriba),
                            linea(xFin, yAbajo, xMedio, yAbajo),
                            linea(xMedio, yArriba, xMedio, yAbajo),
                            linea(xMedio, yDestino, x + SEPARACION_RONDAS, yDestino));
                }
            }
        }
        double alto = y[0][lugaresPrimera - 1] + ALTO_LLAVE + 10;
        lienzo.setPrefSize(rondas * SEPARACION_RONDAS + 240, alto);
        lienzo.setMinSize(rondas * SEPARACION_RONDAS + 240, alto);
        return lienzo;
    }

    private Label crearLlave(Cuadro cuadro, int ronda, int lugar, Participante[] lugares) {
        Participante participante = lugares[lugar];
        Label llave = new Label();
        llave.getStyleClass().add("llave");
        llave.setPrefSize(ANCHO_LLAVE, ALTO_LLAVE);
        Partido partido = cuadro.buscarPartido(ronda, lugar / 2);
        if (participante != null) {
            llave.setText(participante.obtenerNombreCompleto());
            if (partido != null && partido.estaFinalizado()) {
                if (participante.equals(partido.getGanador())) {
                    llave.getStyleClass().add("llave-ganador");
                }
                llave.setTooltip(new Tooltip(partido + "\nResultado: " + partido.getResultado().getMarcador()));
            }
        } else if (ronda == 0 && lugar % 2 == 1) {
            llave.setText("Bye");
            llave.getStyleClass().add("llave-bye");
        } else {
            llave.setText("A definir");
            llave.getStyleClass().add("llave-vacia");
        }
        return llave;
    }

    private HBox crearCampeon(Participante campeon, double x, double y) {
        Circle circulo = new Circle(17, 17, 17);
        circulo.getStyleClass().add("campeon-icono");
        Polygon estrella = estrella();
        estrella.setStyle("-fx-fill: white;");
        estrella.setTranslateX(17);
        estrella.setTranslateY(17);
        Pane icono = new Pane(circulo, estrella);
        icono.setPrefSize(34, 34);
        Label texto = vista.etiquetaClase(campeon == null ? "Campeón" : campeon.obtenerNombreCompleto(), "campeon-texto");
        HBox caja = new HBox(10, icono, texto);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.relocate(x, y + ALTO_LLAVE / 2 - 17);
        return caja;
    }

    private Polygon estrella() {
        Polygon estrella = new Polygon();
        for (int i = 0; i < 10; i++) {
            double radio = i % 2 == 0 ? 10 : 4.2;
            double angulo = Math.toRadians(-90 + i * 36);
            estrella.getPoints().addAll(radio * Math.cos(angulo), radio * Math.sin(angulo));
        }
        return estrella;
    }

    private Line linea(double x1, double y1, double x2, double y2) {
        Line linea = new Line(x1, y1, x2, y2);
        linea.getStyleClass().add("linea-cuadro");
        return linea;
    }
}
