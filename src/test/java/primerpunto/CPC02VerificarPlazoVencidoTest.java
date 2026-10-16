package primerpunto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import primerpunto.modelo.Modalidad;
import primerpunto.modelo.Torneo;

/**
 * Caso de prueba CPC02 (caja blanca, CU05): cobertura de caminos del método
 * verificarPlazoVencido() de la clase Torneo.
 */
@DisplayName("CPC02 - Torneo.verificarPlazoVencido(): cobertura de caminos")
class CPC02VerificarPlazoVencidoTest {

    private Torneo torneoConPlazo(LocalDate plazo) {
        return new Torneo(1, "Torneo Apertura 2026", Modalidad.SINGLE, 8500,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), plazo, "Club Los Cuervos");
    }

    @Test
    @DisplayName("Camino 1: plazo no configurado (null) - false, sin lanzar excepción")
    void camino1PlazoNoConfigurado() {
        assertFalse(torneoConPlazo(null).verificarPlazoVencido(LocalDate.of(2026, 10, 1)));
    }

    @Test
    @DisplayName("Camino 2: fecha actual 30/09/2026 ≤ plazo 30/09/2026 - false (no vencido)")
    void camino2DentroDelPlazo() {
        assertFalse(torneoConPlazo(LocalDate.of(2026, 9, 30)).verificarPlazoVencido(LocalDate.of(2026, 9, 30)));
    }

    @Test
    @DisplayName("Camino 3: fecha actual 01/10/2026 > plazo 30/09/2026 - true (vencido)")
    void camino3PlazoVencido() {
        assertTrue(torneoConPlazo(LocalDate.of(2026, 9, 30)).verificarPlazoVencido(LocalDate.of(2026, 10, 1)));
    }
}
