/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistdist.controlador;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sistdist.interfaces.IServerRMI;
import java.net.MalformedURLException;
import java.util.concurrent.Semaphore;

/**
 *
 * @author lesca
 */
public class HiloServerRMI extends Thread{
    
    IServerRMI server;
    
    public HiloServerRMI(IServerRMI s){
        server = s;
    }
    
    @Override
    public void run(){
        try {
            LocateRegistry.createRegistry(1099);
            Naming.rebind("rmi://localhost:1099/ServerRMI", server);                        
        } catch (RemoteException ex) {
            Logger.getLogger(HiloServerRMI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HiloServerRMI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
