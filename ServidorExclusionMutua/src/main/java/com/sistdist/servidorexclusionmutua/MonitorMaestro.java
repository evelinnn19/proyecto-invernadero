package com.sistdist.servidorexclusionmutua;

import com.sistdist.interfaces.IDetectorFalla;
import java.rmi.Naming;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hilo que monitorea el estado del servidor maestro.
 * Si detecta que cayó, inicia proceso de elección mediante votación.
 */
public class MonitorMaestro implements Runnable {
    
    private ServerExclusionMutuaRMI servidorRespaldo;
    private DetectorFallo detector;
    private boolean maestroActivo = true;
    private boolean eleccionEnProceso = false;
    
    
    private static final int[] idsServidores = {1, 2, 3, 4, 5};

    
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
                if (detector.maestroCaido() && !eleccionEnProceso) {
                   
                    System.out.println("MAESTRO CAÍDO DETECTADO");
                   
                    
                    eleccionEnProceso = true;
                    maestroActivo = false;
                    
                    // Iniciar proceso de elección
                    int ganador = realizarEleccion();
                    
                    if (ganador == servidorRespaldo.getIdServidor()) {
                        // Este servidor ganó la elección
                        activarComoMaestro();
                    } else {
                        System.out.println("Servidor ID " + ganador + " es el nuevo maestro");
                    }
                    
                    eleccionEnProceso = false;
                    
                } else if (!detector.maestroCaido() && !maestroActivo) {
                    // El maestro original se recuperó
                    System.out.println("Maestro original recuperado. Volviendo a modo pasivo.");
                    desactivarRespaldo();
                    maestroActivo = true;
                    detector.resetearFallos();
                }
                
            } catch (InterruptedException e) {
                System.out.println("Monitor interrumpido");
                break;
            } catch (Exception e) {
                System.err.println("Error en monitor: " + e.getMessage());
                Logger.getLogger(MonitorMaestro.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
    
    /**
     * Realiza el proceso de elección mediante votación.
     * Contacta a todos los servidores, recolecta votos, y determina el ganador.
     * @return ID del servidor ganador
     */
    private int realizarEleccion() throws RemoteException {
        System.out.println("INICIANDO PROCESO DE ELECCIÓN");
       
        
        List<Integer> participantes = new ArrayList<>();
        
        // Agregar mi propio ID como participante
        int miId = servidorRespaldo.getIdServidor();
        participantes.add(miId);
        System.out.println("Yo participo: ID " + miId);
        
        // Contactar a todos los demás servidores
        for (int id : idsServidores) {
            if (id == 1) {
                // Saltar el maestro caído
                System.out.println("Saltando servidor maestro caído (ID 1)");
                continue;
            }
            
            if (id == miId) {
                // Ya me agregué
                continue;
            }
            
            // Intentar contactar al servidor
            int voto = solicitarVoto(id);
            
            if (voto > 0) {
                participantes.add(voto);
                System.out.println("Servidor ID " + id + " participa");
            } else {
                System.out.println("Servidor ID " + id + " no participa o no responde");
            }
        }
        
        // Determinar ganador (ID más alto)
        int ganador = participantes.stream()
                                   .max(Integer::compareTo)
                                   .orElse(miId);
        
        
        System.out.println("Participantes: " + participantes);
        System.out.println("GANADOR: ID " + ganador);
       
        
        // Anunciar el resultado a todos
        anunciarGanador(ganador);
        
        return ganador;
    }
    
    /**
     * Solicita voto a un servidor específico
     * @param idServidor ID del servidor a contactar
     * @return ID del servidor si participa, -1 si no
     */
    private int solicitarVoto(int idServidor) {
        try {
            int puerto = 10000 + idServidor;
            String url = "rmi://localhost:" + puerto + "/servidorCentralEM";
            
            IServicioExclusionMutua servidor = (IServicioExclusionMutua) Naming.lookup(url);
            
            // Preguntar si detectó el fallo y quiere participar
            int voto = servidor.notificarFalloYSolicitarVoto();
            
            return voto;
            
        } catch (Exception e) {
            // Servidor no responde o no está disponible
            return -1;
        }
    }
    
    /**
     * Anuncia el ganador a todos los servidores participantes
     */
    private void anunciarGanador(int idGanador) {
        System.out.println("Anunciando ganador a todos los servidores...");
        
        for (int id : idsServidores) {
            if (id == 1) continue; // Saltar maestro caído
            
            try {
                int puerto = 10000 + id;
                String url = "rmi://localhost:" + puerto + "/servidorCentralEM";
                
                IServicioExclusionMutua servidor = (IServicioExclusionMutua) Naming.lookup(url);
                servidor.anunciarNuevoMaestro(idGanador);
                
                System.out.println("Anuncio enviado a servidor ID " + id);
                
            } catch (Exception e) {
                System.out.println(" No se pudo notificar a servidor ID " + id);
            }
        }
    }
    
    /**
     * Activa este servidor como el nuevo maestro
     */
    private void activarComoMaestro() {
try {
        System.out.println("ACTIVANDO COMO NUEVO MAESTRO ");
        // ==== publicar servicio de exclusión mutua en 10000 ====
        try {
            LocateRegistry.getRegistry(10000).list();
        } catch (RemoteException re) {
            LocateRegistry.createRegistry(10000);
            System.out.println("Registry creado en 10000");
        }
        Naming.rebind("rmi://localhost:10000/servidorCentralEM", servidorRespaldo);
        System.out.println("Rebind exitoso en 10000");

        // ==== publicar detector como ServidorMaestro en 9000 ====
        try {
            // Cambiar el detector a modo maestro
            DetectorFallo det = servidorRespaldo.getDetector();
            if (det != null) {
                det.setNombre("ServidorMaestro");
                det.setEsMaestro(true);
                try {
                    LocateRegistry.getRegistry(9000).list();
                } catch (RemoteException re2) {
                    LocateRegistry.createRegistry(9000);
                    System.out.println("Registry creado en 9000");
                }
                Naming.rebind("rmi://localhost:9000/ServidorMaestro", (IDetectorFalla) det);
                System.out.println("Detector rebind exitoso en 9000 como ServidorMaestro");
            } else {
                System.err.println("No se encontró DetectorFallo para publicar en 9000");
            }
        } catch (Exception ex) {
            System.err.println("Error publicando detector como ServidorMaestro en 9000: " + ex.getMessage());
            ex.printStackTrace();
        }
    } catch (Exception e) {
        System.err.println("Error al activar como maestro (bind): " + e.getMessage());
        e.printStackTrace();
    }
    }
    
    /**
     * Desactiva este servidor del rol de maestro
     */
    private void desactivarRespaldo() {
        try {
            System.out.println("Devolviendo control al maestro original...");
            Naming.unbind("rmi://localhost:10000/servidorCentralEM");
            System.out.println("Vuelvo a modo respaldo");
            
        } catch (Exception e) {
            System.err.println("Error al desactivar respaldo: " + e.getMessage());
        }
    }
}