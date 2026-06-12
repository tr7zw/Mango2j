package dev.tr7zw.mango2j.util;

import java.io.File;

public class FileUtil {

    public static void delete(File file) {
        if(file.isDirectory()) {
            for(File f : file.listFiles())
                delete(f);
        }
        file.delete();
    }
    
    /**
     * Remove invalid chars/trim string length. TODO better method
     * 
     * @param title
     * @return
     */
    public static String cleanName(String title) {
        title = title.replace("\n", "");
        title = title.replace('|', '-');
        title = title.replace(':', '-');
        title = title.replace('\'', '-');
        title = title.replace('/', '-');
        title = title.replace('\\', '-');
        title = title.replace('.', ',');
        title = title.replace('"', '\'');
        title = title.replace('"', '\'');
        title = title.replace('@', 'A');
        title = title.replace('?', '-');
        title = title.replace('*', '-');
        title = title.replace('!', '-');
        if (title.length() > 60)
            title = title.substring(0, 50);

        title = title.trim();
        return title;
    }
    
}
