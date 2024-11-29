import javax.swing.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int cantidadProcesos = obtenerEntradaValida("Ingrese el total de procesos:", 1, Integer.MAX_VALUE);
        int quantum = obtenerEntradaValida("Ingrese el valor del quantum en milisegundos", 1, Integer.MAX_VALUE);
        int cambioContexto = obtenerEntradaValida("Ingrese el valor del tiempo de intercambio en milisegundos", 1, Integer.MAX_VALUE);
        List<Proceso> procesos = new ArrayList<>();
        for (int i = 0; i < cantidadProcesos; i++) {
            int tiempoLlegada = obtenerEntradaValida("Ingrese el tiempo de llegada del proceso " + (i) + " (en ms):", 0, Integer.MAX_VALUE);

            int tiempoCPUEnQuantums = obtenerEntradaValida("Ingrese el tiempo total necesario en CPU (en número de quantums) del proceso " + (i) + ":", 1, Integer.MAX_VALUE);
            int tiempoCPU = tiempoCPUEnQuantums * quantum;

            int fasesES = obtenerEntradaValida("Ingrese la cantidad de fases de entrada/salida (E/S) del proceso " + (i) + ":", 0, Integer.MAX_VALUE);
            List<Integer> tiempos = new ArrayList<>();

            tiempos.add(tiempoCPU);


            for (int j = 0; j < fasesES; j++) {
                int tiempoESEnQuantums = obtenerEntradaValida(
                        "Ingrese el tiempo de E/S de la fase " + (j + 1) + " del proceso " + (i + 1) + " (en número de quantums):", 1, Integer.MAX_VALUE);
                int tiempoES = tiempoESEnQuantums * quantum;
                tiempos.add(tiempoES);

                int cpuPostESEnQuantums = obtenerEntradaValida(
                        "Ingrese el tiempo de CPU necesario después de la E/S " + (j + 1) + " del proceso " + (i + 1) + " (en número de quantums):", 1, Integer.MAX_VALUE);
                int cpuPostES = cpuPostESEnQuantums * quantum;
                tiempos.add(cpuPostES);
            }

            procesos.add(new Proceso(i, tiempoLlegada, tiempos));
        }

        simularRoundRobin(procesos, quantum, cambioContexto);
    }

    private static void simularRoundRobin(List<Proceso> procesos, int quantum, int cambioContexto) {
        StringBuilder diagramaGantt = new StringBuilder("Diagrama de Gantt:\n");
        Queue<Proceso> colaListos = new LinkedList<>();
        Queue<Proceso> colaBloqueados = new LinkedList<>();
        int tiempoActual = 0;
        int tiempoTotalVuelta = 0, tiempoTotalEspera = 0;

        procesos.sort(Comparator.comparingInt(p -> p.tiempoLlegada));
        List<Proceso> procesosCompletados = new ArrayList<>();
        boolean ban1 = false;
        while (!procesos.isEmpty() || !colaListos.isEmpty() || !colaBloqueados.isEmpty()) {

            if(ban1){
                mostrarColaListosConGantt(colaListos, diagramaGantt.toString());
            }else{
                ban1 = true;
            }

            Iterator<Proceso> iterBloqueados = colaBloqueados.iterator();
            while (iterBloqueados.hasNext()) {
                Proceso proceso = iterBloqueados.next();
                if (proceso.tiempoActualBloqueo <= tiempoActual) {
                    colaListos.add(proceso);
                    iterBloqueados.remove();
                }
            }

            while (!procesos.isEmpty() && procesos.get(0).tiempoLlegada <= tiempoActual) {
                colaListos.add(procesos.remove(0));
            }

            if (colaListos.isEmpty() && procesos.isEmpty() && colaBloqueados.isEmpty()) {
                break;
            }

            if (!colaListos.isEmpty()) {
                Proceso procesoActual = colaListos.poll();

                if (tiempoActual == 0) {
                    if(procesoActual.primerEjecucion){
                        procesoActual.tiempoEspera = 0;
                        procesoActual.primerEjecucion = false;
                    }

                } else {
                    if(procesoActual.primerEjecucion){
                        procesoActual.tiempoEspera = tiempoActual - procesoActual.tiempoLlegada;
                        procesoActual.primerEjecucion = false;
                    }
                }

                diagramaGantt.append("[P").append(procesoActual.id).append("] ");

                int tiempoEjecutado = quantum;
                if (procesoActual.faseActual < procesoActual.tiempos.size()) {
                    procesoActual.tiempos.set(procesoActual.faseActual, procesoActual.tiempos.get(procesoActual.faseActual) - tiempoEjecutado) ;
                }

                tiempoActual += tiempoEjecutado;
                tiempoActual += cambioContexto;

                if (procesoActual.tiempos.get(procesoActual.faseActual) == 0) {
                    procesoActual.faseActual++;
                    if (procesoActual.faseActual < procesoActual.tiempos.size()) {
                        if (procesoActual.faseActual % 2 == 0) {
                            colaListos.add(procesoActual);
                        } else {
                            int tiempoBloqueo = procesoActual.tiempos.get(procesoActual.faseActual);
                            procesoActual.tiempoActualBloqueo = tiempoActual + tiempoBloqueo +cambioContexto;
                            procesoActual.faseActual++;
                            colaBloqueados.add(procesoActual);
                        }
                    } else {
                        // Proceso completado
                        procesoActual.tiempoTotalES = calcularTiempoES(procesoActual.tiempos);
                        procesoActual.tiempoVuelta = tiempoActual - procesoActual.tiempoLlegada - procesoActual.tiempoTotalES - cambioContexto;
                        procesosCompletados.add(procesoActual);
                        tiempoTotalVuelta += procesoActual.tiempoVuelta;
                        tiempoTotalEspera += procesoActual.tiempoEspera;
                    }
                } else {

                    while (!procesos.isEmpty() && procesos.getFirst().tiempoLlegada <= tiempoActual) {
                        colaListos.add(procesos.removeFirst());
                    }

                    if (procesoActual.tiempos.get(procesoActual.faseActual) > 0) {
                        boolean bandera = true;
                        Iterator<Proceso> iterBloqueados2 = colaBloqueados.iterator();
                        Proceso procesoDesbloqueado;
                        while (bandera) {
                            try {
                                procesoDesbloqueado = iterBloqueados2.next();
                                if (procesoDesbloqueado.tiempoActualBloqueo <= tiempoActual) {
                                    if(procesoDesbloqueado.tiempoActualBloqueo < tiempoActual) {
                                        colaListos.add(procesoDesbloqueado);
                                        colaListos.add(procesoActual);
                                        iterBloqueados2.remove();
                                        bandera = false;
                                    }else{
                                        if(!colaListos.contains(procesoActual)){
                                            colaListos.add(procesoActual);
                                            bandera = false;
                                        }
                                    }
                                }
                            }catch (Exception e){
                                colaListos.add(procesoActual);
                                bandera = false;
                            };

                        }
                    }
                }
            }
        }

        mostrarResultados(procesosCompletados, tiempoTotalVuelta, tiempoTotalEspera, diagramaGantt.toString());
    }

    private static void mostrarColaListosConGantt(Queue<Proceso> colaListos, String diagramaGantt) {
        if (colaListos.isEmpty()) {
            JOptionPane.showMessageDialog(null, "La cola de listos está vacía.", "Cola de Listos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder mensaje = new StringBuilder("Procesos en la cola de listos:\n\n");
        mensaje.append(String.format("%-10s%-15s\n", "Proceso", "Tiempo Restante"));

        Queue<Proceso> copiaCola = new LinkedList<>(colaListos);
        for (Proceso proceso : copiaCola) {
            int tiempoRestante = proceso.faseActual < proceso.tiempos.size()
                    ? proceso.tiempos.get(proceso.faseActual)
                    : 0;
            mensaje.append(String.format("%-10s%-15d\n", "P" + proceso.id, tiempoRestante));
        }

        mensaje.append(diagramaGantt).append("\n");

        JOptionPane.showMessageDialog(null, mensaje.toString(), "Cola de Listos y Diagrama de Gantt", JOptionPane.INFORMATION_MESSAGE);
    }



    private static int calcularTiempoES(List<Integer> tiempos) {
        int tiempoTotalES = 0;
        for (int i = 1; i < tiempos.size(); i += 2) {
            tiempoTotalES += tiempos.get(i);
        }
        return tiempoTotalES;
    }

    private static void mostrarResultados(List<Proceso> procesos, int tiempoTotalVuelta, int tiempoTotalEspera, String diagramaGantt) {
        StringBuilder resultados = new StringBuilder();
        resultados.append("Resultados de la simulación:\n\n");
        resultados.append(diagramaGantt).append("\n\n");

        resultados.append(String.format("%-10s%-15s%-15s\n", "Proceso", "Tiempo Vuelta", "Tiempo Espera"));
        for (Proceso p : procesos) {
            resultados.append(String.format("%-10s%-15d%-15d\n", "P" + p.id, p.tiempoVuelta, p.tiempoEspera));
        }

        resultados.append("\nPromedio Tiempo de Vuelta: ").append((double) tiempoTotalVuelta / procesos.size());
        resultados.append("\nPromedio Tiempo de Espera: ").append((double) tiempoTotalEspera / procesos.size());

        JOptionPane.showMessageDialog(null, resultados.toString(), "Resultados Round Robin", JOptionPane.INFORMATION_MESSAGE);
    }

    private static int obtenerEntradaValida(String mensaje, int min, int max) {
        while (true) {
            try {
                String entrada = JOptionPane.showInputDialog(null, mensaje);
                if (entrada == null) {
                    System.exit(0);
                }
                int valor = Integer.parseInt(entrada);
                if (valor >= min && valor <= max) {
                    return valor;
                } else {
                    JOptionPane.showMessageDialog(null, "Por favor, ingrese un número dentro del rango.");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Por favor, ingrese un número válido.");
            }
        }
    }


}