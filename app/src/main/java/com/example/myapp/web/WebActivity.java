package com.example.myapp.web;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapp.DatabaseHelper;
import com.example.myapp.R;
import com.example.myapp.web.serializable.Table;
import com.example.myapp.web.serializable.Row;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashMap;

public class WebActivity extends AppCompatActivity {

    long id_user = 0;

    protected Button sendDataButton;
    protected Spinner spinUser;
    //protected EditText usernameEditText;
    protected EditText passwordEditText;

    protected DatabaseHelper databaseHelper;
    protected AlertDialog.Builder alertBuilder;
    ArrayList<String> userList;
    protected String username;
    protected String password;

    protected static final String URL = "http://10.0.2.2:9898/ws/webservice";
    protected static final String NAMESPACE = "http://webservice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        sendDataButton = findViewById(R.id.sendDataButton);
        spinUser = findViewById(R.id.spinUser);
        //usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        databaseHelper = new DatabaseHelper(this);
        alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setCancelable(true);

        try {
            userList = new GetUsersTask().execute().get();
            spinUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent,
                                           View itemSelected, int selectedItemPosition, long selectedId) {
                    id_user = selectedId;
                }
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            ArrayAdapter<String> usersArrayAdapter = new ArrayAdapter<String>
                    (this, android.R.layout.simple_spinner_item, userList.toArray(new String[0]));
            usersArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinUser.setAdapter(usersArrayAdapter);

            alertBuilder.setMessage("Список пользователей успешно получен")
                    .create()
                    .show();
        } catch (Exception ex){
            alertBuilder.setMessage(ex.getMessage())
                    .create()
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //Главный метод, срабатывающий при нажатии на кнопку sendDataButton
    public void sendLocalDataAndGetServerData(View view) {
        username = spinUser.getSelectedItem().toString();
        password = passwordEditText.getText().toString();

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setCancelable(true);

        if (username.isEmpty() || password.isEmpty()){
            alertBuilder.setMessage("Имя или пароль не введены.")
                    .create()
                    .show();
            return;
        }

        WebAsyncTask webAsyncTask = new WebAsyncTask();
        try {
            webAsyncTask.execute();
            short result = webAsyncTask.get();
            switch (result){
                case 0: alertBuilder.setMessage("Обмен данными прошёл успешно");
                    break;
                case 1: alertBuilder.setMessage("Не удалось отправить данные");
            }
            alertBuilder.create()
                    .show();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            alertBuilder.setMessage("Ошибка")
                    .create()
                    .show();
        }
    }

    //Метод, ивлекающий данные из таблицы (либо STB, либо STATUS)
    protected Table getLocalData(String tableName) {

        Table table = new Table();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String sqlQuery = "";

        switch (tableName){
            case "stb": sqlQuery = "SELECT * FROM STB WHERE mobiles IS 1";
            break;
            case "status": sqlQuery = "SELECT * FROM STATUS WHERE mobiles IS 1";
        }

        Cursor cursor = db.rawQuery(sqlQuery, null);
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            Row row = new Row();
            for (int i = 0; i < cursor.getColumnCount(); i++){
                if (cursor.getType(i) == cursor.FIELD_TYPE_BLOB){
                    byte[] blob = cursor.getBlob(i);
                    String base64 = Base64.encodeToString(blob, Base64.DEFAULT);
                    row.add(base64);
                }
                row.add(cursor.getString(i));
            }
            table.add(row);
        }
        cursor.close();
        db.close();
        System.out.println("Данные получил");
        return table;
    }

    //Занимается отправкой и принятием сообщений
    protected class WebAsyncTask extends AsyncTask<Void, Void, Short> {

        @Override
        protected Short doInBackground(Void... voids) {
            try {
                String METHOD_NAME = "dataExchangeRequest";
                String SOAP_ACTION = NAMESPACE + "/" + METHOD_NAME;

                SoapObject dataExchangeRequest = new SoapObject(NAMESPACE, METHOD_NAME);
                setDataExchangeRequest(dataExchangeRequest);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                setEnvelope(envelope, dataExchangeRequest);

                HttpTransportSE httpTransportSE = new HttpTransportSE(URL);
                httpTransportSE.call(SOAP_ACTION, envelope);

                SoapObject response = (SoapObject) envelope.bodyIn;
                SoapObject table = (SoapObject) response.getProperty(0);
                System.out.println(response.getPropertyInfo(0).name);
                for (int i = 0; i < table.getPropertyCount(); i++){
                    SoapObject row = (SoapObject) table.getProperty(i);
                    for (int j = 0; j < row.getPropertyCount(); j++){
                        System.out.print(" " + row.getProperty(j));
                    }
                    System.out.println("");
                }
                return 0;
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
                return 1;
            }
        }
    }

    //Настраивает soap-конверт
    protected void setEnvelope(SoapSerializationEnvelope envelope, SoapObject body) {
        envelope.setAddAdornments(false);
        envelope.addMapping(NAMESPACE, "table", Table.class);
        envelope.addMapping(NAMESPACE, "row", Row.class);
        envelope.setOutputSoapObject(body);
    }

    //Настраивает soap-объект в соответствии с DataExchangeRequest
    protected void setDataExchangeRequest(SoapObject request) {
        Table stb = getLocalData("stb");
        Table status = getLocalData("status");

        PropertyInfo piUsername = new PropertyInfo();
        piUsername.setNamespace(NAMESPACE);
        piUsername.setName("username");
        piUsername.setValue(username);
        piUsername.setType(String.class);

        PropertyInfo piPassword = new PropertyInfo();
        piPassword.setNamespace(NAMESPACE);
        piPassword.setName("password");
        piPassword.setValue(password);
        piPassword.setType(String.class);

        PropertyInfo piSTB = new PropertyInfo();
        piSTB.setNamespace(NAMESPACE);
        piSTB.setName("stb");
        piSTB.setValue(stb);
        piSTB.setType(Table.class);

        PropertyInfo piStatus = new PropertyInfo();
        piStatus.setNamespace(NAMESPACE);
        piStatus.setName("status");
        piStatus.setValue(status);
        piStatus.setType(Table.class);

        request.addProperty(piUsername)
                .addProperty(piPassword)
                .addProperty(piSTB)
                .addProperty(piStatus);
    }


    protected class GetUsersTask extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            String METHOD_NAME = "getUsersRequest";
            String SOAP_ACTION = "http://webservice/" + METHOD_NAME;

            SoapObject soapObject = new SoapObject(NAMESPACE, METHOD_NAME);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(soapObject);

            HttpTransportSE httpTransportSE = new HttpTransportSE(URL);
            try {
                httpTransportSE.call(SOAP_ACTION, envelope);
                SoapObject response = (SoapObject) envelope.bodyIn;

                ArrayList<String> userList = new ArrayList<String>();
                SoapObject usersen = (SoapObject) response.getProperty(0);
                for (int i = 0; i < usersen.getPropertyCount(); i++){
                    SoapObject row = (SoapObject) usersen.getProperty(i);
                    String username = row.getPropertyAsString(1);
                    userList.add(username);
                }
                return userList;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    //Тестовый метод. Не используется. Первый аргумент - входные данные, второй - промежуточные, третий - выходные данные
    protected class WAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String METHOD_NAME = "getDataFromServerRequest";
                String SOAP_ACTION = "http://webservice/" + METHOD_NAME;

                SoapObject soapObject = new SoapObject(NAMESPACE, METHOD_NAME);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                //envelope.implicitTypes = true;
                envelope.setAddAdornments(false);
                //envelope.avoidExceptionForUnknownProperty = true;
                //envelope.addMapping(NAMESPACE, "testGetDataRequest", SerializableTestGetDataRequest.class);
                //envelope.addMapping(NAMESPACE, "plusRequest", SerializablePlusRequest.class);
                //envelope.addMapping(NAMESPACE, "data", SerializableData.class);
                //envelope.addMapping(NAMESPACE, "testResponse", SerializableTestResponse.class);
                envelope.setOutputSoapObject(soapObject);

                //10.0.2.2 - адрес связи с вебсервисом на локальном компьютере с эмулятора
                HttpTransportSE httpTransportSE = new HttpTransportSE(URL);
                httpTransportSE.call(SOAP_ACTION, envelope);
                SoapObject response = (SoapObject) envelope.bodyIn;
                for (int i = 0; i < response.getPropertyCount(); i++){
                    System.out.println(response.getProperty(i));
                }
//                System.out.println("Response: " + response.getProperty("response") + " "
//                        + response.getProperty("answer") + " " + response.getProperty(2) + " " +
//                        response.getPropertyCount() + " " + response.getProperty(4));
//                SerializableTestResponse response = (SerializableTestResponse) envelope.bodyIn;
//                System.out.println("Response: " + response.getResponse() + " " + response.getAnswer() + response.getArray());
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
