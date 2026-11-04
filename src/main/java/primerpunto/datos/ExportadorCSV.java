package primerpunto.datos;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import primerpunto.excepciones.ArchivoException;
import primerpunto.modelo.Inscripcion;
import primerpunto.modelo.Partido;
import primerpunto.modelo.Torneo;

/**
 * Exporta listados a archivos CSV (valores separados por punto y coma), que se abren
 * directamente en Excel o Google Sheets: los inscriptos de un torneo, para el control
 * de cobros, y la programación de un día, para la cartelera del club.
 */
public class ExportadorCSV {

    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Inscriptos del torneo con su fecha de inscripción, estado de pago y estado (CU08). */
    public void exportarInscriptos(Torneo torneo, String rutaArchivo) throws ArchivoException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] {"Torneo", "N.º inscripción", "Participante", "Fecha de inscripción", "Pago", "Estado",
            "Motivo de baja"});
        for (Inscripcion inscripcion : torneo.getInscripciones()) {
            filas.add(new String[] {
                torneo.getNombre(),
                String.valueOf(inscripcion.getIdInscripcion()),
                inscripcion.getParticipante().obtenerNombreCompleto(),
                inscripcion.getFecha().format(FECHA),
                inscripcion.isEstadoPago() ? "Abonado" : "Pendiente",
                inscripcion.getEstadoInscripcion().toString(),
                inscripcion.getMotivoBaja() == null ? "" : inscripcion.getMotivoBaja()
            });
        }
        escribir(rutaArchivo, filas);
    }

    /** Partidos de un día, de todos los torneos, ordenados por horario y cancha (CU12). */
    public void exportarProgramacion(LocalDate dia, List<Partido> partidos, List<Torneo> torneos, String rutaArchivo)
            throws ArchivoException {
        List<Partido> ordenados = new ArrayList<>(partidos);
        ordenados.sort(Comparator.comparing(Partido::getHorario).thenComparing(p -> p.getCancha().getNumero()));
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] {"Fecha", "Horario", "Cancha", "Torneo", "Instancia", "Participante 1", "Participante 2",
            "Resultado"});
        for (Partido partido : ordenados) {
            filas.add(new String[] {
                dia.format(FECHA),
                partido.getHorario().toString(),
                String.valueOf(partido.getCancha().getNumero()),
                nombreTorneo(partido, torneos),
                partido.getInstancia(),
                partido.getNombreParticipante1(),
                partido.getNombreParticipante2(),
                partido.getResultado() == null ? "" : partido.getResultado().getMarcador()
            });
        }
        escribir(rutaArchivo, filas);
    }

    /**
     * Escribe las filas en el archivo, una por línea. Empieza con la marca de UTF-8
     * para que Excel muestre bien las tildes y la ñ. El try-with-resources cierra el
     * archivo aunque ocurra un error.
     */
    private void escribir(String rutaArchivo, List<String[]> filas) throws ArchivoException {
        try (PrintWriter salida = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(rutaArchivo), StandardCharsets.UTF_8)))) {
            salida.print('\uFEFF');
            for (String[] campos : filas) {
                salida.println(armarLinea(campos));
            }
            if (salida.checkError()) {
                throw new IOException("error al escribir en el disco");
            }
        } catch (IOException e) {
            throw new ArchivoException("No se pudo guardar el archivo " + rutaArchivo + ". Si está abierto en Excel, "
                    + "cerralo y probá de nuevo.\n\nDetalle: " + e.getMessage(), e);
        }
    }

    /** Une los campos con el separador; si un campo tiene ; o comillas, se encierra entre comillas. */
    private String armarLinea(String[] campos) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                linea.append(SEPARADOR);
            }
            String campo = campos[i];
            if (campo.contains(SEPARADOR) || campo.contains("\"") || campo.contains("\n")) {
                campo = "\"" + campo.replace("\"", "\"\"") + "\"";
            }
            linea.append(campo);
        }
        return linea.toString();
    }

    private String nombreTorneo(Partido partido, List<Torneo> torneos) {
        for (Torneo torneo : torneos) {
            if (torneo.getPartidos().contains(partido)) {
                return torneo.getNombre();
            }
        }
        return "";
    }
}
