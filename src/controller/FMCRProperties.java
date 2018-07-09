import java.io.IOException;
import java.util.Properties;

/**
 *
 * Provides an interface for getting properties from a file, and exposing them
 * internally.
 */
public class FMCRProperties extends Properties {

    /**
     *  Instrumentation related properties
     */
    public static final String INSTRUMENTATION_PACKAGES_IGNORE_PREFIXES_KEY = "mcr.instrumentation.packages.ignore.prefixes";
    public static final String INSTRUMENTATION_PACKAGES_IGNORE_KEY = "mcr.instrumentation.packages.ignore";
    public static final String INSTRUMENTATION_CLASSES_IGNORE_PREFIXES_KEY = "mcr.instrumentation.classes.ignore.prefixes";
    public static final String INSTRUMENTATION_CLASSES_IGNORE_KEY = "mcr.instrumentation.classes.ignore";
    public static final String INSTRUMENTATION_PACKAGES_ALLOW_PREFIXES_KEY = "mcr.instrumentation.packages.allow.prefixes";
    public static final String INSTRUMENTATION_PACKAGES_ALLOW_KEY = "mcr.instrumentation.packages.allow";
    public static final String INSTRUMENTATION_CLASSES_ALLOW_PREFIXES_KEY = "mcr.instrumentation.classes.allow.prefixes";
    public static final String INSTRUMENTATION_CLASSES_ALLOW_KEY = "mcr.instrumentation.classes.allow";

    /**
     *  Scheduling related properties
     */


    /**
     *  properties related property
     */
    public static final String PROPERTIES_KEY = "mcr.properties";
    public static final String DEFAULT_PROPERTIES = "/default.properties";

    private static FMCRProperties fmcrProperties;

    private FMCRProperties() {

        String propertiesFileLocation = System.getProperty(PROPERTIES_KEY);

        try {

            //load default properties first
            if(this.getClass().getResourceAsStream(DEFAULT_PROPERTIES) == null){

                System.err.println("NO " + DEFAULT_PROPERTIES + " found");
            }
            load(this.getClass().getResourceAsStream(DEFAULT_PROPERTIES));
            //load user provide properties
            if(propertiesFileLocation != null){

                load(this.getClass().getResourceAsStream(PROPERTIES_KEY));
            }
        } catch (IOException e) {

            System.err.println("Unable to load the properties " + PROPERTIES_KEY);
            e.printStackTrace();
        }

    }

    public static FMCRProperties getFmcrProperties() {

        if(fmcrProperties == null){

            fmcrProperties = new FMCRProperties();
        }
        return fmcrProperties;
    }

    @Override
    public String getProperty(String key) {

        //check system property first
        String property = System.getProperty(key);
        //then check mcr.property
        if(property == null){
            property = super.getProperty(key);
        }
        return property;
    }
}
