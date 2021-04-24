package com.example.myapp.web;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class WebHelper {
    public void method(){
        try {
            URL endpoint = new URL("https://localhost");
            HttpsURLConnection myConnection = (HttpsURLConnection) endpoint.openConnection();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
