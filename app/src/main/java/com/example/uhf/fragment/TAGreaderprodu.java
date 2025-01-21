package com.example.uhf.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.example.uhf.R;
import com.example.uhf.WebServiceManager;
import com.example.uhf.activity.Enviar;
import com.example.uhf.activity.UHFMainActivity;
import com.example.uhf.tools.NumberTool;
import com.example.uhf.tools.StringUtils;
import com.example.uhf.tools.UIHelper;
import com.rscja.deviceapi.RFIDWithUHFA4;
import com.rscja.deviceapi.entity.GPIStateEntity;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.rscja.deviceapi.exception.ConfigurationException;

public class TAGreaderprodu extends KeyDownFragment implements Enviar.EnviarListener {
    // Declarar una variable booleana para controlar el estado del hilo
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> futureTask;
    private boolean hiloActivo = false;
    private int previousState = -1; // Estado anterior inicializado
    RFIDWithUHFA4 rfidWithUHFA4 = null;
    //    private final Semaphore semaphore = new Semaphore(1);
    private static final String TAG = "UHFReadTagFragment";
    private boolean loopFlag = false;
    private int inventoryFlag = 1;
    public ArrayList<HashMap<String, String>> tagList;
    private WebServiceManager webServiceManager;

    private boolean isProgressing = false;

    SimpleAdapter adapter;
    Button BtClear;
    TextView tv_count, tv_totalNum, tv_time;
    RadioGroup RgInventory;
    RadioButton RbInventorySingle;
    RadioButton RbInventoryLoop;
    TextView Et_ArtEsp, Et_Pedidos, TXTART_ENC, Et_Partidas, Et_Bodegas, TxtEmbarque;
    TextView MSAlerta, MSAlertaActivo, MSAlertaincompletos;
    Button BtInventory;
    ListView LvTags;
    private UHFMainActivity mContext;
    private HashMap<String, String> map;
    private CheckBox cbFilter;

    public static final String TAG_EPCAndTidUser = "TAG_EPCAndTidUser";
    public static final String TAG_EPC = "tagEpc";
    public static final String TAG_TID = "tagTid";
    public static final String TAG_USER = "tagUser";
    public static final String TAG_LEN = "tagLen";
    public static final String TAG_COUNT = "tagCount";
    public static final String TAG_RSSI = "tagRssi";
    public static final String TAG_ANT = "tagAnt";
    public String CadenaEPCS = "";
    private int totalNum;
    private List<String> tempDatas;
    boolean isStop = false;
   //Clase para amndar mensaje a Enviar
    private Enviar enviar;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                UHFTAGInfo info = (UHFTAGInfo) msg.obj;
                addDataToList(mergeTidEpc(info.getTid(), info.getEPC(), info.getUser()), info.getEPC(), info.getTid(), info.getUser(), info.getRssi(), info.getAnt());
                //mContext.playSound();
                mContext.led();
                BtClear.setEnabled(false);
            } else {
                setTotalTime();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "UHFReadTagFragmentProductos.onCreateVetTimeiew");
        View view = inflater.inflate(R.layout.fragment_t_a_greaderprodu, container, false);
        inits(view);
        // Definir el listener
        Enviar.EnviarListener listener = new Enviar.EnviarListener() {
            @Override
            public void onEnvioExitoso(String direccion, int puerto) {
                System.out.println("Envío exitoso a " + direccion + ":" + puerto);
            }

            @Override
            public void onEnvioFallido(String direccion, int puerto, Exception e) {
                System.err.println("Fallo al enviar a " + direccion + ":" + puerto + " - " + e.getMessage());
            }
        };

        // Crear la instancia de Enviar
        enviar = new Enviar(5, 1000, 3, listener, getContext());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "UHFReadTagFragmentProductos.onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        try {
            rfidWithUHFA4 = RFIDWithUHFA4.getInstance();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        mContext = (UHFMainActivity) getActivity();
        executorService = Executors.newFixedThreadPool(20);
        Et_ArtEsp = getView().findViewById(R.id.Et_ArtEsp);
        Et_Pedidos =  getView().findViewById(R.id.Et_Pedidos);
        Et_Bodegas =  getView().findViewById(R.id.Et_Bodegas);
        Et_Partidas =  getView().findViewById(R.id.Et_Partidas);
        TXTART_ENC =  getView().findViewById(R.id.TXTART_ENC);
        TxtEmbarque =  getView().findViewById(R.id.TxtEmbarque);
        MSAlerta = getView().findViewById(R.id.MSAlerta);
        MSAlertaincompletos =  getView().findViewById(R.id.MSAlertaincompletos);
        MSAlertaActivo =  getView().findViewById(R.id.MSAlertaActivo);
        MSAlerta.setVisibility(View.GONE);
        MSAlertaincompletos.setVisibility(View.GONE);
        MSAlertaActivo.setVisibility(View.GONE);
        webServiceManager = new WebServiceManager(requireContext());
        //iniciarHilo();
    }

    private void inits(View view) {
        BtClear = (Button) view.findViewById(R.id.BtClear1);
        tv_count = (TextView) view.findViewById(R.id.tv_count);
        tv_totalNum = (TextView) view.findViewById(R.id.tv_totalNum);
        tv_time = (TextView) view.findViewById(R.id.tv_time);
        RgInventory = (RadioGroup) view.findViewById(R.id.RgInventory);
        // etTime = (EditText) view.findViewById(R.id.etTime);

        RbInventorySingle = (RadioButton) view.findViewById(R.id.RbInventorySingle);
        RbInventoryLoop = (RadioButton) view.findViewById(R.id.RbInventoryLoop);

        tagList = new ArrayList<>();
        tempDatas = new ArrayList<>();
        BtInventory = (Button) view.findViewById(R.id.BtInventory1);
        LvTags = (ListView) view.findViewById(R.id.LvTags);
        adapter = new SimpleAdapter(getContext(), tagList, R.layout.listtag_items,
                new String[]{TAG_EPC, TAG_LEN, TAG_COUNT, TAG_RSSI, TAG_ANT},
                new int[]{R.id.TvTagUii, R.id.TvTagLen, R.id.TvTagCount, R.id.TvTagRssi, R.id.TvAnt});
        LvTags.setAdapter(adapter);

        BtClear.setOnClickListener(new TAGreaderprodu.BtClearClickListener());
        //RgInventory.setOnCheckedChangeListener(new TAGreaderprodu.RgInventoryCheckedListener());
        BtInventory.setOnClickListener(new TAGreaderprodu.BtInventoryClickListener());
        //initFilter(view); // Inicializar filtrado

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (!isVisibleToUser) {
            // Dejar de identificar
            stopInventory();
        }
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint>>>isVisibleToUser=" + isVisibleToUser);
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.currentFragment = this;
    }

    @Override
    public void onPause() {
        Log.i(TAG, "UHFReadTagFragment.onPause");
        super.onPause();
        // 停止识别
        stopInventory();
        mContext.currentFragment = null;
    }

    ///Agregar EPC a la lista
    private void addDataToList(String tidAndEPCUser, String Epc, String Tid, String User, String rssi, String ant) {
        if (StringUtils.isNotEmpty(Epc)) {
            int index = checkIsExist(tidAndEPCUser);
            map = new HashMap<>();
            map.put(TAG_EPCAndTidUser, tidAndEPCUser);
            map.put(TAG_EPC, Epc); //Este es el EPC que se imprime en la pantalla
            map.put(TAG_TID, Tid);
            map.put(TAG_USER, User);
            map.put(TAG_COUNT, String.valueOf(1));
            map.put(TAG_RSSI, rssi);
            map.put(TAG_ANT, ant);
            //El index determina si el epc ha sido leido, si no lo imprime y en caso contrario lo salta
            if (index == -1) {
                ///En esta funcion se le debe de agregar los epcs y almacenarlos en una variable publica para posteriormente mandar la al web service
                tagList.add(map);
                tempDatas.add(tidAndEPCUser);
                tv_count.setText(String.valueOf(adapter.getCount()));///En esta parte se le agrega el epc que no han sido leidos
                CadenaEPCS += "'" + Epc + "',";

            } else {
                int tagCount = Integer.parseInt(tagList.get(index).get(TAG_COUNT), 10) + 1; //En esta parte se le agreaga el contador de tag +1
                map.put(TAG_COUNT, String.valueOf(tagCount));//
                tagList.set(index, map);
            }
            tv_totalNum.setText(String.valueOf(++totalNum));
            adapter.notifyDataSetChanged();
        }
    }

    private long mStartTime;

    private void setTotalTime() {///En esta parte se mandara a llamar a la funcion que imprimira las variables y que detanga la lectura
        if (loopFlag) {
            float useTime = (System.currentTimeMillis() - mStartTime) / 1000.0F;
            double dTime = NumberTool.getPointDouble(1, useTime);
            tv_time.setText(dTime + "s");
            //String strTime = etTime.getText().toString();
            //int time = 9999999; //Valor original
            //Se recupera el valor almacenado en el dispositivo
            int timer;
            timer = 20;
            if (dTime >= timer && dTime<=28) {
                //String valorembarque = TxtEmbarque.getSelectedItem().toString();
                stopInventory();
                // Remover la última coma
                if (CadenaEPCS.length() > 0) {
                    CadenaEPCS = CadenaEPCS.substring(0, CadenaEPCS.length() - 1);
                }
                ProgressBar(CadenaEPCS);
                if (map == null) {
                    LimpiarValores();
                    //iniciarHilo();
                }
            }
        }
    }

    public class BtClearClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            LimpiarValores();
            //iniciarHilo();
            rfidWithUHFA4.output3Off();
            rfidWithUHFA4.output1Off();
        }
    }

    private void LimpiarValores() {
        totalNum = 0;
        TxtEmbarque.setText("");
        tv_count.setText("0");
        tv_totalNum.setText("");
        tv_time.setText("0s");
        Et_ArtEsp.setText("");
        TXTART_ENC.setText("");
        Et_Pedidos.setText("");
        Et_Partidas.setText("");
        Et_Bodegas.setText("");
        MSAlerta.clearAnimation();
        MSAlerta.setVisibility(View.GONE);
        MSAlertaincompletos.clearAnimation();
        MSAlertaincompletos.setVisibility(View.GONE);
        MSAlertaActivo.clearAnimation();
        MSAlertaActivo.setVisibility(View.GONE);
        CadenaEPCS = "";
        tagList.clear();
        tempDatas.clear();
        adapter.notifyDataSetChanged();
    }

    public class RgInventoryCheckedListener implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == RbInventorySingle.getId()) {
                // Identificación de un solo paso
                inventoryFlag = 0;
                cbFilter.setChecked(false);
                cbFilter.setVisibility(View.INVISIBLE);
            } else if (checkedId == RbInventoryLoop.getId()) {
                // Identificación de bucle de etiqueta única
                inventoryFlag = 1;
                cbFilter.setVisibility(View.VISIBLE);
            }
        }
    }

    public class BtInventoryClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            //readTag();
            mensajesocket();
        }
    }

    private void readTag() {
        if (BtInventory.getText().equals(mContext.getString(R.string.btInventory)))//Si el boton es igual a iniciar
        {
            BtInventory.setBackgroundColor(getResources().getColor(R.color.txtblue));
            switch (inventoryFlag) {
                case 0:// 单步
                    //En el caso 0 es cuando se lee de un tag en un tag
                    mStartTime = System.currentTimeMillis();
                    UHFTAGInfo uhftagInfo = mContext.mReader.inventorySingleTag();
                    if (uhftagInfo != null) {
                        tv_count.setText(String.valueOf(adapter.getCount()));
                        tv_totalNum.setText(String.valueOf(totalNum));
                        addDataToList(mergeTidEpc(uhftagInfo.getTid(), uhftagInfo.getEPC(), uhftagInfo.getUser()), uhftagInfo.getEPC(), uhftagInfo.getTid(), uhftagInfo.getUser(), uhftagInfo.getRssi(), uhftagInfo.getAnt());
                        // setTotalTime();
                        mContext.playSound();
                    } else {
                        UIHelper.ToastMessage(mContext, R.string.uhf_msg_inventory_fail);
                    }
                    break;
                case 1:// En el caso 1 es para leer de manera de scaner
                    if (mContext.mReader.startInventoryTag()) {
                        BtInventory.setText(mContext.getString(R.string.title_stop_Inventory));
                        BtClear.setBackgroundColor(getResources().getColor(R.color.grayslate));
                        loopFlag = true;
                        isStop = false;
                        setViewEnabled(false);
                        mStartTime = System.currentTimeMillis();
                        new TagThread().start();
                    } else {
                        mContext.mReader.stopInventory();
                        UIHelper.ToastMessage(mContext, R.string.uhf_msg_inventory_open_fail);
                    }
                    break;
                default:
                    break;
            }
        } else { // detener el reconocimiento
            stopInventory();
            //setTotalTime();
        }
    }

    private void setViewEnabled(boolean enabled) {
        //RbInventorySingle.setEnabled(enabled);
        RbInventoryLoop.setEnabled(enabled);
        //cbFilter.setEnabled(enabled);
        //btnSetFilter.setEnabled(enabled);
        BtClear.setEnabled(enabled);
        // BtClear.setBackgroundColor(getResources().getColor(R.color.txtblue));
    }

    /**
     * detener el reconocimiento
     */
    private synchronized void stopInventory() {
        if (loopFlag && !isStop) {
            isStop = true;
            executorService.execute(() -> {
                if (mContext.mReader.stopInventory()) {
                    SystemClock.sleep(200);
                    mContext.runOnUiThread(() -> {
                        BtInventory.setText(mContext.getString(R.string.btInventory));
                        BtClear.setBackgroundColor(getResources().getColor(R.color.txtblue));
                        loopFlag = false;
                        setViewEnabled(true);
                    });
                } else {
                    mContext.runOnUiThread(() -> {
                        UIHelper.ToastMessage(mContext, R.string.uhf_msg_inventory_stop_fail);
                        loopFlag = false;
                        setViewEnabled(true);
                    });
                }
            });
        }
    }

    /**
     * Búsqueda binaria, busque el subíndice del valor en la matriz, de lo contrario-1
     */
    static int binarySearch(List<String> array, String src) {
        if (array == null) {
            return -1;
        }
        int left = 0;
        int right = array.size() - 1;
        // Esto debe ser <=
        while (left <= right) {
            if (compareString(array.get(left), src)) {
                return left;
            } else if (left != right) {
                if (compareString(array.get(right), src))
                    return right;
            }
            left++;
            right--;
        }
        return -1;
    }

    static boolean compareString(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        } else if (str1.hashCode() != str2.hashCode()) {
            return false;
        } else {
            char[] value1 = str1.toCharArray();
            char[] value2 = str2.toCharArray();
            int size = value1.length;
            for (int k = 0; k < size; k++) {
                if (value1[k] != value2[k]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 判断EPC是否在列表中
     *
     * @param epc 索引
     * @return
     */
    public int checkIsExist(String epc) {
        return binarySearch(tempDatas, epc);
    }

    class TagThread extends Thread {
        public void run() {
            UHFTAGInfo uhftagInfo;
            Message msg;
            long time = 0;
            while (loopFlag) {
                uhftagInfo = mContext.mReader.readTagFromBuffer();
                if (uhftagInfo != null) {
                    msg = handler.obtainMessage();
                    msg.obj = uhftagInfo;
                    msg.what = 1;
                    handler.sendMessage(msg);
                }

                if (System.currentTimeMillis() - time > 100) {
                    time = System.currentTimeMillis();
                    Log.e("AABB", "111");
                    handler.sendEmptyMessage(2);
                }
            }
        }
    }

    private String mergeTidEpc(String tid, String epc, String user) {
        if (!TextUtils.isEmpty(user)) {
            return "TID:" + tid + "\nEPC:" + epc + "\nUser:" + user;
        } else if (!TextUtils.isEmpty(tid) && !tid.equals("0000000000000000") && !tid.equals("000000000000000000000000")) {
            return "TID:" + tid + "\nEPC:" + epc;
        } else {
            return epc;
        }
    }

    @Override
    public void myOnKeyDwon() {
        readTag();
    }

    private void iniciarAnimacionParpadeo(final int Activacion) {
        AlphaAnimation parpadeo = new AlphaAnimation(1f, 0f); // De totalmente visible a totalmente invisible
        parpadeo.setDuration(500); // Duración de cada fase del parpadeo en milisegundos
        parpadeo.setRepeatMode(Animation.REVERSE);
        parpadeo.setRepeatCount(5);
        if (Activacion == 1) {
            ///Activar la etiqueta de embarque completo
            MSAlertaActivo.setVisibility(View.VISIBLE);
            // Asignar la animación al ImageView
            MSAlertaActivo.startAnimation(parpadeo);
        } else if (Activacion == 2) {
            ///A
            MSAlertaincompletos.setVisibility(View.VISIBLE);
            // Asignar la animación al ImageView
            MSAlertaincompletos.startAnimation(parpadeo);
        } else {
            MSAlerta.setVisibility(View.VISIBLE);
            // Asignar la animación al ImageView
            MSAlerta.startAnimation(parpadeo);
        }
    }

    public void monitorizarCambiosGPIO() {
        while (hiloActivo) {
            GPIO_estatus();
            // Esperar un tiempo antes de la próxima verificación
            try {
                Thread.sleep(1000); // Puedes ajustar el tiempo según sea necesario
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Interrumpir el hilo
                break; // Salir del bucle si se interrumpe el hilo
            }
        }
    }

    public void GPIO_estatus() {
        try {
            // Obtener el estado actual del GPIO
            List<GPIStateEntity> list = rfidWithUHFA4.inputStatus();
            if (list == null) {
                mostrarToast("No se pudo obtener");
                return;
            }
            // Obtener el estado actual del GPIO
            int currentState = list.get(0).getGpiState();
            // Comparar con el estado anterior
            if (currentState != previousState) {
                // Si hay un cambio, mostrar el mensaje
                mostrarToast("Cambio detectado en GPIO: " + currentState);
                if (currentState == 0) {
                    detenerHilo();
                    readTag();
                    mensajesocket();
                }
                previousState = currentState; // Actualizar el estado anterior
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void iniciarHilo() {
        if (futureTask == null || futureTask.isDone()) {
            hiloActivo = true;
            rfidWithUHFA4.output1Off();
            futureTask = executorService.submit(this::monitorizarCambiosGPIO);
        }
    }

    public void detenerHilo() {
        hiloActivo = false;
        if (futureTask != null && !futureTask.isDone()) {
            futureTask.cancel(true);
        }
    }

    private void mostrarToast(final String mensaje) {
        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show());
    }

    public void ProgressBar(String EPCTAG) {
        if (EPCTAG.isEmpty()) {
            //iniciarHilo();
            LimpiarValores();
            return;
        }

        // Mostrar el ProgresDialog
        if (isProgressing) {
            return;
        }
        isProgressing = true;
        // Crea y muestra el ProgresDialog
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Procesando...");
        progressDialog.setCancelable(false); // Evita que el usuario pueda cancelar el diálogo
        mostrarToast(EPCTAG);
        progressDialog.show();

        // Crear el mapa de propiedades para enviar
        Map<String, String> properties = new HashMap<>();
        properties.put("EPCTAG", EPCTAG);

        webServiceManager.callWebService("ProcesarGuia_Maestro", properties, result -> {
            // Ocultar el ProgressDialog si está activo
            progressDialog.dismiss();
            isProgressing = false;

            try {
                // Validar si el resultado es un error
                if (result.toLowerCase().contains("error") || result.toLowerCase().contains("time out")) {
                    mostrarToast("No se pudo determinar la guía para el EPCTAG proporcionado.");
                    ejecutarAccionPostError();
                    return;
                }

                // Procesar el resultado JSON
                JSONArray jsonArray = new JSONArray(result);
                procesarRespuestaJSON(jsonArray);

            } catch (JSONException e) {
                e.printStackTrace();
                mostrarToast("Error al procesar los datos del servidor.");
                ejecutarAccionPostError();
            } catch (Exception e) {
                e.printStackTrace();
                mostrarToast("Error inesperado: " + e.getMessage());
                ejecutarAccionPostError();
            }
        });
    }
    // Método para procesar la respuesta JSON
    private void procesarRespuestaJSON(JSONArray jsonArray) throws JSONException {
        String encontrados = "";
        String esperados = "";
        String guia = "";
        String bandera = "";
        StringBuilder numPaquete = new StringBuilder();
        // Limpiar las listas antes de agregar los nuevos datos
        tagList.clear();
        tempDatas.clear();
        adapter.notifyDataSetChanged();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            // Procesar valores individuales
            String kGuia = jsonObject.optString("K_Guia", "");
            String nPaquete = jsonObject.optString("NumPaquete", "");
            String epc = jsonObject.optString("EPC", "");
            String descripcion = jsonObject.optString("Descripcion", "");
            String cantidad = jsonObject.optString("Cantidad", "");

            numPaquete.append(nPaquete).append(",");

            // Validar claves adicionales
            if (jsonObject.has("CantidadEncontrada") && jsonObject.has("art_esperados")) {
                encontrados = jsonObject.optString("CantidadEncontrada", "0");
                esperados = jsonObject.optString("art_esperados", "0");
                guia = jsonObject.optString("k_Guia", "0");
                bandera = jsonObject.optString("Bandera", "0");
            } else {
                // Crear un mapa para las listas
                // Crear un mapa con los valores procesados y agregarlo a las listas
                map = new HashMap<>();
                map.put(TAG_EPC, descripcion);
                map.put(TAG_COUNT, cantidad);
                map.put(TAG_RSSI, kGuia);

                // Agregar datos a las listas
                if (!epc.isEmpty()) {
                    tagList.add(map);
                    tempDatas.add(descripcion);
                }
            }
        }

        // Actualizar la interfaz
        actualizarInterfaz(encontrados, esperados, guia, bandera);
    }
    // Método para actualizar la interfaz
    private void actualizarInterfaz(String encontrados, String esperados, String guia, String bandera) {
        adapter.notifyDataSetChanged();
        Et_ArtEsp.setText(esperados);
        TxtEmbarque.setText(guia);
        Et_Bodegas.setText(encontrados);

        // Manejo de las animaciones según la bandera
        switch (bandera) {
            case "1":
                rfidWithUHFA4.outputWgData0On();
                iniciarAnimacionParpadeo(1);
                break;
            case "2":
                iniciarAnimacionParpadeo(2);
                break;
            case "3":
                iniciarAnimacionParpadeo(3);
                break;
        }

        // Acción posterior con retraso
        new Handler().postDelayed(() -> {
            LimpiarValores();
            //iniciarHilo();
            rfidWithUHFA4.outputWgData0Off();
        }, 5000); // 5 segundos
    }

    // Método para manejar errores
    private void ejecutarAccionPostError() {
        new Handler().postDelayed(() -> {
            LimpiarValores();
            //iniciarHilo();
        }, 5000); // 5 segundos
    }

    //Metodos para la clase enviar
    private void mensajesocket() {
        // Direcciones y puertos de los esclavos
        List<String> direcciones = List.of("192.168.1.56");
        List<Integer> puertos = List.of(5053);

        // Enviar mensaje a los esclavos
        enviar.enviarMensaje("Iniciar lectura", direcciones, puertos);

    }

    @Override
    public void onEnvioExitoso(String direccion, int puerto) {
        // Notificar éxito
        System.out.println("Mensaje enviado con éxito a: " + direccion + ":" + puerto);
        mostrarToast("Éxito en " + direccion + ":" + puerto);
    }

    @Override
    public void onEnvioFallido(String direccion, int puerto, Exception e) {
        // Notificar fallo
        System.err.println("Fallo en el envío a: " + direccion + ":" + puerto + " - " + e.getMessage());
        mostrarToast("Fallo en " + direccion + ":" + puerto);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Asegurarse de cerrar el pool de hilos al destruir el fragmento
        if (enviar != null) {
            enviar.cerrar();
        }
    }

}