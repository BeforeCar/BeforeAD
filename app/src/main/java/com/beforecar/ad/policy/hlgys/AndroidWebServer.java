package com.beforecar.ad.policy.hlgys;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

//自定义一个Server继承NanoHTTPD
public class AndroidWebServer extends NanoHTTPD {

    public AndroidWebServer(int port) throws IOException {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello AutoPy</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("code") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your code: <input type='text' name='code'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("code") + "!</p>";
            return newFixedLengthResponse(msg + "</body></html>\n");
        }
        return newFixedLengthResponse(msg + "</body></html>\n");

    }
}