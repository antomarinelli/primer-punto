package primerpunto.vista;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import primerpunto.negocio.Sistema;

/**
 * Ventana principal: menú de selección lateral (con el logo y la opción activa
 * resaltada en verde) y, a la derecha, la ventana del módulo elegido.
 */
public class VentanaPrincipal extends BorderPane {

    private final ScrollPane area = new ScrollPane();
    private final List<Button> opciones = new ArrayList<>();

    private final VentanaTorneos ventanaTorneos;
    private final VentanaParticipantes ventanaParticipantes;
    private final VentanaCuadroZona ventanaCuadroZona;
    private final VentanaPartidoCancha ventanaPartidoCancha;
    private final VentanaHistorial ventanaHistorial;

    public VentanaPrincipal(Sistema sistema) {
        ventanaTorneos = new VentanaTorneos(sistema);
        ventanaParticipantes = new VentanaParticipantes(sistema);
        ventanaCuadroZona = new VentanaCuadroZona(sistema);
        ventanaPartidoCancha = new VentanaPartidoCancha(sistema);
        ventanaHistorial = new VentanaHistorial(sistema);

        area.setFitToWidth(true);
        area.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setLeft(crearMenu());
        setCenter(area);
        opciones.get(0).fire(); // al iniciar se muestra la ventana de torneos
    }

    private VBox crearMenu() {
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/primerpunto/vista/logo.png")));
        logo.setFitWidth(150);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        Label panel = new Label("PANEL DEL ORGANIZADOR");
        panel.getStyleClass().add("panel-organizador");

        Button torneos = opcion("Torneos", () -> mostrar(ventanaTorneos));
        Button participantes = opcion("Participantes", () -> mostrar(ventanaParticipantes));
        Button cuadros = opcion("Formato", () -> mostrar(ventanaCuadroZona));
        Button partidos = opcion("Programación", () -> mostrar(ventanaPartidoCancha));

        Region separador = new Region();
        separador.getStyleClass().add("menu-separador");
        VBox.setMargin(separador, new javafx.geometry.Insets(16, 10, 16, 10));

        Button historial = opcion("Historial", () -> mostrar(ventanaHistorial));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        VBox menu = new VBox(logo, panel, torneos, participantes, cuadros, partidos, separador, historial,
                espacio);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);
        VBox.setMargin(logo, new javafx.geometry.Insets(0, 0, 0, 10));
        return menu;
    }

    /** Crea una opción del menú. Al elegirla, queda resaltada y se ejecuta su acción. */
    private Button opcion(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("menu-item");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(e -> {
            for (Button otra : opciones) {
                otra.getStyleClass().remove("menu-item-activo");
            }
            boton.getStyleClass().add("menu-item-activo");
            accion.run();
        });
        opciones.add(boton);
        return boton;
    }

    /** Muestra la ventana elegida con sus datos actualizados. */
    private void mostrar(VistaBase ventana) {
        ventana.actualizar();
        area.setContent(ventana);
        area.setVvalue(0);
    }
}
