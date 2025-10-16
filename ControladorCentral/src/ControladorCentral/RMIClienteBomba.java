package ControladorCentral;

import com.sistdist.interfaces.IClienteEM;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Cliente RMI usado por el CoordinadorBomba para solicitar/soltar el recurso (bomba).
 */
public class RMIClienteBomba extends UnicastRemoteObject implements IClienteEM {

   private IServicioExclusionMutua servicio; // stub remoto
    private final Semaphore sem = new Semaphore(0); // inicio 0: bloqueado

    public RMIClienteBomba(String servidorUrl) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        // Ej: servidorUrl = "rmi://localhost:10000/servidorCentralEM"
        servicio = (IServicioExclusionMutua) Naming.lookup(servidorUrl);
    }

    /**
     * Llamar antes de entrar en sección crítica (antes de abrir electrovalvula general o asignar bomba).Bloquea hasta que RecibirToken sea invocado por el servidor.
     * @throws java.rmi.RemoteException
     * @throws java.lang.InterruptedException
     */
    public void solicitarRecurso() throws RemoteException, InterruptedException {
         servicio.ObtenerRecurso(this);
        sem.acquire();
    }

    /**
     * Llamar al salir de la sección crítica.
     * @throws java.rmi.RemoteException
     * @throws java.lang.InterruptedException
     */
    public void devolverRecurso() throws RemoteException, InterruptedException {
        servicio.ObtenerRecurso(this);
        sem.acquire(); // espera hasta que RecibirToken haga release()
    }
    
    

    /**
     * Callback invocado remotamente por el servidor cuando el token está disponible.Desbloquea al hilo que esperaba.
     * @throws java.rmi.RemoteException
     */
    @Override
    public void RecibirToken() throws RemoteException {
        if (sem.availablePermits() == 0) sem.release(); // evita acumulación
        System.out.println("RMIClienteBomba: Recibí el token del servidor de exclusión mutua.");
    }
}
