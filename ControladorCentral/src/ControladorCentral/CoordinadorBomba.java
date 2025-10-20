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
    private PrintWriter outferti;   // para enviar a EV1

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
            // Esperar si hay fertirrigación activa
            while (fertirrigando) {
                System.out.println("⏳ Parcela " + parcela + " esperando (fertirrigación activa)");
                puedeRegar.await();
            }

            valvulasRiegoActivas++;
            System.out.println("✅ Se inició el riego en parcela " + parcela);
            System.out.println("📊 " + valvulasRiegoActivas + " válvula(s) de riego activas");

            // Verificar si necesitamos abrir la válvula general
            if (valvulasRiegoActivas == 1 && !riegoGeneral) {
                needOpenGeneral = true;
            }
        } finally {
            lock.unlock();
        }

        // 🔧 FUERA DEL LOCK: Solicitar recurso RMI y abrir válvula general
        if (needOpenGeneral) {
            boolean recursoObtenido = false;
            
            try {
                if (rmiCliente != null) {
                    System.out.println("🔄 Solicitando recurso RMI para riego general...");
                    rmiCliente.solicitarRecurso();
                    recursoObtenido = true;
                    System.out.println("✅ Recurso RMI obtenido");
                } else {
                    System.err.println("❌ RMI Cliente no configurado al iniciar riego general");
                }
            } catch (RemoteException rex) {
                System.err.println("❌ RMI no disponible al abrir general: " + rex.getMessage());
                recursoObtenido = false;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("⚠️ Interrumpido al solicitar recurso RMI para riego general");
                throw ie;
            }

            // 🔧 VOLVER AL LOCK: Actualizar estado y abrir válvula
            lock.lock();
            try {
                if (recursoObtenido) {
                    riegoGeneral = true;
                    
                    // 🎯 CRÍTICO: Abrir la válvula general (EV2)
                    if (outGeneral != null) {
                        outGeneral.println("ON");
                        outGeneral.flush();
                        
                        if (outGeneral.checkError()) {
                            System.err.println("❌ Error al enviar ON a válvula general");
                        } else {
                            System.out.println("💧 Electroválvula2 ABIERTA (riego general)");
                        }
                    } else {
                        System.err.println("⚠️ outGeneral es NULL - Válvula no conectada");
                    }
                    
                    // Actualizar impresor
                    if (imp != null) {
                        imp.setRiegoGeneral(riegoGeneral);
                    }
                } else {
                    // No pudimos obtener el recurso, revertir
                    valvulasRiegoActivas--;
                    System.err.println("❌ No se pudo abrir electroválvula general: revertido estado.");
                    System.err.println("📊 Válvulas activas revertidas a: " + valvulasRiegoActivas);
                    
                    if (valvulasRiegoActivas == 0) {
                        puedeFertirregar.signalAll();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    // Comunicación válvula general
    public void setElectrovalvulaGeneral(Socket cliente) {
        lock.lock();
        try {
            this.outGeneral = new PrintWriter(cliente.getOutputStream(), true);
            System.out.println("✅ PrintWriter para Electroválvula General (EV2) configurado");
        } catch (IOException e) {
            System.err.println("❌ Error al obtener outputStream de electroválvula general: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    // Comunicación Válvula Ferti
    public void setElectroValvulaFerti(Socket cliente) {
        lock.lock();
        try {
            this.outferti = new PrintWriter(cliente.getOutputStream(), true);
            System.out.println("✅ PrintWriter para Electroválvula Ferti (EV1) configurado");
        } catch (IOException ex) {
            System.err.println("❌ Error al configurar válvula ferti: " + ex.getMessage());
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
            
            System.out.println("🛑 La parcela " + parcela + " terminó el riego");
            System.out.println("📊 " + valvulasRiegoActivas + " válvula(s) de riego activas");

            // Si ya no hay válvulas activas, cerrar la general
            if (valvulasRiegoActivas == 0 && riegoGeneral) {
                needCloseGeneral = true;
            }
        } finally {
            lock.unlock();
        }

        // 🔧 FUERA DEL LOCK: Devolver recurso RMI
        if (needCloseGeneral) {
            try {
                if (rmiCliente != null) {
                    System.out.println("🔄 Devolviendo recurso RMI...");
                    rmiCliente.devolverRecurso();
                    System.out.println("✅ Recurso RMI devuelto");
                } else {
                    System.err.println("❌ RMI Cliente no configurado al cerrar riego general");
                }
            } catch (RemoteException rex) {
                System.err.println("❌ Error RMI al devolver recurso: " + rex.getMessage());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("⚠️ Interrumpido al devolver recurso RMI para riego general");
            }

            // 🔧 VOLVER AL LOCK: Cerrar válvula y actualizar estado
            lock.lock();
            try {
                riegoGeneral = false;
                
                // 🎯 CRÍTICO: Cerrar la válvula general (EV2)
                if (outGeneral != null) {
                    outGeneral.println("OFF");
                    outGeneral.flush();
                    
                    if (outGeneral.checkError()) {
                        System.err.println("❌ Error al enviar OFF a válvula general");
                    } else {
                        System.out.println("💧 Electroválvula2 CERRADA");
                    }
                } else {
                    System.err.println("⚠️ outGeneral es NULL al cerrar");
                }
                
                // Actualizar impresor
                if (imp != null) {
                    imp.setRiegoGeneral(riegoGeneral);
                }
                
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
            // Esperar mientras haya riego activo
            while (valvulasRiegoActivas > 0 || riegoGeneral) {
                System.out.println("⏳ Fertirrigación en espera (riego activo)...");
                puedeFertirregar.await();
            }
        } finally {
            lock.unlock();
        }

        // 🔧 FUERA DEL LOCK: Solicitar recurso RMI
        boolean recursoObtenido = false;
        try {
            if (rmiCliente != null) {
                System.out.println("🔄 Solicitando recurso RMI para fertirrigación...");
                rmiCliente.solicitarRecurso();
                recursoObtenido = true;
                System.out.println("✅ Recurso RMI obtenido para fertirrigación");
            } else {
                System.err.println("❌ RMI no configurado para fertirrigación");
            }
        } catch (RemoteException ex) {
            System.err.println("❌ Error RMI al solicitar recurso para fertirrigación: " + ex.getMessage());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        }

        // 🔧 VOLVER AL LOCK: Abrir válvula ferti
        if (recursoObtenido) {
            lock.lock();
            try {
                fertirrigando = true;
                
                // 🎯 CRÍTICO: Abrir la válvula de fertirrigación (EV1)
                if (outferti != null) {
                    outferti.println("ON");
                    outferti.flush();
                    
                    if (outferti.checkError()) {
                        System.err.println("❌ Error al enviar ON a válvula ferti");
                    } else {
                        System.out.println("🌿 Electroválvula1 ABIERTA (fertirrigación)");
                    }
                } else {
                    System.err.println("⚠️ outferti es NULL - Válvula no conectada");
                }
                
                // Actualizar impresor
                if (imp != null) {
                    imp.setFertirrigacion(fertirrigando);
                    imp.setTiempos(1, 0);
                    imp.setTiempos(2, 0);
                    imp.setTiempos(3, 0);
                    imp.setTiempos(4, 0);
                    imp.setTiempos(5, 0);
                }
            } finally {
                lock.unlock();
            }
        } else {
            System.err.println("❌ No se pudo iniciar fertirrigación por fallo RMI.");
        }
    }

    // Termina fertirrigación
    public void terminaFertirrigacion() {
        // 🔧 FUERA DEL LOCK: Devolver recurso RMI
        try {
            if (rmiCliente != null) {
                System.out.println("🔄 Devolviendo recurso RMI de fertirrigación...");
                rmiCliente.devolverRecurso();
                System.out.println("✅ Recurso RMI devuelto");
            } else {
                System.err.println("❌ RMI no configurado al terminar fertirrigacion");
            }
        } catch (RemoteException rex) {
            System.err.println("❌ Error RMI al devolver recurso: " + rex.getMessage());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ Interrumpido al devolver recurso de fertirrigacion");
        }

        // 🔧 VOLVER AL LOCK: Cerrar válvula
        lock.lock();
        try {
            fertirrigando = false;
            
            // 🎯 CRÍTICO: Cerrar la válvula de fertirrigación (EV1)
            if (outferti != null) {
                outferti.println("OFF");
                outferti.flush();
                
                if (outferti.checkError()) {
                    System.err.println("❌Error al enviar OFF a válvula ferti");
                } else {
                    System.out.println("🌿 Electroválvula1 CERRADA");
                }
            } else {
                System.err.println("⚠️ outferti es NULL al cerrar");
            }
            
            // Actualizar impresor
            if (imp != null) {
                imp.setFertirrigacion(fertirrigando);
            }
            
            System.out.println("✅ Fertirrigación terminada");
            
            // Notificar que se puede iniciar riego
            puedeRegar.signalAll();
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
                
                puedeFertirregar.signalAll();
            }
        } finally {
            lock.unlock();
        }
        
        // Devolver recurso RMI fuera del lock
        if (riegoGeneral) {
            try {
                if (rmiCliente != null) {
                    rmiCliente.devolverRecurso();
                }
            } catch (RemoteException e) {
                System.err.println("Error al devolver recurso RMI: " + e.getMessage());
            }
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
                
                puedeRegar.signalAll();
            }
        } finally {
            lock.unlock();
        }
        
        // Devolver recurso RMI fuera del lock
        if (fertirrigando) {
            try {
                if (rmiCliente != null) {
                    rmiCliente.devolverRecurso();
                }
            } catch (RemoteException e) {
                System.err.println("Error al devolver recurso RMI: " + e.getMessage());
            }
        }
    }
}