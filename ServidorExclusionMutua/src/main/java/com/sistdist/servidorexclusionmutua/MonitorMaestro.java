package com.sistdist.servidorexclusionmutua;

import java.rmi.Naming;
import com.sistdist.interfaces.IDetectorFalla;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hilo que monitorea constantemente el estado del servidor maestro.
 * Si detecta que cayó, activa el servidor respaldo.
 */
public class MonitorMaestro implements Runnable {
    
    private ServerExclusionMutuaRMI servidorRespaldo;
    private DetectorFallo detector;
    private boolean maestroActivo = true;
    
    public MonitorMaestro(ServerExclusionMutuaRMI servidor, DetectorFallo detector) {
        this.servidorRespaldo = servidor;
        this.detector = detector;
    }
    
    @Override
    public void run() {
        System.out.println("Monitor del Maestro iniciado");
        
        while (true) {
            try {
                Thread.sleep(5000); // Verificar cada 5 segundos
                
                // Verificar estado del maestro
                if (detector.maestroCaido() && maestroActivo) {
                    System.out.println("MAESTRO CAÍDO DETECTADO!");
                    System.out.println("ACTIVANDO SERVIDOR DE RESPALDO...");
                    
                    activarRespaldo();
                    maestroActivo = false;
                    
                } else if (!detector.maestroCaido() && !maestroActivo) {
                    // El maestro se recuperó
                    System.out.println("Maestro recuperado. Volviendo a modo pasivo.");
                    desactivarRespaldo();
                    maestroActivo = true;
                }
                
            } catch (InterruptedException e) {
                System.out.println("Monitor interrumpido");
                break;
            }
        }
    }
    
    private void activarRespaldo() {
        try {
            // Registrar el respaldo en la dirección del maestro
            System.out.println("Registrando respaldo como servidor principal...");
            
            // El respaldo ahora responde en el puerto del maestro (10000)
            Naming.rebind("rmi://localhost:10000/servidorCentralEM", servidorRespaldo);
            
            System.out.println("SERVIDOR DE RESPALDO AHORA ES EL MAESTRO");
            System.out.println("Aceptando conexiones en puerto 10000");
            
        } catch (Exception e) {
            System.err.println("Error al activar respaldo: " + e.getMessage());
            Logger.getLogger(MonitorMaestro.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    private void desactivarRespaldo() {
        try {
            // Quitar el registro del respaldo del puerto del maestro
            System.out.println("Devolviendo control al maestro original...");
            Naming.unbind("rmi://localhost:10000/servidorCentralEM");
            System.out.println("Respaldo vuelve a modo pasivo");
            
        } catch (Exception e) {
            System.err.println("Error al desactivar respaldo: " + e.getMessage());
        }
    }
}