package ControladorCentral;

import com.sistdist.interfaces.IClienteEM;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente RMI usado por el CoordinadorBomba para solicitar/soltar el recurso (bomba).
 */
public class RMIClienteBomba extends UnicastRemoteObject implements IClienteEM {

  private IServicioExclusionMutua servicio; // stub remoto actual
    private final Semaphore sem = new Semaphore(0); // inicio 0: bloqueado
    
    // URLs de los servidores
    private final String urlServer = "rmi://localhost:10000/servidorCentralEM";
    private boolean usandoMaestro = true;
    
    private static final int MAX_REINTENTOS = 3;

    public RMIClienteBomba() throws RemoteException, MalformedURLException, NotBoundException {
        super();
        conectarConFailover();
    }

    /**
     * Intenta conectar al maestro, si falla intenta con el respaldo.
     */
    private void conectarConFailover() throws RemoteException, MalformedURLException, NotBoundException {
        try {
            System.out.println("🔄 Intentando conectar con servidor maestro...");
            servicio = (IServicioExclusionMutua) Naming.lookup(urlServer);
            usandoMaestro = true;
            System.out.println("✅ Conectado al servidor MAESTRO (puerto 10000)");
            
        } catch (Exception e) {
            System.out.println("⚠️ Maestro no disponible, intentando con respaldo...");
            
            try {
                servicio = (IServicioExclusionMutua) Naming.lookup(urlServer);
                usandoMaestro = false;
                System.out.println("✅ Conectado al servidor RESPALDO (puerto 10001)");
                
            } catch (Exception ex) {
                System.err.println("❌ Ni maestro ni respaldo disponibles");
                throw new RemoteException("No hay servidores disponibles", ex);
            }
        }
    }

    /**
     * Intenta cambiar al otro servidor si el actual falla.
     */
    private boolean intentarFailover() {
        try {
           
            System.out.println("🔄 Intentando failover a servidor ");
            
            servicio = (IServicioExclusionMutua) Naming.lookup(urlServer);
            usandoMaestro = !usandoMaestro;
            
         
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failover falló: " + e.getMessage());
            return false;
        }
    }

    /**
     * Solicitar recurso con reintentos y failover automático.
     */
    public void solicitarRecurso() throws RemoteException, InterruptedException {
        int intentos = 0;
        
        while (intentos < MAX_REINTENTOS) {
            try {
                servicio.ObtenerRecurso(this);
                sem.acquire(); // Espera hasta que RecibirToken haga release()
                return; // Éxito
                
            } catch (RemoteException ex) {
                intentos++;
                System.err.println("⚠️ Error al solicitar recurso (intento " + intentos + "/" + MAX_REINTENTOS + "): " + ex.getMessage());
                
                if (intentos < MAX_REINTENTOS) {
                    // Intentar failover
                    if (intentarFailover()) {
                        // Reintentar con el nuevo servidor
                        continue;
                    } else {
                        // Esperar antes de reintentar
                        Thread.sleep(2000);
                    }
                } else {
                    throw new RemoteException("No se pudo obtener recurso después de " + MAX_REINTENTOS + " intentos", ex);
                }
            }
        }
    }

    /**
     * Devolver recurso con reintentos.
     */
    public void devolverRecurso() throws RemoteException, InterruptedException {
        int intentos = 0;
        
        while (intentos < MAX_REINTENTOS) {
            try {
                servicio.DevolverRecurso();
                System.out.println("✅ Recurso devuelto exitosamente");
                return;
                
            } catch (RemoteException ex) {
                intentos++;
                System.err.println("⚠️ Error al devolver recurso (intento " + intentos + "/" + MAX_REINTENTOS + "): " + ex.getMessage());
                
                if (intentos < MAX_REINTENTOS) {
                    // Intentar failover
                    if (intentarFailover()) {
                        continue;
                    } else {
                        Thread.sleep(2000);
                    }
                } else {
                    throw new RemoteException("No se pudo devolver recurso después de " + MAX_REINTENTOS + " intentos", ex);
                }
            }
        }
    }

    /**
     * Callback invocado remotamente por el servidor cuando el token está disponible.
     */
    @Override
    public void RecibirToken() throws RemoteException {
        if (sem.availablePermits() == 0) {
            sem.release(); // Evita acumulación
        }
        System.out.println("🔑 Token recibido del servidor de exclusión mutua");
    }
    
    /**
     * Obtiene el nombre del servidor actual (para debugging).
     */
    public String getServidorActual() {
        return usandoMaestro ? "MAESTRO (10000)" : "RESPALDO (10001)";
    }
}
