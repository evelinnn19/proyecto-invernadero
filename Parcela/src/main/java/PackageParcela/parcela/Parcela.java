import java.io.*;
import java.net.*;
import java.util.Random;

public class Parcela {
    
    
    private final int id;
    private double humedad;
    private boolean electrovalvulaActivada;
    private int tiempoRiego;
    
    
    
    // Pesos para el cálculo del INR
    private final double w1 = 0.5;
    private final double w2 = 0.3;
    private final double w3 = 0.2;
    
    
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean ejecutando;
    
    
    // Puerto para comunicación
    private static final int PUERTO = 12345;
    private static final String SERVER_HOST = "localhost";

    
    
    /*/ inicializador de la parcela, se le asigna numero de id por parametro
    El valor de la humedad arranca aleatorio.
    La electrovalvula deberia recibirse por parametro en realidad.
    El tiempo de riego comienza en 0
    No se está regando
    /*/
    
    public Parcela(int id) {
        this.id = id;
        this.humedad = 20 + new Random().nextDouble() * 40; // Humedad inicial 20-60%
        this.electrovalvulaActivada = false;
        this.tiempoRiego = 0;
        this.ejecutando = false;
    }
    
    
    
    /*/ Encendido de la parcela, comienza a monitorear los sensores centrales (Temperatura,Radiacion, lluvia)
    Abre puerto para la comunicacion.

    /*/
    public void iniciar() {
        ejecutando = true;
        System.out.println("Parcela " + id + " iniciada. Humedad inicial: " + String.format("%.1f", humedad) + "%");
        
        // Hilo para el monitoreo continuo
        new Thread(this::monitorear).start();
        
        // Hilo para escuchar comandos del sistema central
        new Thread(this::escucharComandos).start();
    }
    
    
    // Detencion del monitorio, apaga la Parcela.
    public void detener() {
        ejecutando = false;
        System.out.println("Parcela " + id + " detenida");
    }
    
    
    
    // con la parcela activada, se monitorea constantemente los valores de los sensores.
    // asi se decide si regar o no y en cuanto tiempo.

    private void monitorear() {
        while (ejecutando) {
            try {
                // Simular lectura de sensores locales (humedad)
                double humedadActual = getHumedad();
                
                // Solicitar datos de sensores compartidos al sistema central
                double temperatura = solicitarDatosSensor("TEMPERATURA");
                double radiacion = solicitarDatosSensor("RADIACION");
                double lluvia = solicitarDatosSensor("LLUVIA");
                
                
                
                // Calcular INR
                double inr = calcularINR(humedadActual, temperatura, radiacion);
                
                System.out.println("Parcela " + id + " - INR: " + String.format("%.2f", inr) + 
                                  ", Humedad: " + String.format("%.1f", humedadActual) + "%" +
                                  ", Temp: " + String.format("%.1f", temperatura) + "°C" +
                                  ", Lluvia: " + (lluvia == 0 ? "No" : "Sí"));
                
                
                
                // Decidir si regar
                if (inr > 0.7 && lluvia == 0) {
                    int tiempo = determinarTiempoRiego(inr);
                    solicitarRiego(tiempo);
                }
                
                // Simular disminución natural de humedad
                double disminucion = 1 + (new Random().nextDouble() * 3);
                humedad = Math.max(0, humedadActual - disminucion);
                
                Thread.sleep(10000); // Monitorear cada 10 segundos
                
            } catch (InterruptedException e) {
                System.out.println("Monitoreo interrumpido en parcela " + id);
            } catch (IOException e) {
                System.out.println("Error de comunicación en parcela " + id + ": " + e.getMessage());
                try {
                    Thread.sleep(5000); // Reintentar después de 5 segundos
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    
    
    private double solicitarDatosSensor(String tipoSensor) throws IOException {
        conectarAlServidor();
        out.println("SENSOR_" + tipoSensor);
        String respuesta = in.readLine();
        desconectarDelServidor();
        return Double.parseDouble(respuesta);
    }

    private void solicitarRiego(int tiempo) throws IOException {
        conectarAlServidor();
        out.println("SOLICITAR_RIEGO_" + id + "_" + tiempo);
        String respuesta = in.readLine();
        desconectarDelServidor();
        
        if ("RIEGO_AUTORIZADO".equals(respuesta)) {
            ejecutarRiego(tiempo);
        } else {
            System.out.println("Riego no autorizado para parcela " + id + ": " + respuesta);
        }
    }

    private void ejecutarRiego(int tiempo) {
        try {
            electrovalvulaActivada = true;
            tiempoRiego = tiempo;
            System.out.println("Parcela " + id + " - REGANDO por " + tiempo + " minutos");
            
            // Simular tiempo de riego (1 segundo = 1 minuto)
            Thread.sleep(tiempo * 1000);
            
            // Aumentar humedad después del riego
            humedad = Math.min(humedad + (tiempo * 5), 100);
            System.out.println("Parcela " + id + " - Riego completado. Humedad: " + String.format("%.1f", humedad) + "%");
            
            
        } catch (InterruptedException e) {
            System.out.println("Riego interrumpido en parcela " + id);
             
            
        } finally {
            electrovalvulaActivada = false;
            tiempoRiego = 0;
            
            // Notificar fin de riego al sistema central
            try {
                conectarAlServidor();
                out.println("FIN_RIEGO_" + id);
                desconectarDelServidor();
            } catch (IOException e) {
                System.out.println("Error notificando fin de riego: " + e.getMessage());
            }
        }
    }

    private void escucharComandos() {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO + id)) {
            System.out.println("Parcela " + id + " escuchando en puerto " + (PUERTO + id));
            
            while (ejecutando) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)) {
                    
                    String comando = clientIn.readLine();
                    if (comando != null) {
                        procesarComando(comando, clientOut);
                    }
                } catch (IOException e) {
                    System.out.println("Error en comunicación de parcela " + id + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error iniciando servidor de parcela " + id + ": " + e.getMessage());
        }
    }

    private void procesarComando(String comando, PrintWriter out) {
        if (comando.startsWith("ESTADO")) {
            out.println("Parcela_" + id + "_Humedad:" + String.format("%.1f", humedad) + 
                       "_Electrovalvula:" + (electrovalvulaActivada ? "Activada" : "Desactivada") +
                       "_TiempoRiego:" + tiempoRiego);
        } else if (comando.startsWith("SET_HUMEDAD")) {
            String[] partes = comando.split("_");
            if (partes.length == 3) {
                try {
                    humedad = Double.parseDouble(partes[2]);
                    out.println("Humedad actualizada: " + String.format("%.1f", humedad) + "%");
                } catch (NumberFormatException e) {
                    out.println("ERROR: Valor de humedad inválido");
                }
            }
        } else if (comando.startsWith("DETENER")) {
            detener();
            out.println("Parcela " + id + " detenida");
        }
    }
    
    
    
    private void conectarAlServidor() throws IOException {
        socket = new Socket(SERVER_HOST, PUERTO);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    
    
    
    private void desconectarDelServidor() throws IOException {
        if (out != null) out.close();
        if (in != null) in.close();
        if (socket != null) socket.close();
    }
    
    
    
    private double calcularINR(double humedad, double temperatura, double radiacion) {
        return w1 * (1 - humedad/100) + w2 * (temperatura/40) + w3 * (radiacion/1000);
    }
    
    
    
    private int determinarTiempoRiego(double inr) {
        if (inr > 0.9) return 10;
        if (inr > 0.8) return 7;
        return 5;
    }

    public double getHumedad() {
        return humedad;
    }

    public boolean isElectrovalvulaActivada() {
        return electrovalvulaActivada;
    }

    public int getTiempoRiego() {
        return tiempoRiego;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java Parcela <id_parcela>");
            System.exit(1);
        }
        
        try {
            int id = Integer.parseInt(args[0]);
            Parcela parcela = new Parcela(id);
            parcela.iniciar();
        } catch (NumberFormatException e) {
            System.out.println("ID de parcela debe ser un número");
        }
    }
}