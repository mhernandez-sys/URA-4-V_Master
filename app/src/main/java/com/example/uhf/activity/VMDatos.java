package com.example.uhf.activity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rscja.team.qcom.deviceapi.S;
import com.example.uhf.fragment.TAGreaderprodu;

import java.util.ArrayList;

public class VMDatos extends ViewModel {
    private MutableLiveData<ArrayList<String>> arrayObDescripcion = new MutableLiveData<>();
    private MutableLiveData<ArrayList<String>> arrayProdEsperados = new MutableLiveData<>();
    private MutableLiveData<ArrayList<String>> ArrayListOrden = new MutableLiveData<>();
    private MutableLiveData<ArrayList<String>> ArrayListEmbarque = new MutableLiveData<>();
    private MutableLiveData<ArrayList<String>> ArrarListTags = new MutableLiveData<>();

    public MutableLiveData<ArrayList<String>> getArrayObDescripcion() {
        return arrayObDescripcion;
    }

    public MutableLiveData<ArrayList<String>> getArrayProdEsperados() {
        return arrayProdEsperados;
    }

    public MutableLiveData<ArrayList<String>> getArrayListOrden() {
        return ArrayListOrden;
    }
    public MutableLiveData<ArrayList<String>> getArrayListEmbarque() {
        return ArrayListEmbarque;
    }
    public MutableLiveData<ArrayList<String>> getArrarListTags(){return  ArrarListTags;}
}
