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
 * Servidor de Exclusión Mutua con soporte para servidor de respaldo.
 * El servidor respaldo monitorea al maestro y toma su lugar si falla.
 */
public class ServidorExclusionMutua{

    public static void main(String[] args) {
        
        // Determinar si es maestro o respaldo según argumento
        boolean esMaestro = true;
        String nombreServidor = "ServidorMaestro";
        int nroPuerto = 9000;
        int nroPuertoExcMutua = 10000;
        
        if (args.length > 0 && args[0].equalsIgnoreCase("respaldo")) {
            esMaestro = false;
            nombreServidor = "ServidorRespaldo";
            nroPuerto = 9001;
            nroPuertoExcMutua = 10001;
            System.out.println("INICIANDO COMO SERVIDOR DE RESPALDO");
        } else {
            System.out.println("INICIANDO COMO SERVIDOR MAESTRO");
        }
        
        Timer planificador = new Timer();
        
        try {            
            // Configurar detector de fallos
            LocateRegistry.createRegistry(nroPuerto);
            DetectorFallo detector = new DetectorFallo();
            detector.setNombre(nombreServidor);
            detector.setEsMaestro(esMaestro);
            
            Naming.rebind("rmi://localhost:" + nroPuerto + "/" + nombreServidor, 
                         (IDetectorFalla) detector);
            
            // Configurar tareas de monitoreo
            AveriguarEstado tareaAveriguarEstado = new AveriguarEstado(detector);
            ControlRespuesta tareaControlRespuesta = new ControlRespuesta(detector);
            
            // Si es respaldo, monitorear al maestro
            if (!esMaestro) {
                planificador.schedule(tareaAveriguarEstado, 0, 3000); // Cada 3s
                planificador.schedule(tareaControlRespuesta, 0, 5000); // Cada 5s
            }
            
            // Configurar servidor de exclusión mutua
            LocateRegistry.createRegistry(nroPuertoExcMutua);
            ServerExclusionMutuaRMI serverEM = new ServerExclusionMutuaRMI();
            serverEM.setNombreServidor(nombreServidor);
            
            Naming.rebind("rmi://localhost:" + nroPuertoExcMutua + "/servidorCentralEM", 
                         (IServicioExclusionMutua) serverEM);
            
            System.out.println("Servidor configurado en puertos " + nroPuerto + "/" + nroPuertoExcMutua);
            System.out.println("Estado: " + (esMaestro ? "ACTIVO (Maestro)" : "PASIVO (Respaldo)"));
            
            // Si es respaldo, iniciar monitoreo continuo
            if (!esMaestro) {
                Thread hiloMonitoreo = new Thread(new MonitorMaestro(serverEM, detector));
                hiloMonitoreo.setDaemon(false);
                hiloMonitoreo.start();
            }
            
            System.out.println("Finalización de configuración.");
            
        } catch (RemoteException ex) {
            Logger.getLogger(ServidorExclusionMutua.class.getName()).log(Level.SEVERE, 
                "Error RMI", ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(ServidorExclusionMutua.class.getName()).log(Level.SEVERE, 
                "URL malformada", ex);
        }
    }
}