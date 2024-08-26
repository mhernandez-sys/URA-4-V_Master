package com.example.uhf.activity;

import com.example.uhf.BuildConfig;
import com.example.uhf.R;

import com.example.uhf.fragment.GPIOFragment;
import com.example.uhf.fragment.TAGreaderprodu;
import com.example.uhf.fragment.UHFKillFragment;
import com.example.uhf.fragment.UHFLockFragment;
import com.example.uhf.fragment.UHFReadFragment;
import com.example.uhf.fragment.UHFReadTagFragment;
import com.example.uhf.fragment.UHFSetFragment;
import com.example.uhf.fragment.UHFUpgradeFragment;
import com.example.uhf.fragment.UHFWriteFragment;
import com.example.uhf.activity.LoginActivity;


import com.rscja.utility.StringUtility;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActionBar;
import android.view.View;
import android.view.LayoutInflater;


import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.ksoap2.transport.HttpsTransportSE;
import org.xmlpull.v1.XmlPullParserException;


import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTabHost;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import android.support.v4.app.INotificationSideChannel;

/**
 * UHF使用demo
 * <p>
 * 1、使用前请确认您的机器已安装此模块。
 * 2、要正常使用模块需要在\libs\armeabi\目录放置libDeviceAPI.so文件，同时在\libs\目录下放置DeviceAPIver20160728.jar文件。
 * 3、在操作设备前需要调用 init()打开设备，使用完后调用 free() 关闭设备
 * <p>
 * <p>
 * 更多函数的使用方法请查看API说明文档
 *
 * @author wushengjun
 * 更新于 2016年8月9日
 */
public class UHFMainActivity extends BaseTabFragmentActivity {

    private final static String TAG = "MainActivity";
    private FragmentTabHost mTabHost;
    private FragmentManager fm;
    public boolean isBuzzer = true;
    private String NAMESPACE = "";
    private String URL = "";
    private String METHOD_NAME = "";
    private String SOAP_ACTION = "";
    public ArrayList<String> miArrayList = new ArrayList<>();
    public ArrayList<String> ArrayListOrden = new ArrayList<>();
    public String artesperados = "";
    public String TiempoLectura;
    public String MY_CHANNEL_ID = "my_channel_id";

    TextView Et_ArtEsp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        creaciondecanal();
        Log.e("zp_add", "-------UHFMainActivity  1--------");
        if (BuildConfig.DEBUG) {
            setTitle(String.format("%s(v%s-debug)", getString(R.string.app_name), BuildConfig.VERSION_NAME));
        } else {
            setTitle(String.format("%s(v%s)", getString(R.string.app_name), BuildConfig.VERSION_NAME));
        }

//------
        initViewPageData2();
        initViewPager();
        initTabs();
//------
        initUHF();
        checkReadWritePermission();
        VMDatos vmDatos = new ViewModelProvider(this).get(VMDatos.class);

        //Activa el segundo plano para el servidor
        Intent intent = new Intent(this, Enviar.class);
        startService(intent);

        ///Configurar AlarmManager para ejecutar la aplicacion al iniciar el dispositivo
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent1 = new Intent(this, BootReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent1, PendingIntent.FLAG_MUTABLE);

        //Configurar el tiempo en el que se deseas se ejecute la accion
        long tiempoEnMilisegundos = System.currentTimeMillis() + 2000;
        alarmManager.set(AlarmManager.RTC_WAKEUP, tiempoEnMilisegundos, pendingIntent);
    }

    public void initViewPageData2() {
        uhfReadTagFragment = new UHFReadTagFragment();

        lstFrg.add(new TAGreaderprodu());
        // lstFrg.add(uhfReadTagFragment);
        //lstFrg.add(new UHFReadFragment());
        //lstFrg.add(new UHFWriteFragment());
        lstFrg.add(new UHFSetFragment());
        //lstFrg.add(new UHFLockFragment());
        //lstFrg.add(new UHFKillFragment());
        // lstFrg.add(new UHFUpgradeFragment());
        lstFrg.add(new GPIOFragment());
        ///Titulos
        lstTitles.add("Productos");
        //lstTitles.add(getString(R.string.uhf_msg_tab_scan));
        //lstTitles.add(getString(R.string.uhf_msg_tab_read));
        //lstTitles.add(getString(R.string.uhf_msg_tab_write));
        lstTitles.add(getString(R.string.uhf_msg_tab_set));
        //lstTitles.add(getString(R.string.uhf_msg_tab_lock));
        //lstTitles.add(getString(R.string.uhf_msg_tab_kill));
        //lstTitles.add(getString(R.string.action_rfid_upgrader));
        lstTitles.add("GPIO");
        lstTitles.add("Productos");
        lstTitles.add(getString(R.string.LED_Controller));
    }

    @Override
    protected void onDestroy() {
        free();
        super.onDestroy();
    }

    private void free() {
        if (mReader != null) {
            mReader.free();
        }

    }

    public void mensajes() {

        Toast.makeText(UHFMainActivity.this, "Contraseña incorrecta", Toast.LENGTH_LONG).show();

    }

    public void lanzarNotificacion() {
        int notificationID = 1;
        Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder constructor = new NotificationCompat.Builder(this, MY_CHANNEL_ID)
                .setSmallIcon(R.drawable.notificacion)
                .setContentTitle("Mi notificacion")
                .setContentText("Estral aviso")
                .setSound(sonido)   //Sonido de la notificacion
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(notificationID, constructor.build());
    }

    public void creaciondecanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(MY_CHANNEL_ID, "My channel", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("My notification Channel00");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public boolean vailHexInput(String str) {

        if (str == null || str.length() == 0) {
            return false;
        }
        if (str.length() % 2 == 0) {
            return StringUtility.isHexNumberRex(str);
        }

        return false;
    }


    /**
     * 读取成功模板声音播放
     */
    public void playSound() {
        if(isBuzzer) {
             mReader.buzzer();
        }
    }
    public void led(){
        mReader.led();
    }
    public void playSound(int i){}

    private void checkReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    public void llenarTxtEmbarque(Spinner spinner, final String Activa){


        NAMESPACE = "http://tag_android.org/";
        URL = "http://192.168.1.65/TAGSSERver.asmx";
        METHOD_NAME = "Embarques";
        SOAP_ACTION = NAMESPACE + METHOD_NAME;
        String res = "";

        Thread nt = new Thread(new Runnable() {

            @Override
            public void run() {
                String NAMESPACE = "http://tag_android.org/";
                String URL = "http://192.168.1.65/TAGSSERver.asmx";
                String METHOD_NAME = "Embarques";
                String SOAP_ACTION = NAMESPACE + METHOD_NAME;
                String res = "";
                try {
                    //Se crea el objeto SOAP
                    SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                    //Se gregan las propiedades que se van a enviar
                    //Se gregan las propiedades que se van a enviar
                    request.addProperty("Activa", Activa);

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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        procesarRespuesta1(finalRes,spinner);
                    }
                });
            }
        });
        nt.start();
    }

    private void procesarRespuesta1(String respuesta, Spinner spinner) {
        ArrayListOrden.clear();
        // Aquí debes implementar la lógica para procesar la respuesta y asignar a diferentes variables
        // Por ejemplo, asumiendo que la respuesta está en un formato separado por comas:
        char caracterAEliminar = '}'; // Por ejemplo, eliminar la coma
        char caracterAEliminar2 = ']'; // Por ejemplo, eliminar la coma
        // Reemplazar el carácter con una cadena vacía
        ///Con esta linea se elimina el parametro } recordar que lleva comillas sinples
        String cadenaSinCaracter = respuesta.replace(String.valueOf(caracterAEliminar), "");
        String cadenaSinCaracter2 = cadenaSinCaracter.replace(String.valueOf(caracterAEliminar2), "");

        String[] partes = cadenaSinCaracter2.split(",");
        String[] matriziOrden = new String[partes.length];

        for (int i = 0; i < partes.length ; i++) {
            String variable = partes[i];
            //Con split se dividen las cadenas cada que se encuentra un parametro grupal
            String[] partes1 = variable.split(":");
            String variable1 = partes1[1];
            ///En esta parte se le quitan las comillas
            String[] partes2 = variable1.split("\"");
            String variable2 = String.join("", partes2);
            ///Se almacena el resulatdoi final en la matriz
            matriziOrden[i] = variable2;
        }

        VMDatos vmDatos = new ViewModelProvider(this).get(VMDatos.class);
        ArrayList<String> Orden = new ArrayList<>();
        // Ejemplo de un ciclo for para agregar elementos al ArrayList
        ArrayListOrden.add("Selecione una opcion");
        int tamaño= partes.length;
        for (int i = 0; i < tamaño; i++) {
            String elemento = matriziOrden[i];
            ArrayListOrden.add(elemento);
        }
        List<String> datos = ArrayListOrden;
        vmDatos.getArrayListOrden().setValue(ArrayListOrden);
        // Utiliza el contexto del spinner o el contexto de la aplicación
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_item, datos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Establecer el adaptador en el Spinner
        spinner.setAdapter(adapter);
    }

    public void OpcionesConfiguracion(final String Parametro, final String Actualizar) {

        String res = "";

        Thread nt = new Thread(new Runnable() {

            @Override
            public void run() {
                String NAMESPACE = "http://tag_android.org/";
                String URL = "http://192.168.1.65/TAGSSERver.asmx";
                String METHOD_NAME = "OpcionesConfiguracion";
                String SOAP_ACTION = NAMESPACE + METHOD_NAME;
                String res = "";

                try {
                    //Se crea el objeto SOAP
                    SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                    //Se gregan las propiedades que se van a enviar
                    request.addProperty("Parametro", Parametro);
                    request.addProperty("Actualizar", Actualizar);

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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            if (finalRes.equals("1")) {
                                mensajes("Timer modificado con exito ");
                            } else if (finalRes.equals("2")) {
                                mensajes("Valor EPC modificado con exito");
                            }else {
                                mensajes(finalRes);
                            }
                            //Termina el else
                        }catch (Exception e){
                            e.printStackTrace();
                            mensajes(finalRes);
                            TiempoLectura = finalRes;
                        }
                    }
                });
            }
        });
        nt.start();
    }

    public void mensajes(final String cadena){
        Toast.makeText(this, cadena, Toast.LENGTH_LONG).show();

    }



}
