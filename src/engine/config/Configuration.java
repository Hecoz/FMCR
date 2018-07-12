package engine.config;

public class Configuration {

    public static String class_name = null;

    public static String mode = "SC";           //default: SC
    public static boolean DEBUG = false;        //use or not use debug mode
    public static boolean Optimize = false;     //use or not use optimization by SDG

    public static void setup(){

        class_name = System.getProperty("class_name"); //返回为 null

        //this is for the constraints reduction using static dependency analysis
        /*
        if (Optimize){

            SDG = ReadSDG.readSDG();
            reachSDG = ReadSDG.ConstructReachability(SDG);
            mapNodeLabelToId = ReadSDG.NodeToId();
        }
        */
        //memory model
        String memory_model = System.getProperty("memory_model");
        if (memory_model != null && !memory_model.isEmpty()) {
            Configuration.mode=memory_model;
        }
    }
}
