package ru.telproject.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtils {
    public static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties(){
        try(InputStream inputStream = PropertiesUtils.class.getClassLoader().getResourceAsStream("rabbitmq.properties")) {
            PROPERTIES.load(inputStream);
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }
    }

    public static String get(String key){
        return PROPERTIES.getProperty(key);
    }
}
