package ControladorCentral;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
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
                iniciarRiegoGeneral();
            }
        } finally {
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
            puedeRegar.signalAll(); // Notifica que se puede iniciar riego
        } finally {
            lock.unlock();
        }
    }
}