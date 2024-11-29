import java.util.List;

public class Proceso {
    int id;
    int tiempoLlegada;
    List<Integer> tiempos; // Contiene tiempos de CPU y E/S
    int faseActual;
    int tiempoEspera;
    int tiempoVuelta;
    int tiempoTotalES; // Tiempo total de E/S
    int tiempoActualBloqueo;
    boolean primerEjecucion;

    Proceso(int id, int tiempoLlegada, List<Integer> tiempos) {
        this.id = id;
        this.tiempoLlegada = tiempoLlegada;
        this.tiempos = tiempos;
        this.faseActual = 0;
        this.tiempoEspera = 0;
        this.tiempoVuelta = 0;
        this.tiempoTotalES = 0;
        this.primerEjecucion = true;
    }
}