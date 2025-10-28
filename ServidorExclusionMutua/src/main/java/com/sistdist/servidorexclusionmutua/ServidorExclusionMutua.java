package com.sistdist.servidorexclusionmutua;

import com.sistdist.interfaces.IDetectorFalla;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor de Exclusión Mutua con sistema híbrido de detección y elección.
 * 
 * Características:
 * - Detección de fallos con DetectorFallo (tu sistema original)
 * - Elección mediante votación cuando se detecta fallo
 * - El servidor con ID más alto se convierte en maestro
 * 
 * Uso:
 *   java ServidorExclusionMutua <ID>
 * 
 * Ejemplos:
 *   java ServidorExclusionMutua 1  (Maestro inicial)
 *   java ServidorExclusionMutua 2  (Respaldo)
 */
public class ServidorExclusionMutua {
    
    public static void main(String[] args) {
        
        // Validar argumentos
        if (args.length < 1) {
            System.err.println("Error: Debe proporcionar el ID del servidor");
            System.err.println("Uso: java ServidorExclusionMutua <ID>");
            System.err.println("Ejemplo: java ServidorExclusionMutua 1");
            System.exit(1);
        }
        
        int idServidor = 0;
        try {
            idServidor = Integer.parseInt(args[0]);
            if (idServidor < 1) {
                throw new NumberFormatException("El ID debe ser mayor o igual a 1");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: ID inválido - " + e.getMessage());
            System.exit(1);
        }
        
        // Determinar si es maestro o respaldo
        String nombreServidor;
        boolean esMaestro;
        int nroPuerto;
        int nroPuertoExcMutua;
        
        if (idServidor == 1) {
            esMaestro = true;
            nombreServidor = "ServidorMaestro";
            nroPuerto = 9000;
            nroPuertoExcMutua = 10000;
            
          
            System.out.println("SERVIDOR MAESTRO INICIANDO");
  
        } else {
            esMaestro = false;
            nombreServidor = "ServidorRespaldo" + idServidor;
            nroPuerto = 9000 + idServidor;
            nroPuertoExcMutua = 10000 + idServidor;
            
          
            System.out.println("SERVIDOR RESPALDO INICIANDO");
           
        }
        
        System.out.println("ID: " + idServidor);
        System.out.println("Puerto Detector: " + nroPuerto);
        System.out.println("Puerto Exclusión Mutua: " + nroPuertoExcMutua);
        System.out.println();
        
        Timer planificador = new Timer();
        
        try {
            // ========== CONFIGURAR DETECTOR DE FALLOS ==========
            LocateRegistry.createRegistry(nroPuerto);
            DetectorFallo detector = new DetectorFallo();
            detector.setNombre(nombreServidor);
            detector.setEsMaestro(esMaestro);
            
            Naming.rebind("rmi://localhost:" + nroPuerto + "/" + nombreServidor, 
                         (IDetectorFalla) detector);
            
            System.out.println("Detector de fallos configurado");
            
            // ========== CONFIGURAR TAREAS DE MONITOREO ==========
            // Solo los respaldos monitorean al maestro
            if (!esMaestro) {
                AveriguarEstado tareaAveriguarEstado = new AveriguarEstado(detector);
                ControlRespuesta tareaControlRespuesta = new ControlRespuesta(detector);
                
                planificador.schedule(tareaAveriguarEstado, 0, 3000); // Cada 3s
                planificador.schedule(tareaControlRespuesta, 0, 5000); // Cada 5s
                
                System.out.println("Tareas de monitoreo programadas");
            }
            
            // ========== CONFIGURAR SERVIDOR DE EXCLUSIÓN MUTUA ==========
            LocateRegistry.createRegistry(nroPuertoExcMutua);
            ServerExclusionMutuaRMI serverEM = new ServerExclusionMutuaRMI(idServidor, esMaestro);
            serverEM.setDetector(detector);
            
            Naming.rebind("rmi://localhost:" + nroPuertoExcMutua + "/servidorCentralEM", 
                         (IServicioExclusionMutua) serverEM);
            
            System.out.println("Servicio de Exclusión Mutua registrado");
            
            // ========== INICIAR MONITOR DE MAESTRO (SOLO RESPALDOS) ==========
            if (!esMaestro) {
                Thread hiloMonitoreo = new Thread(new MonitorMaestro(serverEM, detector));
                hiloMonitoreo.setDaemon(false);
                hiloMonitoreo.start();
                
                System.out.println("✓ Monitor de maestro iniciado");
            }
            
            System.out.println("SERVIDOR OPERATIVO");
            if (esMaestro) {
                System.out.println("   Estado: ACTIVO (Maestro)");
                System.out.println("   Los clientes se conectan al puerto 10000");
            } else {
                System.out.println("   Estado: PASIVO (Respaldo)");
                System.out.println("   Monitoreando maestro en puerto 9000");
            }
            System.out.println("════════════════════════════════════════\n");
            
            // Mantener el servidor vivo
            Thread.currentThread().join();
            
        } catch (RemoteException ex) {
            System.err.println("Error RMI: " + ex.getMessage());
            Logger.getLogger(ServidorExclusionMutua.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            System.err.println("URL malformada: " + ex.getMessage());
            Logger.getLogger(ServidorExclusionMutua.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            System.err.println("Servidor interrumpido");
            Logger.getLogger(ServidorExclusionMutua.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            planificador.cancel();
            System.out.println("Servidor finalizado");
        }
    }
}