package com.example.uhf.fragment;

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
import com.rscja.deviceapi.exception.ConfigurationException;

public class TAGreaderprodu extends KeyDownFragment {

    // Declarar una variable booleana para controlar el estado del hilo
    private boolean hiloActivo = true;
    // Variable para almacenar el estado anterior del GPIO
    private int previousState = -1; // Inicializar con un valor que no puede ser un estado válido
    RFIDWithUHFA4 rfidWithUHFA4 = null;
    //    private final Semaphore semaphore = new Semaphore(1);
    private static String TAG = "UHFReadTagFragment";
    private boolean loopFlag = false;
    private int inventoryFlag = 1;
    public ArrayList<HashMap<String, String>> tagList;
    private String NAMESPACE = "";
    private String URL = "";
    private String METHOD_NAME = "";
    private String SOAP_ACTION = "";
    private WebServiceManager webServiceManager;

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

    ExecutorService executorService = null;
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

        Thread gpioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                monitorizarCambiosGPIO();
            }
        });
        gpioThread.start();
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
                CadenaEPCS += Epc + "+";

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
                ProgressBar(CadenaEPCS);
                if (map == null) {
                    LimpiarValores();
                    reanudarHilo();
                }
            }
        }
    }

    public class BtClearClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            LimpiarValores();
            reanudarHilo();
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
            detenerHilo();
            readTag();
            //enviarNotificacion();
            mensajesocket();
            ///ProgressBar("E28011606000020EB7OC5DBB+001804BA0460B527A68B52F4+E28011606000020EB70C4CB3+");
            ProgressBar2("'1111222233334444924145E2','E280116060000208EBCEA56E', 'E280116060000209924145E4', 'E280116060000209924145E5'");
        }
    }

    ///Esta es la funcion Maestro
    private void mensajesocket() {
        Enviar enviar = new Enviar();
        enviar.execute("Estral ejecutar programa");
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
        } else {// detener el reconocimiento
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
     * 停止识别
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

    public void ProgressBar(final String EPCTag) {

        String res = "";
        if (EPCTag.isEmpty()) {
            mostrarToast("No se detecto ningun TAG \n" +
                    "Realizar una comprobacion de TAGS");
            return;
        }
        String ultimocaracter = EPCTag.substring(0, EPCTag.length() - 1);
        String Tags = ultimocaracter;
        Thread nt = new Thread(new Runnable() {

            @Override
            public void run() {
                NAMESPACE = "http://tag_android.org/";
                URL = "http://192.168.1.83/TAGSSERver.asmx";
                METHOD_NAME = "ProcesarTAGS";
                SOAP_ACTION = NAMESPACE + METHOD_NAME;
                String res = "";

                try {
                    //Se crea el objeto SOAP
                    SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                    //Se gregan las propiedades que se van a enviar
                    request.addProperty("EPCTag", Tags);

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
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (finalRes.equals("Embarque no encontrado")) {
                                mostrarToast("Embarque no encontrado");
                                LimpiarValores();
                                reanudarHilo();
                            } else {

                                String[] partes = finalRes.split("\\+");
                                String art_encontrados = partes[1];
                                String art_esperados = partes[0];
                                String cadena = partes[2];
                                String[] caracter1 = cadena.split("\\?");
                                String numpiezas = partes[3];
                                String[] caracter2 = numpiezas.split("\\¿");
                                String embarque = partes[4];
                                String etiqueta = partes[5];
                                String pedidos = partes[6];
                                String[] caracter3 = pedidos.split("\\-");

                                Et_ArtEsp.setText(art_esperados);
                                TXTART_ENC.setText(art_encontrados);
                                TxtEmbarque.setText(embarque);

                                if (etiqueta.equals("1")) {
                                    iniciarAnimacionParpadeo(1);
                                } else if (etiqueta.equals("2")) {
                                    iniciarAnimacionParpadeo(2);
                                } else if (etiqueta.equals("3")) {
                                    iniciarAnimacionParpadeo(3);
                                }

                                OpcionSelect2(pedidos);

                                tagList.clear();
                                tempDatas.clear();
                                adapter.notifyDataSetChanged();

                                for (int i = 0; i < caracter1.length; i++) {
                                    String variable = caracter1[i];
                                    String variable1 = caracter2[i];
                                    int index = checkIsExist(variable);
                                    map = new HashMap<>();
                                    map.put(TAG_EPC, variable); //Este es el EPC que se imprime en la pantalla
                                    map.put(TAG_COUNT, variable1); //Esta es para el conteo de los productoas
                                    //map.put(TAG_RSSI, rssi); //Estos dos son para el numero de paquete
                                    //map.put(TAG_ANT, ant);
                                    //El index determina si el epc ha sido leido, si no lo imprime y en caso contrario lo salta
                                    if (index == -1) {
                                        ///En esta funcion se le debe  deagregar los epcs y almacenarlos en una variable publica para posteriormente mandar la al web service
                                        tagList.add(map);
                                        tempDatas.add(variable);
                                        tv_count.setText(String.valueOf(adapter.getCount()));///En esta parte se le agrega el epc que no han sido leidos
                                    }
                                }
                                tv_totalNum.setText(art_encontrados);
                                adapter.notifyDataSetChanged();
                            }
                            //Termina el else
                        } catch (Exception e) {
                            e.printStackTrace();
                            //mensajes("");
                        }
                    }
                });
            }
        });
        nt.start();
    }

    private void OpcionSelect2(String respuesta) {

        // Aquí debes implementar la lógica para procesar la respuesta y asignar a diferentes variables
        // Por ejemplo, asumiendo que la respuesta está en un formato separado por comas:
        // Aquí debes implementar la lógica para procesar la respuesta y asignar a diferentes variables
        // Por ejemplo, asumiendo que la respuesta está en un formato separado por comas:
        char caracterAEliminar = '}'; // Por ejemplo, eliminar la coma
        char caracterAEliminar2 = ']'; // Por ejemplo, eliminar la coma
        // Reemplazar el carácter con una cadena vacía
        ///Con esta linea se elimina el parametro } recordar que lleva comillas sinples
        String cadenaSinCaracter = respuesta.replace(String.valueOf(caracterAEliminar), "");
        String cadenaSinCaracter2 = cadenaSinCaracter.replace(String.valueOf(caracterAEliminar2), "");

        String[] caracter2 = cadenaSinCaracter2.split("\\-");


        String pedidos = caracter2[0];
        String bodegas = caracter2[1];
        String partidas = caracter2[2];

        if (pedidos.contains(",")) {
            pedidos = procesartextos(pedidos, 1);
            Et_Pedidos.setText(pedidos);
        } else {
            pedidos = procesartextos(pedidos, 2);
            Et_Pedidos.setText(pedidos);
        }

        if (bodegas.contains(",")) {
            procesartextos(bodegas, 1);
        } else {
            bodegas = procesartextos(bodegas, 2);
            Et_Bodegas.setText(bodegas);
        }
        if (partidas.contains(",")) {
            partidas = procesartextos(partidas, 1);
            Et_Partidas.setText(partidas);
        } else {
            partidas = procesartextos(partidas, 2);
            Et_Partidas.setText(partidas);
        }
    }

    public static String procesartextos(String nombre, int pedido) {
        String resultado = "";

        if (pedido == 1) {
            String[] Matriz = nombre.split(",");

            for (int i = 0; i < Matriz.length; i++) {
                String variable = Matriz[i];
                String[] partes1 = variable.split(":");
                String variable1 = partes1[1];
                ///En esta parte se le quitan las comillas
                String[] partes2 = variable1.split("\"");
                String variable2 = String.join("", partes2);
                resultado += variable2 + ",";
            }
            String ultimocaracter = resultado.substring(0, resultado.length() - 1);
            resultado = ultimocaracter;

        } else if (pedido == 2) {
            String variable = nombre;
            String[] partes1 = variable.split(":");
            String variable1 = partes1[1];
            ///En esta parte se le quitan las comillas
            String[] partes2 = variable1.split("\"");
            String variable2 = String.join("", partes2);
            resultado = variable2;
        }
        return resultado;


    }


    public void Reiniciar(final String Embarque) {
        final String finalRes = "";
        String res = "";

        Thread nt = new Thread(new Runnable() {

            @Override
            public void run() {
                NAMESPACE = "http://tag_android.org/";
                URL = "http://192.168.1.73/TAGSSERver.asmx";
                METHOD_NAME = "Reiniciar";
                SOAP_ACTION = NAMESPACE + METHOD_NAME;
                String res = "";

                try {
                    //Se crea el objeto SOAP
                    SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                    //Se gregan las propiedades que se van a enviar
                    request.addProperty("Embarque", Embarque);

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

                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mostrarToast(finalRes);
                    }
                });
            }
        });
        nt.start();
    }

    private void iniciarAnimacionParpadeo(final int Activacion) {
        AlphaAnimation parpadeo = new AlphaAnimation(1f, 0f); // De totalmente visible a totalmente invisible
        parpadeo.setDuration(500); // Duración de cada fase del parpadeo en milisegundos
        parpadeo.setRepeatMode(Animation.REVERSE);
        parpadeo.setRepeatCount(Animation.INFINITE);
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
                e.printStackTrace();
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
                String valor = tv_time.getText().toString();
                if (currentState == 1 && valor.equals("0s")) {
                    readTag();
                    detenerHilo();
                }
                previousState = currentState; // Actualizar el estado anterior
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    // Método para detener el hilo
    private void detenerHilo() {
        hiloActivo = false;
    }

    // Método para reanudar el hilo
    private void reanudarHilo() {
        hiloActivo = true;
        // Crear y comenzar un nuevo hilo para la monitorización
        Thread nuevoHilo = new Thread(this::monitorizarCambiosGPIO);
        nuevoHilo.start();
    }

    private void ProgressBar2(String EPCTAG) {
        // Crear el mapa de propiedades para enviar
        Map<String, String> properties = new HashMap<>();
        properties.put("EPCTAG", EPCTAG);

        // Llamar al web service utilizando WebServiceManager
        webServiceManager.callWebService("ProcesarGuia", properties, result -> {
            JSONArray jsonArray = new JSONArray(result);

            //Limpiar las listas antes de agregar los nuevos datos
            tagList.clear();
            tempDatas.clear();
            adapter.notifyDataSetChanged();

            //Recorrer cada objeto del array JSON
            for (int i = 0; i<jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                //Extraer valores individuales
                String k_Guia = jsonObject.optString("K_Guia","");
                String NumPaquete = jsonObject.optString("NumPaquete","");
                String EPC = jsonObject.optString("EPC", "");
                String Partida_Estral = jsonObject.optString("Partida_Estral", "");
                String Descripcion = jsonObject.optString("Descripcion", "");
                String Cantidad  = jsonObject.optString("Cantidad", "");

                //Comprobar si el objeto actual tiene los valores adicionales
                if (jsonObject.has("CantidadEncontrada") && jsonObject.has("art_esperados")){
                    String CantidadEncontrada = jsonObject.optString("CantidadEncontrada", "0");
                    String art_esperados = jsonObject.optString("art_esperados", "0");
                    String Guia = jsonObject.optString("k_Guia", "0");

                    String mensajes = "Guía:" + Guia + " / Encontrados: " + CantidadEncontrada + " / Esperados: " + art_esperados;
                    //Mensaje que visuliza los resultados
                    Toast.makeText(getContext(), mensajes, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void procesarEPCTag(String epcTag) {
        // Aquí agregas la lógica específica para manejar el EPCTag
        if (epcTag.startsWith("ABC")) {
            iniciarAnimacionParpadeo(1);
        } else if (epcTag.startsWith("XYZ")) {
            iniciarAnimacionParpadeo(2);
        } else {
            iniciarAnimacionParpadeo(3);
        }
    }
}
