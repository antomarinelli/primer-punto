package primerpunto.modelo;

/**
 * Una fila de la tabla de posiciones de una zona. Se ordena por partidos ganados
 * y, en caso de empate, por diferencia de sets.
 */
public class FilaPosicion implements Comparable<FilaPosicion> {

    private final Participante participante;
    private int jugados;
    private int ganados;
    private int setsAFavor;
    private int setsEnContra;

    public FilaPosicion(Participante participante) {
        this.participante = participante;
    }

    /** Suma un partido jugado a la fila. */
    public void registrarPartido(boolean gano, int setsGanados, int setsPerdidos) {
        jugados++;
        if (gano) {
            ganados++;
        }
        setsAFavor += setsGanados;
        setsEnContra += setsPerdidos;
    }

    public Participante getParticipante() {
        return participante;
    }

    public int getJugados() {
        return jugados;
    }

    public int getGanados() {
        return ganados;
    }

    public int getPerdidos() {
        return jugados - ganados;
    }

    public int getDiferenciaSets() {
        return setsAFavor - setsEnContra;
    }

    @Override
    public int compareTo(FilaPosicion otra) {
        if (ganados != otra.ganados) {
            return Integer.compare(otra.ganados, ganados);
        }
        return Integer.compare(otra.getDiferenciaSets(), getDiferenciaSets());
    }
}
