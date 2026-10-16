package primerpunto.vista;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.Sistema;

/**
 * Ventana del Historial (CU16): muestra los torneos ya finalizados como tarjetas.
 * Al tocar una, se abre su ficha con los datos generales a la vista y botones para
 * desplegar los participantes, el formato y los partidos disputados.
 */
public class VentanaHistorial extends VistaBase {

    private final FlowPane listaTorneos = new FlowPane(20, 20);
    private final VBox contenedor = new VBox(22);

    public VentanaHistorial(Sistema sistema) {
        super(sistema, "Historial");
        getChildren().add(contenedor);
    }

    @Override
    public void actualizar() {
        mostrarLista();
    }

    /** Lista de torneos finalizados (RF32), ordenados del más reciente al más antiguo. */
    private void mostrarLista() {
        List<Torneo> finalizados = sistema.getGestionTorneos().ordenarPorFecha(
                sistema.getGestionTorneos().consultarHistorialTorneos());
        java.util.Collections.reverse(finalizados);
        listaTorneos.getChildren().clear();
        for (Torneo torneo : finalizados) {
            listaTorneos.getChildren().add(crearTarjeta(torneo));
        }
        contenedor.getChildren().clear();
        if (finalizados.isEmpty()) {
            VBox vacio = tarjeta(null);
            vacio.getChildren().add(etiquetaClase("Todavía no hay torneos finalizados.", "mensaje-vacio"));
            contenedor.getChildren().add(vacio);
        } else {
            contenedor.getChildren().addAll(etiquetaClase(cantidad(finalizados.size(), "torneo finalizado", "torneos finalizados"),
                    "dato-aclaracion"), listaTorneos);
        }
    }

    private VBox crearTarjeta(Torneo torneo) {
        Cuadro cuadro = sistema.getGestionCuadrosZonas().obtenerCuadro(torneo);
        String campeon = cuadro != null && cuadro.getCampeon() != null
                ? cuadro.getCampeon().obtenerNombreCompleto() : "—";
        Label modalidad = new Label(torneo.getModalidad().toString());
        modalidad.getStyleClass().addAll("chip", "chip-gris");
        VBox tarjeta = new VBox(8,
                modalidad,
                etiquetaClase(torneo.getNombre(), "tarjeta-titulo"),
                etiquetaClase(formatear(torneo.getFechaInicio()) + " al " + formatear(torneo.getFechaFin()),
                        "dato-aclaracion"),
                etiquetaClase(torneo.getLugar(), "dato-aclaracion"),
                separador(),
                etiquetaClase("CAMPEÓN", "etiqueta"),
                etiquetaClase(campeon, "dato-ficha"));
        tarjeta.getStyleClass().add("tarjeta-historial");
        tarjeta.setPrefWidth(250);
        tarjeta.setOnMouseClicked(e -> mostrarTorneo(torneo));
        return tarjeta;
    }

    /** Ficha del torneo: solo los datos generales a la vista; el resto se despliega con los botones. */
    private void mostrarTorneo(Torneo torneo) {
        HBox volver = new HBox(botonSecundario("Volver", e -> mostrarLista()));
        volver.setAlignment(Pos.CENTER_LEFT);
        VBox ficha = tarjeta(null);
        ficha.getChildren().add(new FichaTorneo(this, sistema, torneo, false, false, this::mostrarLista));
        contenedor.getChildren().setAll(volver, ficha);
    }
}
