package primerpunto.modelo;

/**
 * Resultado de un partido. El marcador se escribe por sets, siempre desde el
 * punto de vista del participante 1 del partido, por ejemplo "6-4 3-6 7-5".
 * Las reglas para validarlo dependen del formato de resultado del torneo.
 */
public class Resultado {

    private final int idResultado;
    private final String marcador;
    private final Participante ganador;
    private final boolean esWO;
    private final boolean esRetiro;
    private final FormatoResultado formato;

    public Resultado(int idResultado, String marcador, Participante ganador, boolean esWO, boolean esRetiro,
                     FormatoResultado formato) {
        if (ganador == null) {
            throw new IllegalArgumentException("El resultado debe indicar un ganador.");
        }
        this.idResultado = idResultado;
        this.marcador = esWO ? "W.O." : (marcador == null ? "" : marcador.trim());
        this.ganador = ganador;
        this.esWO = esWO;
        this.esRetiro = esRetiro;
        this.formato = formato;
        if (!validarFormatoMarcador()) {
            throw new IllegalArgumentException("Marcador no válido para «" + formato + "» (ej.: "
                    + formato.getEjemplo() + ").");
        }
    }

    /** Resultado de un partido al mejor de 3 sets (el formato más habitual). */
    public Resultado(int idResultado, String marcador, Participante ganador, boolean esWO, boolean esRetiro) {
        this(idResultado, marcador, ganador, esWO, esRetiro, FormatoResultado.MEJOR_DE_TRES);
    }

    public int getIdResultado() {
        return idResultado;
    }

    public String getMarcador() {
        return marcador;
    }

    public Participante getGanador() {
        return ganador;
    }

    public boolean isEsWO() {
        return esWO;
    }

    public boolean isEsRetiro() {
        return esRetiro;
    }

    public FormatoResultado getFormato() {
        return formato;
    }

    /**
     * Valida el marcador según el formato del torneo: cada set tiene que ser válido y
     * gana el partido quien llega primero a los sets necesarios. Si hubo retiro, el
     * último set puede haber quedado incompleto. Un W.O. no lleva marcador.
     */
    public boolean validarFormatoMarcador() {
        if (esWO) {
            return true;
        }
        try {
            int[] sets = contarSets(marcador, esRetiro, formato);
            return esRetiro || Math.max(sets[0], sets[1]) == formato.getSetsParaGanar();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Sets ganados por el participante 1 y por el participante 2, en ese orden. */
    public int[] getSets() {
        return esWO ? new int[] {0, 0} : contarSets(marcador, esRetiro, formato);
    }

    /**
     * Recorre el marcador set por set y cuenta cuántos ganó cada participante.
     * Lanza IllegalArgumentException si algún set no respeta las reglas del formato.
     */
    public static int[] contarSets(String marcador, boolean admiteSetIncompleto, FormatoResultado formato) {
        if (marcador == null || marcador.isBlank()) {
            throw new IllegalArgumentException("Marcador vacío");
        }
        String[] partes = marcador.trim().split("\\s+");
        if (partes.length > formato.getMaximoSets()) {
            throw new IllegalArgumentException("Este formato tiene como máximo " + formato.getMaximoSets() + " sets");
        }
        int setsP1 = 0;
        int setsP2 = 0;
        for (int i = 0; i < partes.length; i++) {
            if (setsP1 == formato.getSetsParaGanar() || setsP2 == formato.getSetsParaGanar()) {
                throw new IllegalArgumentException("Hay sets cargados después de que el partido terminó");
            }
            String[] games = partes[i].split("-");
            if (games.length != 2) {
                throw new IllegalArgumentException("Set con formato incorrecto: " + partes[i]);
            }
            int g1 = Integer.parseInt(games[0]); // si no es un número, lanza NumberFormatException
            int g2 = Integer.parseInt(games[1]);
            boolean ultimoSet = i == partes.length - 1;
            if (formato.esSetValido(i + 1, g1, g2)) { // la regla depende del formato del torneo
                if (g1 > g2) {
                    setsP1++;
                } else {
                    setsP2++;
                }
            } else if (!(admiteSetIncompleto && ultimoSet && g1 >= 0 && g2 >= 0)) {
                throw new IllegalArgumentException("Set inválido: " + partes[i]);
            }
        }
        return new int[] {setsP1, setsP2};
    }
}
