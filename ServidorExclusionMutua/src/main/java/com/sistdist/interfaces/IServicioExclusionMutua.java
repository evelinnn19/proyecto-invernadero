package com.sistdist.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interfaz extendida con métodos para elección de maestro
 */
public interface IServicioExclusionMutua extends Remote {
    
    // Métodos originales
    public void ObtenerRecurso(IClienteEM cliente) throws RemoteException;
    public void DevolverRecurso() throws RemoteException;
    
    // Nuevos métodos para elección
    
    /**
     * Notifica a un servidor que el maestro cayó y solicita su voto
     * @return ID del servidor si participa en la elección, -1 si no detectó fallo
     * @throws RemoteException
     */
    public int notificarFalloYSolicitarVoto() throws RemoteException;
    
    /**
     * Anuncia que este servidor es el nuevo maestro
     * @param idNuevoMaestro ID del servidor que ganó la elección
     * @throws RemoteException
     */
    public void anunciarNuevoMaestro(int idNuevoMaestro) throws RemoteException;
    
    /**
     * Obtiene el ID de este servidor
     * @return ID del servidor
     * @throws RemoteException
     */
    public int getIdServidor() throws RemoteException;
    
    /**
     * Verifica si este servidor está activo
     * @return true si está activo
     * @throws RemoteException
     */
    public boolean estoyVivo() throws RemoteException;
}