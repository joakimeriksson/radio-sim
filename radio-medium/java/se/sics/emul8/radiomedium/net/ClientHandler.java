package se.sics.emul8.radiomedium.net;

import com.eclipsesource.json.JsonObject;

public interface ClientHandler {

    public boolean handleMessage(ClientConnection clientConnection, JsonObject json);

    public void clientClosed(ClientConnection clientConnection);

}
