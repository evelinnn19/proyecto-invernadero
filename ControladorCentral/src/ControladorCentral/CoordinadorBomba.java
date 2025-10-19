package ControladorCentral;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoordinadorBomba {
    private int valvulasRiegoActivas = 0;
    private boolean fertirrigando = false;
    private boolean riegoGeneral = false;
    private PrintWriter outGeneral; // para enviar a EV2
    private PrintWriter outferti;

    private volatile Impresor imp;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition puedeRegar = lock.newCondition();
    private final Condition puedeFertirregar = lock.newCondition();

    private volatile RMIClienteBomba rmiCliente;

    public Impresor getImp() {
        return imp;
    }

    public void setImp(Impresor imp) {
        this.imp = imp;
        this.imp.setFertirrigacion(fertirrigando);
        this.imp.setRiegoGeneral(riegoGeneral);
    }

    public void setRmiCliente(RMIClienteBomba cliente){
        this.rmiCliente = cliente;
    }

    // Inicia un riego (si se esta fertirrigando, lo bloquea)
    public void iniciarRiego(int parcela) throws InterruptedException {
        boolean needOpenGeneral = false;

        lock.lock();
        try {
            while (fertirrigando) {
                puedeRegar.await();
            }

            valvulasRiegoActivas++;
            System.out.println("Se inicio el riego en parcela " + parcela);
            System.out.println(valvulasRiegoActivas + " Valvulas de riego activas");

            if (valvulasRiegoActivas == 1 && !riegoGeneral) {
                needOpenGeneral = true;
            }
        } finally {
            lock.unlock();
        }

        if (needOpenGeneral) {
            boolean ok = false;
            try {
                if (rmiCliente != null) {
                    // idealmente rmiCliente.solicitarRecurso(timeout) para no bloquear indefinidamente
                    rmiCliente.solicitarRecurso();
                    ok = true;
                } else {
                    System.err.println("RMI Cliente no configurado al iniciar riego general");
                }
            } catch (RemoteException rex) {
                System.err.println("RMI no disponible al abrir general: " + rex.getMessage());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("Interrumpido al solicitar recurso RMI para riego general");
            }

            lock.lock();
            try {
                if (ok) {
                    riegoGeneral = true;
                    imp.setRiegoGeneral(riegoGeneral);
                    System.out.println("Electrovalvula2 abierta (riego general)");
                    if (outGeneral != null) outGeneral.println("ON");
                } else {
                    // revertir incremento porque no logramos abrir la general
                    valvulasRiegoActivas--;
                    System.err.println("No se pudo abrir electrovalvula general: revertido estado.");
                    if (valvulasRiegoActivas == 0) {
                        puedeFertirregar.signalAll();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    // Comunicación valvula general
    public void setElectrovalvulaGeneral(Socket cliente) {
        lock.lock();
        try {
            this.outGeneral = new PrintWriter(cliente.getOutputStream(),true);
        } catch (IOException e) {
            System.err.println("Error al obtener outputStream de electrovalvula general: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    // Comunicación Valvula Ferti
    public void setElectroValvulaFerti(Socket cliente) {
        lock.lock();
        try {
            this.outferti = new PrintWriter(cliente.getOutputStream(),true);
        } catch (IOException ex) {
            Logger.getLogger(CoordinadorBomba.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            lock.unlock();
        }
    }

    // Termina el riego
    public void terminarRiego(int parcela) {
        boolean needCloseGeneral = false;

        lock.lock();
        try {
            valvulasRiegoActivas--;
            if (valvulasRiegoActivas < 0) valvulasRiegoActivas = 0;
            System.out.println("La parcela " + parcela + " termino el riego");
            System.out.println(valvulasRiegoActivas + " Valvulas de riego activas");

            if (valvulasRiegoActivas == 0 && riegoGeneral) {
                needCloseGeneral = true;
            }
        } finally {
            lock.unlock();
        }

        if (needCloseGeneral) {
            // cerrar general fuera del lock
            try {
                if (rmiCliente != null) rmiCliente.devolverRecurso();
                else System.err.println("RMI Cliente no configurado al cerrar riego general");
            } catch (RemoteException rex) {
                System.err.println("Error RMI al devolver recurso: " + rex.getMessage());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("Interrumpido al devolver recurso RMI para riego general");
            }

            lock.lock();
            try {
                riegoGeneral = false;
                imp.setRiegoGeneral(riegoGeneral);
                if (outGeneral != null) outGeneral.println("OFF");
                // Notifica que se puede iniciar fertirrigación
                puedeFertirregar.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    // Inicia fertirrigación
    public void iniciarFertirrigacion() throws InterruptedException {
        lock.lock();
        try {
            while (valvulasRiegoActivas > 0 || riegoGeneral) {
                System.out.println("Fertirrigacion en espera...");
                puedeFertirregar.await();
            }
        } finally {
            lock.unlock();
        }

        boolean ok = false;
        try {
            if (rmiCliente != null) {
                rmiCliente.solicitarRecurso();
                ok = true;
            } else {
                System.err.println("RMI no configurado");
            }
        } catch (RemoteException ex) {
            System.err.println("Error RMI al solicitar recurso para fertirrigación: " + ex.getMessage());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        }

        if (ok) {
            lock.lock();
            try {
                fertirrigando = true;
                imp.setFertirrigacion(fertirrigando);
                imp.setTiempos(1, 0);
                imp.setTiempos(2, 0);
                imp.setTiempos(3, 0);
                imp.setTiempos(4, 0);
                imp.setTiempos(5, 0);
                
                if (outferti != null) outferti.println("ON");
            } finally {
                lock.unlock();
            }
        } else {
            System.err.println("No se pudo iniciar fertirrigación por fallo RMI.");
        }
    }

    // Termina fertirrigación
    public void terminaFertirrigacion() {
        // devolver recurso fuera del lock
        try {
            if (rmiCliente != null) rmiCliente.devolverRecurso();
            else System.err.println("RMI no configurado al terminar fertirrigacion");
        } catch (RemoteException rex) {
            System.err.println("Error RMI al devolver recurso: " + rex.getMessage());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("Interrumpido al devolver recurso de fertirrigacion");
        }

        lock.lock();
        try {
            fertirrigando = false;
            if (imp != null) imp.setFertirrigacion(fertirrigando);
            System.out.println("Fertirrigacion terminada");
            if (outferti != null) outferti.println("OFF");
            puedeRegar.signalAll(); // Notifica que se puede iniciar riego
        } finally {
            lock.unlock();
        }
    }
    
    
    public void notificarDesconexionValvulaGeneral() throws InterruptedException {
        lock.lock();
        try {
            System.out.println("⚠️ CoordinadorBomba: Válvula general desconectada, cerrando PrintWriter");
            if (outGeneral != null) {
                try {
                    outGeneral.close();
                } catch (Exception e) {
                    // Ignorar errores al cerrar
                }
                outGeneral = null;
            }
            
            // Si había riego general activo, terminar por seguridad
            if (riegoGeneral) {
                System.out.println("⚠️ Cerrando riego general por desconexión de válvula");
                riegoGeneral = false;
                valvulasRiegoActivas = 0; // Resetear contador
                
                if (imp != null) {
                    imp.setRiegoGeneral(false);
                }
                
                // Devolver recurso RMI
                try {
                    if (rmiCliente != null) {
                        rmiCliente.devolverRecurso();
                    }
                } catch (RemoteException e) {
                    System.err.println("Error al devolver recurso RMI: " + e.getMessage());
                }
                
                puedeFertirregar.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    
    public void notificarDesconexionValvulaFerti() throws InterruptedException {
        lock.lock();
        try {
            System.out.println("⚠️ CoordinadorBomba: Válvula ferti desconectada, cerrando PrintWriter");
            if (outferti != null) {
                try {
                    outferti.close();
                } catch (Exception e) {
                    // Ignorar errores al cerrar
                }
                outferti = null;
            }
            
            // Si estaba fertirrigando, terminar por seguridad
            if (fertirrigando) {
                System.out.println("⚠️ Terminando fertirrigación por desconexión de válvula");
                fertirrigando = false;
                if (imp != null) {
                    imp.setFertirrigacion(false);
                }
                
                // Devolver recurso RMI
                try {
                    if (rmiCliente != null) {
                        rmiCliente.devolverRecurso();
                    }
                } catch (RemoteException e) {
                    System.err.println("Error al devolver recurso RMI: " + e.getMessage());
                }
                
                puedeRegar.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
