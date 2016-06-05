package com.lucaszanella.ParthenonCrawler;

import com.lucaszanella.UnespSisgradCrawler.SisgradCrawler;
import com.lucaszanella.SimpleRequest.SimpleRequest;
import java.net.URLEncoder;
import java.net.URL;
import java.io.*;

public class ParthenonCrawler {
    public String username;
    private String password;
    private static String protocol = "https";
    private static String domain = "parthenon.biblioteca.unesp.br";
    //http://www.parthenon.biblioteca.unesp.br/pds
    private Boolean debugMode = false;
    private SimpleRequest parthenonRequest = new SimpleRequest();
    public void doLogin(String username, String password) throws Exception{
        this.domain = domain;
        String postQuery = "bor_id=" + URLEncoder.encode(username, "UTF-8") 
          + "&" + "bor_verification=" + URLEncoder.encode(password, "UTF-8")
          + "&" + "calling_system=" + URLEncoder.encode("", "UTF-8")
          + "&" + "institute=" + URLEncoder.encode("UNESP", "UTF-8")
          + "&" + "selfreg=" + URLEncoder.encode("", "UTF-8")
          + "&" + "url=" + URLEncoder.encode("", "UTF-8");
      
        if (debugMode) {
            System.out.println("logging in to sentinela");
        }
        URL parthenonLogin = new URL(this.protocol + "://" + this.domain + "/" + "pds");
        SimpleRequest.requestObject loginRequest = parthenonRequest.SimpleRequest(parthenonLogin, postQuery); //calls the login url, POSTing the query with user and password
    }
}