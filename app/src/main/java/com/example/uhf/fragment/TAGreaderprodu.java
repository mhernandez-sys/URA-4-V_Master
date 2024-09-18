package com.example.uhf.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
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
import android.widget.ProgressBar;
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
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.rscja.deviceapi.exception.ConfigurationException;

public class TAGreaderprodu extends KeyDownFragment {

    // Declarar una variable booleana para controlar el estado del hilo
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> futureTask;
    private boolean hiloActivo = false;
    private int previousState = -1; // Estado anterior inicializado
    RFIDWithUHFA4 rfidWithUHFA4 = null;
    //    private final Semaphore semaphore = new Semaphore(1);
    private static String TAG = "UHFReadTagFragment";
    private boolean loopFlag = false;
    private int inventoryFlag = 1;
    public ArrayList<HashMap<String, String>> tagList;
    private WebServiceManager webServiceManager;
    private int i = 0;

    private boolean isProgressing = false;

    SimpleAdapter adapter;
    Button BtClear;
    TextView tv_count, tv_totalNum, tv_time;
    RadioGroup RgInventory;
    RadioButton RbInventorySingle;
    RadioButton RbInventoryLoop;
    TextView Et_ArtEsp, Et_Pedidos, TXTART_ENC, Et_Partidas, Et_Bodegas, TxtEmbarque;
    //EditText etTime;
    ProgressBar PB_Ariculos;
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
        Et_ArtEsp = (TextView) getView().findViewById(R.id.Et_ArtEsp);
        Et_Pedidos = (TextView) getView().findViewById(R.id.Et_Pedidos);
        Et_Bodegas = (TextView) getView().findViewById(R.id.Et_Bodegas);
        Et_Partidas = (TextView) getView().findViewById(R.id.Et_Partidas);
        TXTART_ENC = (TextView) getView().findViewById(R.id.TXTART_ENC);
        TxtEmbarque = (TextView) getView().findViewById(R.id.TxtEmbarque);
        //mContext.llenarspinnerembarque(spinnerEmbarque, "0");
        PB_Ariculos = (ProgressBar) getView().findViewById(R.id.PB_Ariculos);
        PB_Ariculos.setProgress(0);
        MSAlerta = (TextView) getView().findViewById(R.id.MSAlerta);
        MSAlertaincompletos = (TextView) getView().findViewById(R.id.MSAlertaincompletos);
        MSAlertaActivo = (TextView) getView().findViewById(R.id.MSAlertaActivo);
        MSAlerta.setVisibility(View.GONE);
        MSAlertaincompletos.setVisibility(View.GONE);
        MSAlertaActivo.setVisibility(View.GONE);
        webServiceManager = new WebServiceManager(requireContext());

        iniciarHilo();


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
        //initFilter(view); // 初始化过滤
        clearData();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (!isVisibleToUser) {
            // 停止识别
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
            timer = 10;
            if (dTime >= timer) {
                //String valorembarque = TxtEmbarque.getSelectedItem().toString();
                stopInventory();
                String contador = tv_count.getText().toString();
                // Remover la última coma
                if (CadenaEPCS.length() > 0) {
                    CadenaEPCS = CadenaEPCS.substring(0, CadenaEPCS.length() - 1);
                }
                ProgressBar(CadenaEPCS);
                if (map == null) {
                    LimpiarValores();
                    iniciarHilo();
                }
            }
        }
    }

    public class BtClearClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            LimpiarValores();
            iniciarHilo();
        }
    }

    private void clearData() {
        totalNum = 0;
        tv_count.setText("0");
        tv_totalNum.setText("0");
        tv_time.setText("0s");
        tagList.clear();
        tempDatas.clear();
        adapter.notifyDataSetChanged();
    }

    private void LimpiarValores() {
        totalNum = 0;
        TxtEmbarque.setText("");
        tv_count.setText("");
        tv_totalNum.setText("");
        tv_time.setText("0s");
        Et_ArtEsp.setText("");
        TXTART_ENC.setText("");
        Et_Pedidos.setText("");
        Et_Partidas.setText("");
        Et_Bodegas.setText("");
        PB_Ariculos.setProgress(0);
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
            //detenerHilo();
         //   readTag();
            //enviarNotificacion();
            mensajesocket();
            ///ProgressBar("E28011606000020EB7OC5DBB+001804BA0460B527A68B52F4+E28011606000020EB70C4CB3+");
            //ProgressBar2("'E280116060000208EBCEA56E', 'E280116060000209924145E4', 'E280116060000209924145E5'");
            //1111222233334444924145E2'.
        }
    }

    ///Esta es la función para el Maestro
    private void mensajesocket() {
        Enviar enviar = new Enviar();
        enviar.enviarMensaje("Estral ejecutar programa");
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
            //    setTotalTime();
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
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (mContext.mReader.stopInventory()) {
                        SystemClock.sleep(200);
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BtInventory.setText(mContext.getString(R.string.btInventory));
                                BtClear.setBackgroundColor(getResources().getColor(R.color.txtblue));
                                loopFlag = false;
                                setViewEnabled(true);
                            }
                        });
                    } else {
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UIHelper.ToastMessage(mContext, R.string.uhf_msg_inventory_stop_fail);
                                loopFlag = false;
                                setViewEnabled(true);
                            }
                        });
                    }
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void ProgressBar(String EPCTAG) {


        if (EPCTAG.isEmpty()) {
            iniciarHilo();
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


        webServiceManager.callWebService("ProcesarGuia", properties, result -> {
            // Ocultar el ProgresDialog
            progressDialog.dismiss();
            isProgressing = false;

            // Limpiar las listas antes de agregar los nuevos datos
            tagList.clear();
            tempDatas.clear();
            adapter.notifyDataSetChanged();

            if (result.contains("Error") || result.contains("Time out")) {
                iniciarAnimacionParpadeo(3);
                mostrarToast("No se pudo determinar la guía para el EPCTAG proporcionado.");
                // Esperar 5 segundos antes de limpiar los valores
                new Handler().postDelayed(() -> {
                    LimpiarValores(); // Llama a la función para limpiar los valores
                    iniciarHilo();
                }, 5000); // 5000 milisegundos = 5 segundos

                return;
            }

            JSONArray jsonArray = new JSONArray(result);

            String Encontrados = "";
            String Esperados = "";
            String Guia = "";
            String Bandera = "";
            StringBuilder Num_Paquete = new StringBuilder();

            // Recorrer cada objeto del array JSON
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Extraer valores individuales
                String k_Guia = jsonObject.optString("K_Guia", "");
                String NPaquete = jsonObject.optString("NumPaquete", "");
                String EPC = jsonObject.optString("EPC", "");
                String Partida_Estral = jsonObject.optString("Partida_Estral", "");
                String Descripcion = jsonObject.optString("Descripcion", "");
                String Cantidad = jsonObject.optString("Cantidad", "");
                Num_Paquete.append(NPaquete).append(",");

                // Comprobar si el objeto actual tiene los valores adicionales
                if (jsonObject.has("CantidadEncontrada") && jsonObject.has("art_esperados")) {
                    Encontrados = jsonObject.optString("CantidadEncontrada", "0");
                    Esperados = jsonObject.optString("art_esperados", "0");
                    Guia = jsonObject.optString("k_Guia", "0");
                    Bandera = jsonObject.optString("Bandera", "0");
                    String mensajes = "Guía: " + Guia + " / Encontrados: " + Encontrados + " / Esperados: " + Esperados;
                }

                // Crear un mapa con los valores procesados y agregarlo a las listas
                map = new HashMap<>();
                map.put(TAG_EPC, Descripcion); // Este es el EPC que se imprime en la pantalla
                map.put(TAG_COUNT, Cantidad);
                map.put(TAG_RSSI, k_Guia); // Estos dos son para el numero de paquete

                // Añadir los datos a la lista de tags si el EPC es válido
                if (!EPC.isEmpty()) {
                    tagList.add(map);
                    tempDatas.add(Descripcion);
                    tv_count.setText(String.valueOf(adapter.getCount()));  // En esta parte se le agrega el EPC que no han sido leídos
                }
                // Actualizar el adaptador para reflejar los cambios
                adapter.notifyDataSetChanged();
                Et_ArtEsp.setText(Esperados);
                TxtEmbarque.setText(Guia);
                Et_Bodegas.setText(Encontrados);

                // Manejo de las condiciones para la animación
                    if (Bandera.equals("1")) {
                        iniciarAnimacionParpadeo(1);
                    } else if (Bandera.equals("2")) {
                        iniciarAnimacionParpadeo(2);
                    } else if (Bandera.equals("3")) {
                        iniciarAnimacionParpadeo(3);
                    }


                // Esperar 5 segundos antes de limpiar los valores
                new Handler().postDelayed(() -> {
                    LimpiarValores(); // Llama a la función para limpiar los valores
                    iniciarHilo();
                }, 5000); // 5000 milisegundos = 5 segundos

            }

        });
    }
}