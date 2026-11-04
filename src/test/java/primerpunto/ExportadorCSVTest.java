package primerpunto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import primerpunto.excepciones.ArchivoException;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Torneo;
import primerpunto.negocio.DatosIniciales;
import primerpunto.negocio.Sistema;

/** Exportación de listados a archivos CSV: se escribe el archivo y se vuelve a leer para controlarlo. */
@DisplayName("Exportación a CSV")
class ExportadorCSVTest {

    @TempDir
    Path carpeta;

    private Sistema sistema;

    @BeforeEach
    void prepararDatos() throws PrimerPuntoException {
        sistema = new Sistema();
        DatosIniciales.cargar(sistema);
    }

    private Torneo torneo(String nombre) {
        for (Torneo torneo : sistema.getGestionTorneos().listarTorneos(null)) {
            if (torneo.getNombre().equals(nombre)) {
                return torneo;
            }
        }
        throw new IllegalStateException("No está el torneo " + nombre);
    }

    @Test
    @DisplayName("Inscriptos: una línea de encabezado y una por inscripción, separadas por punto y coma")
    void exportaInscriptos() throws PrimerPuntoException, IOException {
        Torneo primavera = torneo("Torneo Primavera");
        Path archivo = carpeta.resolve("inscriptos.csv");

        sistema.getGestionParticipantes().exportarInscriptos(primavera, archivo.toString());

        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        assertEquals(1 + primavera.getInscripciones().size(), lineas.size());
        assertTrue(lineas.get(0).startsWith("\uFEFFTorneo;N.º inscripción;Participante"));
        assertTrue(lineas.get(1).startsWith("Torneo Primavera;"));
        assertTrue(lineas.stream().anyMatch(linea -> linea.contains(";Pendiente;")), "Hay un pago pendiente");
    }

    @Test
    @DisplayName("Programación del día: los partidos de hoy de todos los torneos, ordenados por horario")
    void exportaProgramacionDelDia() throws PrimerPuntoException, IOException {
        LocalDate hoy = LocalDate.now();
        Path archivo = carpeta.resolve("programacion.csv");

        sistema.getGestionPartidosCanchas().exportarProgramacion(hoy, archivo.toString());

        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        int partidosDeHoy = sistema.getGestionPartidosCanchas().consultarPartidosDelDia(hoy).size();
        assertEquals(1 + partidosDeHoy, lineas.size());
        assertTrue(lineas.stream().anyMatch(linea -> linea.contains("Copa Los Cuervos;Semifinal")));
    }

    @Test
    @DisplayName("Si la carpeta no existe, se informa con ArchivoException y el programa no se interrumpe")
    void carpetaInexistente() {
        Torneo primavera = torneo("Torneo Primavera");
        String ruta = carpeta.resolve("no-existe").resolve("inscriptos.csv").toString();

        ArchivoException error = assertThrows(ArchivoException.class,
                () -> sistema.getGestionParticipantes().exportarInscriptos(primavera, ruta));
        assertTrue(error.getMessage().startsWith("No se pudo guardar el archivo"));
    }
}
