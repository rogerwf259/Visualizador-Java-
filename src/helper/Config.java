package helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    public static Properties properties = new Properties();

    public void saveProperty(String title, String value) {

        try {
            File file = new File("config.ini");
            if (!file.exists()) {
                file.createNewFile();
            }
            properties.put(title, value);
            properties.store(new FileOutputStream("config.ini"), null);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String readProperties(String title) {
        String value="";
        try {
            properties.load(new FileInputStream("config.ini"));
            value = properties.getProperty(title);
        }catch (IOException e){
            e.printStackTrace();
        }
        return value;
    }
}

