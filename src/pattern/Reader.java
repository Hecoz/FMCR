package pattern;

import com.google.gson.*;
import engine.trace.*;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class Reader {

    public static String readFromFile(String filename) {
        try {
            File f = new File(filename);
            FileReader fileReader = new FileReader(f);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String str;
            StringBuffer content = new StringBuffer();
            while ((str = bufferedReader.readLine()) != null) {
                content.append(str);
            }
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static List<AbstractNode> getNodesFromString(String content) {

        JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

        ArrayList<AbstractNode> nodes = new ArrayList<AbstractNode>();

        JsonArray fulltrace = obj.get("fulltrace").getAsJsonArray();
        Map<String, String> sharedVariableMap = getSharedVariableMap(content);

        for (JsonElement o :fulltrace) {
            JsonObject temp = o.getAsJsonObject();

            try {
                AbstractNode node = getNodeFromJsonObject(temp, sharedVariableMap);
                nodes.add(node);
            } catch (Exception e) {
                System.out.println("error:" + o);
                e.printStackTrace();
                System.exit(2);
            }
        }
        return nodes;
    }

    public static List<String> getSharedVariables(String content) {
        JsonObject obj = new JsonParser().parse(content).getAsJsonObject();
        JsonArray variables = obj.get("sharedAddresses").getAsJsonArray();
        Map<String, String> sharedVariableMap = getSharedVariableMap(content);

        List<String> result = new ArrayList<String>();
        for (JsonElement element: variables) {
            String variable = element.getAsString();
            variable = variable.substring(variable.indexOf(".") + 1);
            variable = sharedVariableMap.get(variable);

            result.add(variable);
        }

        return result;
    }

    private static AbstractNode getNodeFromJsonObject(JsonObject object, Map<String, String> sharedVariableMap ) {

        String type = object.get("type").getAsString();
        long GID = object.get("GID").getAsLong();
        int ID = object.get("ID").getAsInt();
        String label = object.get("label").getAsString();
        long tid = object.get("tid").getAsLong();
        String addr = object.get("addr") == null ? null : object.get("addr").getAsString();
        if(addr != null) {
            int index = addr.indexOf(".");
            if(index > 0 && index < addr.length() - 1) {
                addr = addr.substring(addr.indexOf(".") + 1);
                addr = sharedVariableMap.get(addr);
            }
        }

        switch (type) {
            case "READ":
            case "WRITE":
                String value = object.get("value").getAsString();
                long prevSyncId = object.get("prevSyncId") == null ? 0 : object.get("prevSyncId").getAsLong() ;
                long prevBranchId = object.get("prevBranchId")== null ? 0 : object.get("prevBranchId").getAsLong();
                AbstractNode node;

                if (type.equals("READ")) {
                    node = new ReadNode(GID, tid, ID, addr, value, AbstractNode.TYPE.READ, label);
                    ((ReadNode)node).setPrevBranchId(prevBranchId);
                    ((ReadNode)node).setPrevSyncId(prevSyncId);
                } else {
                    node = new WriteNode(GID, tid, ID, addr, value, AbstractNode.TYPE.WRITE, label);
                    ((WriteNode)node).setPrevBranchId(prevBranchId);
                    ((WriteNode)node).setPrevSyncId(prevSyncId);
                }
                return node;
            case "LOCK":
            case "UNLOCK":
                long did = object.get("did").getAsLong();
                String lock_addr = object.get("lock_addr").getAsString();
                if (type.equals("LOCK")) {
                    node = new LockNode(GID, tid, ID, lock_addr, AbstractNode.TYPE.LOCK);
                    ((LockNode)node).setDid(did);
                } else {
                    node = new UnlockNode(GID, tid, ID, lock_addr, AbstractNode.TYPE.UNLOCK);
                    ((UnlockNode)node).setDid(did);
                }
                return node;
            case "START":
                String tid_child = object.get("tid_child").getAsString();
                return new StartNode(GID, tid, ID, tid_child, AbstractNode.TYPE.START);
            case "JOIN":
                String tid_join = object.get("tid_join").getAsString();
                return new JoinNode(GID, tid, ID, tid_join, AbstractNode.TYPE.JOIN);
            case "INIT":
                value = object.get("value").getAsString();
                return new InitNode(GID, tid, ID, addr, value, AbstractNode.TYPE.INIT);
            case "WATI":
                String sig_addr = object.get("sig_addr").getAsString();
                return new WaitNode(GID, tid, ID, sig_addr, AbstractNode.TYPE.WAIT);
            case "NOTIFY":
                sig_addr = object.get("sig_addr").getAsString();
                long waitTid = object.get("waitTid").getAsLong();
                return new NotifyNode(GID, tid, ID, sig_addr, waitTid, AbstractNode.TYPE.NOTIFY);
            case "BRANCH":
                return new BranchNode(GID, tid, ID, AbstractNode.TYPE.BRANCH);
            case "BB":
                return new BBNode(GID, tid, ID, AbstractNode.TYPE.BB);
            case "PROPERTY":
                String object_addr = object.get("object_addr").getAsString();
                return new PropertyNode(GID, tid, ID, object_addr, AbstractNode.TYPE.PROPERTY);
            default:
                throw new IllegalStateException();
        }
    }

    public static Map<String, String> getSharedVariableMap(String content) {
        JsonObject object = new JsonParser().parse(content).getAsJsonObject();

        JsonObject info = object.get("info").getAsJsonObject();

        JsonObject sharedVariableMap = info.get("sharedVarIdSigMap").getAsJsonObject();

        Map<String, String> result = new HashMap<>();

        Gson objGson = new GsonBuilder().setPrettyPrinting().create();
        result = (Map<String, String>) objGson.fromJson(sharedVariableMap.toString(), result.getClass());

        return result;
    }
}
