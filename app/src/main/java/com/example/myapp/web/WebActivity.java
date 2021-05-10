package com.example.myapp.web;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapp.DatabaseHelper;
import com.example.myapp.R;
import com.example.myapp.web.serializable.DataArray;
import com.example.myapp.web.serializable.SerializablePlusRequest;
import com.sibsiu.dbwebservice.wsdl.PlusRequest;
import com.example.myapp.web.serializable.TestGetDataRequest;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.MarshalBase64;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;

public class WebActivity extends AppCompatActivity {
    Button sendDataButton;
    Button getDBDataButton;

    String sqlQuery;
    DatabaseHelper databaseHelper;
    SQLiteDatabase db;
    TestGetDataRequest dataRequest;

    static final String URL = "http://10.0.2.2:9898/ws/webservice";
    static final String NAMESPACE = "http://webservice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        sendDataButton = (Button) findViewById(R.id.sendDataButton);
        getDBDataButton = (Button) findViewById(R.id.getDBDataButton);
        databaseHelper = new DatabaseHelper(this);
        dataRequest = new TestGetDataRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void getDBData(View view) {
        db = databaseHelper.getReadableDatabase();
        sqlQuery = "SELECT STB._id, tab1.date as date, ORG.nameOrg as org, locats.nameloc as loc, tab1.namest as status, texts.name1 as desc\n" +
                "FROM\n" +
                "(((((SELECT idstb, MAX(begins) as date, idstatusname, statusN.namest \n" +
                "FROM STATUS INNER JOIN statusN ON STATUS.idstatusname = statusN._id\n" +
                "GROUP BY (idstb)) AS tab1) \n" +
                "INNER JOIN STB ON STB._id=tab1.idstb) \n" +
                "INNER JOIN ORG ON STB.idorg = ORG._id) \n" +
                "INNER JOIN locats ON STB.idl = locats._id) \n" +
                "INNER JOIN texts ON STB.idt = texts._id\n" +
                "WHERE tab1.idstatusname < 90\n" +
                "ORDER BY STB._id DESC";
        Cursor cursor = db.rawQuery(sqlQuery, null);
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            TestGetDataRequest.Data data = new TestGetDataRequest.Data();
            data.setId(cursor.getString(0));
            data.setDate(cursor.getString(1));
            data.setOrg(cursor.getString(2));
            data.setLoc(cursor.getString(3));
            data.setStatus(cursor.getString(4));
            data.setDesc(cursor.getString(5));
            dataRequest.getData().add(data);
        }
        System.out.println("Данные получил");
        cursor.close();
        db.close();
    }

    //Отправить запрос с данными вебсерверу
    public void sendData(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);

        WebAsyncTask myTask = new WebAsyncTask();
        myTask.execute();
        try {
            Boolean success = myTask.get();
            String message = success ? "Сообщение отправлено" : "Ошибка";
            builder.setMessage(message);
            AlertDialog alertSuccess = builder.create();
            alertSuccess.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            builder.setMessage("Даже get не сработал");
            AlertDialog alertSuccess = builder.create();
            alertSuccess.show();
        }
    }

    //Первый аргумент - входные данные, второй - промежуточные, третий - выходные данные
    protected class WebAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
//                SerializablePlusRequest plusRequest = new SerializablePlusRequest(1,2, "");
//                JAXBContext contextData = JAXBContext.newInstance("com.sibsiu.dbwebservice.wsdl");
//                StringWriter writer = new StringWriter();
//                Marshaller m = contextData.createMarshaller();
//                m.marshal(plusRequest, writer);
//                System.out.println("XML реквест:" + writer.toString());

                TestGetDataRequest.Data data1 = new TestGetDataRequest.Data();
                data1.setId("1");
                data1.setDate("Date1");
                data1.setOrg("Org1");
                data1.setLoc("Loc1");
                data1.setStatus("Status1");
                data1.setDesc("Desc1");

                TestGetDataRequest.Data data2 = new TestGetDataRequest.Data();
                data2.setId("2");
                data2.setDate("Date2");
                data2.setOrg("Org2");
                data2.setLoc("Loc2");
                data2.setStatus("Status2");
                data2.setDesc("Desc2");

                DataArray dataArray = new DataArray();
                dataArray.add(data1);
                dataArray.add(data2);

                String METHOD_NAME = "testGetDataRequest";
                String SOAP_ACTION = NAMESPACE + "/" + METHOD_NAME;

                SoapObject soapObject = new SoapObject(NAMESPACE, METHOD_NAME);

                PropertyInfo arrayPI = new PropertyInfo();
                arrayPI.setName("data");
                arrayPI.setValue(dataArray);
                arrayPI.setType(DataArray.class);
                soapObject.addProperty(arrayPI);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                //envelope.addMapping("http://webservice", "testGetDataRequest", TestGetDataRequest.class);
                envelope.addMapping("http://webservice", "dataLine", TestGetDataRequest.Data.class);
                envelope.addMapping(NAMESPACE, "data", DataArray.class);
                envelope.setOutputSoapObject(soapObject);

                //10.0.2.2 - адрес связи с вебсервисом на локальном компьютере с эмулятора
                HttpTransportSE httpTransportSE = new HttpTransportSE(URL);
                httpTransportSE.setXmlVersionTag("<?xml version=\"1.1\" encoding=\"utf-8\"?>");
                httpTransportSE.call(SOAP_ACTION, envelope);
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
