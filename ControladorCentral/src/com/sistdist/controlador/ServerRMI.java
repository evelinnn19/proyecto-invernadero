/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistdist.controlador;

import com.sistdist.interfaces.IClienteEM;
import com.sistdist.interfaces.ISensorTemperatura;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import com.sistdist.interfaces.IServerRMI;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lesca
 */
public class ServerRMI extends UnicastRemoteObject implements IServerRMI, IClienteEM{
    
    double temperatura;
    boolean tengoToken;
    IServicioExclusionMutua serverEM;
    Semaphore semaforo;
    
    public ServerRMI(IServicioExclusionMutua server, Semaphore sem) throws RemoteException{
        super();
        tengoToken = false;
        serverEM = server;
        semaforo = sem;
    }

    public double getTemperatura() {
        return temperatura;
    }
    
    

    @Override
    public void DameTemperatura(double temperatura, ISensorTemperatura servidorTemperatura) throws RemoteException {
        this.temperatura = temperatura;
        servidorTemperatura.Saludar("Gracias");
    }

    @Override
    public void RecibirToken() throws RemoteException {
        tengoToken = true;
        semaforo.release();
        
    }
    
    public void DevolverToken(){
        tengoToken = false;
        try {
            serverEM.DevolverRecurso();
        } catch (RemoteException ex) {
            Logger.getLogger(ServerRMI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void PedirRecurso(){
        try {
            serverEM.ObtenerRecurso(this);
        } catch (RemoteException ex) {
            Logger.getLogger(ServerRMI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
