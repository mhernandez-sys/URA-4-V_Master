package com.example.uhf.activity;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Enviar {
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void enviarMensaje(String mensaje) {
        executor.execute(() -> {
            try (Socket S = new Socket("192.168.1.31", 5052);
                 PrintWriter PW = new PrintWriter(S.getOutputStream())) {

                PW.write(mensaje);
                PW.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
