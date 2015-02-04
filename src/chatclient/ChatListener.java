package chatclient;

import java.util.ArrayList;

public interface ChatListener {

    void messageArrived(String data);

    void updateUserList(String[] users);
}
