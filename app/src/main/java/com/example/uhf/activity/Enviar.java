package com.example.uhf.activity;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Enviar extends AsyncTask<String, Void, Void> {
    Socket S;
    PrintWriter PW; /// Esto es un proceso que imprime los caracteres en formato byte

    @Override
    protected Void doInBackground(String... strings) {
        String mensaje = strings[0];  // Usar strings, no voids

        try {
            S = new Socket("192.168.1.3", 5051);
            PW = new PrintWriter(S.getOutputStream());
            PW.write(mensaje);
            PW.flush();
            PW.close();
            S.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}