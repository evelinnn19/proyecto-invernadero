/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.sistdist.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author lesca
 */
public interface IServerRMI extends Remote {
    void DameTemperatura(double temperatura, ISensorTemperatura servidorTemperatura) throws RemoteException;
}
