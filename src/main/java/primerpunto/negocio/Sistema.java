package primerpunto.negocio;

/**
 * Punto de acceso a la capa de negocio: reúne los cuatro controladores del
 * sistema para que la interfaz pueda usarlos. Cada ventana trabaja con el
 * controlador de su módulo.
 */
public class Sistema {

    private final GestionTorneos gestionTorneos;
    private final GestionParticipantes gestionParticipantes;
    private final GestionCuadrosZonas gestionCuadrosZonas;
    private final GestionPartidosCanchas gestionPartidosCanchas;

    public Sistema() {
        gestionTorneos = new GestionTorneos();
        gestionParticipantes = new GestionParticipantes();
        gestionCuadrosZonas = new GestionCuadrosZonas(gestionTorneos);
        gestionPartidosCanchas = new GestionPartidosCanchas(gestionTorneos);
    }

    public GestionTorneos getGestionTorneos() {
        return gestionTorneos;
    }

    public GestionParticipantes getGestionParticipantes() {
        return gestionParticipantes;
    }

    public GestionCuadrosZonas getGestionCuadrosZonas() {
        return gestionCuadrosZonas;
    }

    public GestionPartidosCanchas getGestionPartidosCanchas() {
        return gestionPartidosCanchas;
    }
}
