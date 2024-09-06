package com.example.uhf;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.Map;


public class WebServiceManager {

    private final Context mContext;

    public WebServiceManager(Context context) {
        this.mContext = context;
    }

    public void callWebService(final String METHOD_NAME, final Map<String, String> properties, final WebServiceCallback callback) {

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String NAMESPACE = "http://Estral.org/";
                String URL = "http://192.168.1.21/Embarques/EmbarquesWS.asmx";
                String SOAP_ACTION = NAMESPACE + METHOD_NAME;
                String res = "";
                int timeout = 5000; // 5000 milisegundos (5 segundos)

                try {
                    SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                    if (properties != null && !properties.isEmpty()) {
                        for (Map.Entry<String, String> entry : properties.entrySet()) {
                            request.addProperty(entry.getKey(), entry.getValue());
                        }
                    }

                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);

                    HttpTransportSE transporte = new HttpTransportSE(URL, timeout);
                    transporte.call(SOAP_ACTION, envelope);

                    SoapPrimitive resultado_xml = (SoapPrimitive) envelope.getResponse();
                    res = resultado_xml.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    res = "Error: " + e.getMessage();
                }
                return res;
            }

            @Override
            protected void onPostExecute(String result) {
                Log.d("WebServiceManager", "WebService Result: " + result);
                if (callback != null) {
                    try {
                        callback.onWebServiceCallComplete(result);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.execute();
    }

    public interface WebServiceCallback {
        void onWebServiceCallComplete(String result) throws JSONException;
    }
}
