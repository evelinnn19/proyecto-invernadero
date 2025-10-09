package ControladorCentral;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alejandro
 */
public class CoordinadorBomba {
    private int valvulasRiegoActivas = 0;
    private boolean fertirrigando = false;
    private boolean riegoGeneral = false;
    private PrintWriter outGeneral; // para enviar a EV2
    private PrintWriter outferti;
    
    // Lock y conditions para control de concurrencia
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition puedeRegar = lock.newCondition();
    private final Condition puedeFertirregar = lock.newCondition();
    
    
    //Campo para cliente RMI
    private RMIClienteBomba rmiCliente;
    
    //setter de Cliente RMI
    public void setRmiCliente(RMIClienteBomba cliente){
        this.rmiCliente = cliente;
    }
    
    
    
    
    //Inicia un riego (si se esta fertirrigando, lo bloquea)
   public void iniciarRiego(int parcela) throws InterruptedException {
    lock.lock();
    try {
        while (fertirrigando) {
            puedeRegar.await();
        }

        valvulasRiegoActivas++;
        System.out.println("Se inicio el riego en parcela " + parcela);
        System.out.println(valvulasRiegoActivas + " Valvulas de riego activas");

        if (valvulasRiegoActivas == 1) {
            // Vamos a solicitar la bomba remotamente. No mantenemos el lock durante la llamada RMI
            lock.unlock(); // liberamos para no bloquear otros hilos mientras esperamos al servidor RMI
            try {
                if (rmiCliente != null) {
                    try {
                        // puede lanzar RemoteException o InterruptedException
                        rmiCliente.solicitarRecurso();
                        // si retorna sin excepción, tenemos el token (o se aplicó la política de espera)
                    } catch (RemoteException rex) {
                        System.err.println("Error RMI al solicitar recurso: " + rex.getMessage());
                        // Política: decidimos continuar en modo local (fallback) o podríamos reintentar.
                    }
                } else {
                    System.out.println("RMI no inicializado - procediendo en modo local.");
                }
            } finally {
                // Nos aseguramos de volver a tomar el lock pase lo que pase (excepción incluida)
                lock.lock();
            }

            // Abrimos la electrovalvula general **después** de solicitar el recurso
            iniciarRiegoGeneral();
        }
    } finally {
        // Este unlock corresponde al lock.lock() del inicio del método
        lock.unlock();
    }
}

    
    
    //Comunicación valvula general
    public void setElectrovalvulaGeneral(Socket cliente) {
        lock.lock();
        try {
            this.outGeneral = new PrintWriter(cliente.getOutputStream(), true);
        } catch (IOException e) {
            // Manejo de error
        } finally {
            lock.unlock();
        }
    }
    
    
    
    //Comunicación Valvula Ferti
    public void setElectroValvulaFerti(Socket cliente) {
        lock.lock();
        try {
            this.outferti = new PrintWriter(cliente.getOutputStream(), true);
        } catch (IOException ex) {
            Logger.getLogger(CoordinadorBomba.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            lock.unlock();
        }
    }
    
    //Termina el riego
    public void terminarRiego(int parcela) {
        lock.lock();
        try {
            valvulasRiegoActivas--;
            System.out.println("La parcela " + parcela + " termino el riego");
            System.out.println(valvulasRiegoActivas + " Valvulas de riego activas");
            
            if (valvulasRiegoActivas == 0) {
                terminarRiegoGeneral();
                //Libera recurso remoto
                try{
                    if(rmiCliente != null) rmiCliente.devolverRecurso();
                }catch (RemoteException ex){
                    System.out.println("Error RMI al devolver recurso");
                }
                puedeFertirregar.signalAll(); // Notifica que se puede iniciar fertirrigación
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    
    //Riego general
    private void iniciarRiegoGeneral() {
        // Método ya está dentro del lock cuando se llama
        riegoGeneral = true;
        System.out.println("Electrovalvula2 abierta (riego general)");
        if (outGeneral != null) outGeneral.println("ON");
    }

    private void terminarRiegoGeneral() {
        // Método ya está dentro del lock cuando se llama
        riegoGeneral = false;
        System.out.println("Electrovalvula2 cerrada (riego general)");
        if (outGeneral != null) outGeneral.println("OFF");
    }
    
    
    //Inicia fertirrigación
    public void iniciarFertirrigacion() throws InterruptedException {
        lock.lock();
        try {
            while (valvulasRiegoActivas > 0 || riegoGeneral) {
                System.out.println("Fertirrigacion en espera...");
                puedeFertirregar.await(); // espera hasta que nadie esté regando
            }
            lock.unlock();
            try{
                if(rmiCliente != null){
                    rmiCliente.solicitarRecurso();
                }else{
                    System.out.println("RMI no configurado");
                }
            }catch(RemoteException ex){
                System.out.println("Error RMI");
            }
            fertirrigando = true;
            System.out.println("Bomba asignada a FERTIRRIGACIÓN");
            if (outferti != null) outferti.println("ON");
        } finally {
            lock.unlock();
        }
    }
    
    //termina fertirrigación
    public void terminaFertirrigacion() {
        lock.lock();
        try {
            fertirrigando = false;
            System.out.println("Fertirrigacion terminada");
            if (outferti != null) outferti.println("OFF");
            try{
                if(rmiCliente != null) rmiCliente.devolverRecurso();
            }catch(RemoteException ex){
                System.out.println("Error RMI al devolver recurso");
            }
            puedeRegar.signalAll(); // Notifica que se puede iniciar riego
        } finally {
            lock.unlock();
        }
    }
}