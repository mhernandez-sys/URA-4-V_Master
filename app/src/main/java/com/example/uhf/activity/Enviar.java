package com.example.uhf.activity;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Enviar {
    private ExecutorService executor = Executors.newFixedThreadPool(5); // Cambia el número según cuántos dispositivos manejarás en paralelo

    // Método para enviar a una lista de dispositivos
    public void enviarMensaje(String mensaje, List<String> direcciones, List<Integer> puertos) {
        if (direcciones.size() != puertos.size()) {
            throw new IllegalArgumentException("La lista de direcciones y puertos debe tener el mismo tamaño.");
        }

        for (int i = 0; i < direcciones.size(); i++) {
            String direccion = direcciones.get(i);
            int puerto = puertos.get(i);

            // Ejecuta la tarea en un hilo separado para cada dispositivo
            executor.execute(() -> {
                try (Socket S = new Socket(direccion, puerto);
                     PrintWriter PW = new PrintWriter(S.getOutputStream())) {

                    PW.write(mensaje);
                    PW.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}