package com.lucaszanella.SimpleRequest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//import org.jsoup.*;

//Just a Class designed to facilitate HTTPs requests to Unesp's Sisgrad server, that will return HTML pages as strings
//

//System.setProperty("jsse.enableSNIExtension", "false"); CONSERTAR ESTE PROBLEMA
public class SimpleHTTPRequest {
    /*
    public String response;
    public String responseCode;
    public String responseMessage;
    public String location
    */
    public List < List < String >> cookies = new ArrayList < List < String >> ();//list of all cookies that will be received during all requests
    public Boolean debugMode = false;
    public SimpleHTTPRequest () {

    }
    public class requestObject {
        public String response;
        public String responseCode;
        public String responseMessage;
        public String location;
        public List<String> cookies;
        public requestObject(String response, String responseCode, String responseMessage, String location, List<String> cookies) {
            this.response = response;
            this.responseCode = responseCode;
            this.responseMessage = responseMessage;
            this.location = location;
            this.cookies = cookies;
        }
    }
    public List < List < String >> getCookies () {
      return this.cookies;
    }
    public requestObject SimpleHTTPRequest(URL url, String postQuery) throws Exception {
        List<String> responseCookies;//cookies for this specific request
        if (debugMode) {
            System.out.println("calling " + url);
        }
        //System.out.println("query "+postQuery);
        if (debugMode) {
            System.out.println("cookies " + cookies);
        }
        List<String> requestCookies = new ArrayList<String>();//cookies that will be selected from cookiesList and sent in this request
        if (this.cookies != null) {//Here we'll send only the cookies for the specific path of the URL
            //Choose the cookies that match the path being requested.
            //System.out.println("cookies not null");

            for (List<String> cookieList : this.cookies) {
                if (debugMode) {
                    //System.out.println("inside cookies: " + cookieList);
                }
                for (String singleCookie : cookieList) {
                    //System.out.println("single cookie is: "+singleCookie);
                    String search = "";
                    try {
                      search = singleCookie.split("Path")[1].split("/")[1];
                    } catch (Exception e) {
                      //System.out.println("exception in cookie cutting, maybe it was Path=/ or cookie doesn't have 'Path'");
                    }
                    if (debugMode) {
                        System.out.println("search " + search);
                    }
                    if (url.getPath().split("/")[1].contains(search)) { //ANALISAR URL COM CUIDADO //if URL contains the path for this cookie, use it
                        requestCookies = cookieList;
                        if (debugMode) {
                            System.out.println("chose " + search);
                        }
                        if (debugMode) {
                            System.out.println("url.getPath() cutted: " + url.getPath().split("/")[1]);
                        }
                    }
                }
            }
        } else {
            if (debugMode) {
                System.out.println("cookies is null");
            }
        }
        if (debugMode) {
            System.out.println("cookie being used: " + requestCookies);
        }

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setInstanceFollowRedirects(false);
        if (postQuery != null) {
            con.setRequestProperty("Content-length", String.valueOf(postQuery.length()));
        }
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 5.0;Windows98;DigExt)");
        con.setDoInput(true);
        if (requestCookies != null) {
            for (String cookie : requestCookies) {
                con.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
            }
        }
        if (postQuery != null) {
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            DataOutputStream output = new DataOutputStream(con.getOutputStream());
            output.writeBytes(postQuery);
            output.close(); //VERIFICAR ISSO!
        }

        //DataInputStream dis = new DataInputStream( con.getInputStream() );
        String charset = "UTF-8";
        BufferedReader buff = new BufferedReader(
                new InputStreamReader(con.getInputStream(), charset));
        String response = "";
        String line;
        while ((line = buff.readLine()) != null) {
            response += line + "\n";
        }

        //use inputLine.toString(); here it would have whole source
        //String response = inputLine.toString();

        responseCookies = con.getHeaderFields().get("Set-Cookie");
        if (debugMode) {
            System.out.println("Cookies got: " + responseCookies);
        }
        String responseCode = Integer.toString(con.getResponseCode());
        //List<String> location = new List<String>();
        String responseMessage = con.getResponseMessage();
        String location = "";
        if (responseCode.equals("302")) {
            location = con.getHeaderFields().get("Location").get(0);
        }
        if (responseCookies != null) {
            //System.out.println("adding following cookies: "+responseCookies);
            this.cookies.add(responseCookies);
            //System.out.println("this.cookies: "+this.cookies);
        }
        return new requestObject(response, responseCode, responseMessage, location, responseCookies);
    }
}