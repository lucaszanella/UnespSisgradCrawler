package com.lucaszanella.ParthenonCrawler;

import com.lucaszanella.SimpleRequest.SimpleHTTPSRequest;

import java.net.URL;
import java.net.URLEncoder;

public class ParthenonCrawler {
    public String username;
    private String password;
    private static String protocol = "https";
    private static String domain = "parthenon.biblioteca.unesp.br";
    //http://www.parthenon.biblioteca.unesp.br/pds
    private Boolean debugMode = false;
    private SimpleHTTPSRequest parthenonRequest = new SimpleHTTPSRequest();
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
        SimpleHTTPSRequest.requestObject loginRequest = parthenonRequest.SimpleHTTPSRequest(parthenonLogin, postQuery); //calls the login url, POSTing the query with user and password
    }
}