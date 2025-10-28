package com.sistdist.servidorexclusionmutua;

import com.sistdist.interfaces.IClienteEM;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Servidor de exclusión mutua con soporte para elección de maestro
 */
public class ServerExclusionMutuaRMI extends UnicastRemoteObject implements IServicioExclusionMutua {

    private boolean token;
    private Queue<IClienteEM> clientes;
    private int idServidor;
    private boolean esMaestro;
    private DetectorFallo detector;
    
    public ServerExclusionMutuaRMI(int idServidor, boolean esMaestro) throws RemoteException {
        super();
        this.idServidor = idServidor;
        this.esMaestro = esMaestro;
        this.token = true;
        this.clientes = new ArrayDeque<>();
    }
    
    @Override
    public synchronized void ObtenerRecurso(IClienteEM cliente) throws RemoteException {
        System.out.println("[EM-" + idServidor + "] Solicitud de recurso recibida");
        
        if (token) {
            token = false;
            cliente.RecibirToken();
            System.out.println("[EM-" + idServidor + "] Token entregado inmediatamente");
        } else {
            clientes.add(cliente);
            System.out.println("[EM-" + idServidor + "] Cliente encolado. Cola: " + clientes.size());
        }
    }

    @Override
    public synchronized void DevolverRecurso() throws RemoteException {
        System.out.println("[EM-" + idServidor + "] Recurso devuelto");
        
        if (!clientes.isEmpty()) {
            IClienteEM cliente = clientes.poll();
            cliente.RecibirToken();
            System.out.println("[EM-" + idServidor + "] Token al siguiente cliente. Cola: " + clientes.size());
        } else {
            token = true;
            System.out.println("[EM-" + idServidor + "] Token disponible");
        }
    }

    @Override
    public int getIdServidor() throws RemoteException {
        return idServidor;
    }

    @Override
    public boolean estoyVivo() throws RemoteException {
        return true;
    }
    
    /**
     * Solicita voto para elección de nuevo maestro
     */
    @Override
    public int notificarFalloYSolicitarVoto() throws RemoteException {
        System.out.println("[EM-" + idServidor + "] Solicitud de voto recibida");
        
        // Solo participo si yo también detecté que el maestro cayó
        if (detector != null && detector.maestroCaido()) {
            System.out.println("[EM-" + idServidor + "] ✓ Confirmo fallo del maestro. Voto: " + idServidor);
            return idServidor;
        } else {
            System.out.println("[EM-" + idServidor + "] ✗ No he detectado fallo. No participo.");
            return -1;
        }
    }

    /**
     * Recibe anuncio del nuevo maestro elegido
     */
    @Override
    public void anunciarNuevoMaestro(int idNuevoMaestro) throws RemoteException {
        System.out.println("[EM-" + idServidor + "] 📢 NUEVO MAESTRO: ID " + idNuevoMaestro);
        
        if (idNuevoMaestro == this.idServidor) {
            System.out.println("[EM-" + idServidor + "] 👑 SOY EL NUEVO MAESTRO");
            this.esMaestro = true;
        } else {
            System.out.println("[EM-" + idServidor + "] Reconociendo nuevo maestro");
            this.esMaestro = false;
        }
        
        // Resetear detector
        if (detector != null) {
            detector.resetearFallos();
        }
    }

    // Getters y setters
    public DetectorFallo getDetector() {
        return detector;
    }

    public void setDetector(DetectorFallo detector) {
        this.detector = detector;
    }

    public boolean isEsMaestro() {
        return esMaestro;
    }

    public void setEsMaestro(boolean esMaestro) {
        this.esMaestro = esMaestro;
    }
}