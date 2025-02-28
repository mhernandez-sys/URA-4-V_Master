package com.example.uhf.activity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Enviar {

    private final int maxReintentos; // Número máximo de reintentos
    private final int timeout; // Tiempo de espera en milisegundos
    private final ExecutorService executor; // Pool de hilos para manejar múltiples envíos
    private final EnviarListener listener; // Listener para notificar resultados
    private final Context context;

    // Interfaz para notificar el resultado del envío
    public interface EnviarListener {
        void onEnvioExitoso(String direccion, int puerto);
        void onEnvioFallido(String direccion, int puerto, Exception e);
    }

    public Enviar(int maxReintentos, int timeout, int poolSize, EnviarListener listener, Context context) {
        this.maxReintentos = maxReintentos;
        this.timeout = timeout;
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.listener = listener;
        this.context = context; // Asegúrate de asignar un contexto válido
    }

    // Método principal para enviar mensajes a múltiples dispositivos
    public void enviarMensaje(String mensaje, List<String> direcciones, List<Integer> puertos) {
        if (direcciones.size() != puertos.size()) {
            throw new IllegalArgumentException("La lista de direcciones y puertos debe tener el mismo tamaño.");
        }

        for (int i = 0; i < direcciones.size(); i++) {
            final String direccion = direcciones.get(i);
            final int puerto = puertos.get(i);

            // Ejecutar cada envío en un hilo separado
            executor.execute(() -> enviarMensajeAEsclavo(mensaje, direccion, puerto));
        }
    }

    // Método para cerrar el pool de hilos
    public void cerrar() {
        executor.shutdown();
    }

    // Método privado para enviar un mensaje a un esclavo específico
    private void enviarMensajeAEsclavo(String mensaje, String direccion, int puerto) {
        boolean exito = false;
        int intentos = 0;

        while (!exito && intentos < maxReintentos) {
            try (Socket socket = new Socket(direccion, puerto);
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                socket.setSoTimeout(timeout); // Configurar tiempo de espera

                // Enviar el mensaje
                writer.println(mensaje);
                writer.flush();

                // Esperar confirmación (ACK)
                String respuesta = reader.readLine();
                if (respuesta != null && respuesta.trim().equalsIgnoreCase("Mensaje Recibido")) {
                    exito = true;
                    if (listener != null) listener.onEnvioExitoso(direccion, puerto);
                    // Mostrar mensaje en un Toast
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> {
                        try {
                            Toast.makeText(context.getApplicationContext(), "Respuesta recibida: " + respuesta, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            System.err.println("Error al mostrar el Toast: " + e.getMessage());
                        }
                    });
                } else {
                    throw new IOException("Respuesta inesperada del servidor: " + respuesta);
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Tiempo de espera excedido para " + direccion + ":" + puerto);
            } catch (IOException e) {
                System.err.println("Error al enviar mensaje a " + direccion + ":" + puerto + " - " + e.getMessage());
            } finally {
                intentos++;
            }
        }

        if (!exito && listener != null) {
            listener.onEnvioFallido(direccion, puerto, new Exception("Máximo número de intentos alcanzado."));
        }
    }
}