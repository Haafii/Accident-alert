package com.leap.accidentalert;

import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.net.URI;
import java.net.URISyntaxException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;


public class WebSocketClientL {
    URI uri;
    public WebSocketClient client;
    public WebSocketClientL(String server){

        try{
            uri = new URI(server);
        }
        catch (URISyntaxException e){return;}
        client=new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {

                MainActivity.WebSocketButton.setBackgroundColor(Color.parseColor("#FF58FF00"));
                MainActivity.WebSocketButton.setText("connected");
                client.send("Hello world");
                MainActivity.activity.NotifyConnected();
            }
            @Override
            public void onMessage(String message) {

                try {
                    Log.d(">>>>>",message);
                    JSONObject obj = new JSONObject(message);
                    switch (obj.getString("status")){
                        case "connected":
                            MainActivity.activity.NotifyConnected();
                            break;
                        case "accident":
//                            MainActivity.activity.NotifyAccident();
                            break;
                        case "overspeed":
                            MainActivity.activity.NotifyOverspeed();
                            break;
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onClose(int code, String reason, boolean remote) {
                MainActivity.WebSocketButton.setBackgroundColor(Color.parseColor("#FF0000"));
            }

            @Override
            public void onError(Exception ex) {

            }
        };
        client.connect();
    }
}
