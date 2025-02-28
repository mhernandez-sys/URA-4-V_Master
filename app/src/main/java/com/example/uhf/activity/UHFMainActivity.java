package com.example.uhf.activity;

import com.example.uhf.BuildConfig;
import com.example.uhf.R;
import com.example.uhf.fragment.GPIOFragment;
import com.example.uhf.fragment.TAGreaderprodu;
import com.example.uhf.fragment.UHFReadTagFragment;
import com.example.uhf.fragment.UHFSetFragment;
import com.rscja.utility.StringUtility;
import android.Manifest;
import android.app.AlarmManager;
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
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTabHost;
import java.util.ArrayList;
import android.content.Context;
import android.os.PowerManager;
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

    private PowerManager.WakeLock wakeLock;

    private final static String TAG = "MainActivity";
    private FragmentTabHost mTabHost;
    private FragmentManager fm;
    public boolean isBuzzer = true;
    public String TiempoLectura;
    public String MY_CHANNEL_ID = "my_channel_id";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Obtener el PowerManager y crear un WakeLock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLockTag");

        // Adquirir el WakeLock
        wakeLock.acquire();

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
        //Activa el segundo plano para el servidor
        Intent intent = new Intent(this, Enviar.class);
        startService(intent);

        ///Configurar AlarmManager para ejecutar la aplicacion al iniciar el dispositivo
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent1 = new Intent(this, BootReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent1, PendingIntent.FLAG_MUTABLE);

        //Configurar el tiempo en el que se deseas se ejecute la accion
        long tiempoEnMilisegundos = System.currentTimeMillis() + 1000;
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
        // Liberar el WakeLock cuando la actividad se destruya
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        free();
        super.onDestroy();
    }

    private void free() {
        if (mReader != null) {
            mReader.free();
        }

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

        if (str == null || str.isEmpty()) {
            return false;
        }
        if (str.length() % 2 == 0) {
            return StringUtility.isHexNumberRex(str);
        }

        return false;
    }
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
}
