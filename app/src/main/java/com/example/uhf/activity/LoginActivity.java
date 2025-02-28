package com.example.uhf.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.uhf.R;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

public class LoginActivity extends BaseTabFragmentActivity {
    EditText ET_Usuario;
    //EditText ET_Password;
    Button btnLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ET_Usuario = findViewById(R.id.ET_Usuario);
        btnLogin =findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String usuario = ET_Usuario.getText().toString();
           // String password = ET_Password.getText().toString();
            login(usuario);
        });
    }

    public void login(final String Usuario) {
        Thread nt = new Thread(() -> {
            String NAMESPACE = "http://tag_android.org/";
            String URL = "http://192.168.1.65/TAGSSERver.asmx";
            String METHOD_NAME = "Login";
            String SOAP_ACTION = NAMESPACE + METHOD_NAME;
            String res ;

            try {
                //Se crea el objeto SOAP
                SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                //Se gregan las propiedades que se van a enviar
                request.addProperty("Usuario", Usuario);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;
                envelope.setOutputSoapObject(request);

                HttpTransportSE transporte = new HttpTransportSE(URL);
                transporte.call(SOAP_ACTION, envelope);

                SoapPrimitive resultado_xml = (SoapPrimitive) envelope.getResponse();
                res = resultado_xml.toString();

            } catch (Exception e) {
                e.printStackTrace();
                res = "Error: " + e.getMessage();
            }
            final String finalRes = res;
            runOnUiThread(() -> {

                int acceso1 = Integer.parseInt(finalRes);
                if (acceso1 != 0) {

                    Intent intent = new Intent(getApplicationContext(), UHFMainActivity.class);
                    startActivity(intent);

                } else {
                    mostrarmensaje();
                }
            });
        });
        nt.start();
    }

    public void mostrarmensaje(){
        Toast.makeText(LoginActivity.this, "Contraseña incorrecta",Toast.LENGTH_LONG).show();
    }

}