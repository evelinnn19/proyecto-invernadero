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

    public Impresor getImp() {
        return imp;
    }

    public void setImp(Impresor imp) {
        this.imp = imp;
    }
    
    // Lock y conditions para control de concurrencia
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition puedeRegar = lock.newCondition();
    private final Condition puedeFertirregar = lock.newCondition();
    
    //impresor
    private Impresor imp;
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
        // Abrimos la electrovalvula general **después** de solicitar el recurso
            lock.unlock();
            iniciarRiegoGeneral();
        }
    } finally {
        // Este unlock corresponde al lock.lock() del inicio del método
        
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
            
            if (valvulasRiegoActivas <= 0) {
                lock.unlock();
                terminarRiegoGeneral();
                //Libera recurso remoto
                puedeFertirregar.signal(); // Notifica que se puede iniciar fertirrigación
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    
    //Riego general
    private void iniciarRiegoGeneral() {
        // Método ya está dentro del lock cuando se llama
        try{
            rmiCliente.solicitarRecurso();
            
        } 
        catch (RemoteException ex) {
            System.out.println("RMI no encontrado");
        } catch (InterruptedException ex) {
            Logger.getLogger(CoordinadorBomba.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
    //Enviar datos al impresor
            riegoGeneral = true;
            imp.setRiegoGeneral(riegoGeneral);
            System.out.println("Electrovalvula2 abierta (riego general)");
            if (outGeneral != null) outGeneral.println("ON");
        
    }
    }

    private void terminarRiegoGeneral(){
        // Método ya está dentro del lock cuando se llama
        try{
            rmiCliente.devolverRecurso();
            if (outGeneral != null) outGeneral.println("OFF");
            riegoGeneral = false;
            imp.setRiegoGeneral(riegoGeneral);
            System.out.println("Electrovalvula2 cerrada (riego general)");
            
        }catch(InterruptedException e){
            System.out.println("Se desconecto la electrovalvula");
        } catch (RemoteException ex) {
            System.out.println("Se desconecto el RMI");
        }
        
    }
    
    
    //Inicia fertirrigación
    public void iniciarFertirrigacion() throws InterruptedException {
            // 1) esperar condición dentro del lock
            lock.lock();
            try {
                while (valvulasRiegoActivas > 0 || riegoGeneral) {
                    System.out.println("Fertirrigacion en espera...");
                    //Puedo mandar al impresor ese mensaje
                    puedeFertirregar.await();
                }
                // ya cumplida la condición: liberamos lock para la llamada remota
            } finally {
                lock.unlock();
            }
            
             // 2) hacer la llamada RMI fuera del lock
            try {
                if (rmiCliente != null) rmiCliente.solicitarRecurso();
                else System.out.println("RMI no configurado");
            } catch (RemoteException ex) {
                System.out.println("Error RMI");
                // política: fallback o abortar
            }
    
            // 3) volver a tomar el lock para modificar estado compartido
            lock.lock();
            try {
                fertirrigando = true;
                imp.setFertirrigacion(fertirrigando);
                if (outferti != null) outferti.println("ON");
            } finally {
                lock.unlock();
            }
            

    }
    
    //termina fertirrigación
    public void terminaFertirrigacion() {
        lock.lock();
        try {
            
            try{
                lock.unlock();
                if(rmiCliente != null) rmiCliente.devolverRecurso();
                fertirrigando = false;
                imp.setFertirrigacion(fertirrigando);
            System.out.println("Fertirrigacion terminada");
            if (outferti != null) outferti.println("OFF");
            }catch(RemoteException ex){
                System.out.println("Error RMI al devolver recurso");
            } catch (InterruptedException ex) {
                System.out.println("Conexión con fertirrigación no encontrada");
            }
            puedeRegar.signal();// Notifica que se puede iniciar riego
        } finally {
            lock.unlock();
        }
    }
}