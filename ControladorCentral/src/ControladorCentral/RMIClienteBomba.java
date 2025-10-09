package ControladorCentral;

import com.sistdist.interfaces.IClienteEM;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CountDownLatch;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;

/**
 * Cliente RMI usado por el CoordinadorBomba para solicitar/soltar el recurso (bomba).
 */
public class RMIClienteBomba extends UnicastRemoteObject implements IClienteEM {

    private IServicioExclusionMutua servicio; // stub remoto
    private CountDownLatch latch; // para esperar al token

    public RMIClienteBomba(String servidorUrl) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        // Ej: servidorUrl = "rmi://localhost:10000/servidorCentralEM"
        servicio = (IServicioExclusionMutua) Naming.lookup(servidorUrl);
    }

    /**
     * Llamar antes de entrar en sección crítica (antes de abrir electrovalvula general o asignar bomba).
     * Bloquea hasta que RecibirToken sea invocado por el servidor.
     */
    public void solicitarRecurso() throws RemoteException, InterruptedException {
        latch = new CountDownLatch(1);
        servicio.ObtenerRecurso(this);
        // Espera hasta que RecibirToken() haga countDown
        latch.await();
    }

    /**
     * Llamar al salir de la sección crítica.
     */
    public void devolverRecurso() throws RemoteException {
        servicio.DevolverRecurso();
    }

    /**
     * Callback invocado remotamente por el servidor cuando el token está disponible.
     * Desbloquea al hilo que esperaba.
     */
    @Override
    public void RecibirToken() throws RemoteException {
        if (latch != null) {
            latch.countDown();
        }
        System.out.println("RMIClienteBomba: Recibí el token del servidor de exclusión mutua.");
    }
}
