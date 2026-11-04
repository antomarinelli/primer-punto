package primerpunto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.excepciones.PrimerPuntoException;
import primerpunto.modelo.Cuadro;
import primerpunto.modelo.FilaPosicion;
import primerpunto.modelo.Formato;
import primerpunto.modelo.Torneo;
import primerpunto.modelo.Zona;
import primerpunto.negocio.DatosIniciales;
import primerpunto.negocio.Sistema;

/**
 * Al leer un cuadro o una zona de la base, se vuelven a armar a partir de sus partidos.
 * El resultado tiene que ser igual al formato original: mismos lugares en cada ronda,
 * mismo campeón y misma tabla de posiciones.
 */
class RestaurarFormatoTest {

    @Test
    @DisplayName("Los cuadros y las zonas de los datos de ejemplo se reconstruyen iguales a partir de sus partidos")
    void formatosReconstruidosIguales() throws PrimerPuntoException {
        Sistema sistema = new Sistema();
        DatosIniciales.cargar(sistema);
        int cuadros = 0;
        int zonas = 0;
        for (Torneo torneo : sistema.getGestionTorneos().listarTorneos(null)) {
            for (Formato formato : torneo.getFormatos()) {
                List<?> partidos = formato.getPartidos();
                if (formato instanceof Cuadro original) {
                    Cuadro copia = new Cuadro(original.getIdFormato());
                    copia.restaurar(original.getTamanio(), original.getCantidadByes(), new ArrayList<>(original.getPartidos()));
                    for (int ronda = 0; ronda <= original.getCantidadRondas(); ronda++) {
                        assertArrayEquals(original.getLugaresRonda(ronda), copia.getLugaresRonda(ronda),
                                torneo.getNombre() + ", ronda " + ronda);
                    }
                    assertEquals(original.getCampeon(), copia.getCampeon());
                    cuadros++;
                } else if (formato instanceof Zona original) {
                    Zona copia = new Zona(original.getIdFormato(), original.getNumeroZona());
                    copia.restaurar(new ArrayList<>(original.getPartidos()));
                    List<FilaPosicion> tablaOriginal = original.calcularTablaPosiciones();
                    List<FilaPosicion> tablaCopia = copia.calcularTablaPosiciones();
                    assertEquals(tablaOriginal.size(), tablaCopia.size());
                    for (int i = 0; i < tablaOriginal.size(); i++) {
                        assertEquals(tablaOriginal.get(i).getParticipante(), tablaCopia.get(i).getParticipante());
                        assertEquals(tablaOriginal.get(i).getGanados(), tablaCopia.get(i).getGanados());
                    }
                    zonas++;
                }
                assertFalse(partidos.isEmpty());
            }
        }
        assertEquals(2, cuadros, "Torneo Invierno y Copa Los Cuervos");
        assertEquals(2, zonas, "Mixto de Primavera");
    }
}
