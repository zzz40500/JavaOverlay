package com.dim.javaoverlay.task.process;

import com.dim.javaoverlay.common.L;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okio.BufferedSource;
import okio.Okio;

/**
 * Created by dim on 17/2/19.
 */

public class SourceProcessor implements IProcessor {

    private static Pattern sPattern = Pattern.compile("package\\s+(.+);");

    public static void main(String[] args) {
        Matcher matcher = sPattern.matcher("package com.dim.javaoverlay;");
        if (matcher.find()) {
            System.out.println(matcher.group(1).replaceAll(" ", ""));
        }
    }

    private Map<String, String> sourceMap = new HashMap<>();

    public void process(String file) {
        String packageClass = getQualifiedName(file);
        String filePath = sourceMap.put(packageClass, file);
        if (filePath != null) {
            L.d(filePath + " (remove)");
        }
    }

    private String getQualifiedName(final String file) {
        BufferedSource buffer = null;
        try {
            buffer = Okio.buffer(Okio.source(new File(file)));
            String line;
            while ((line = buffer.readUtf8Line()) != null) {
                Matcher matcher = sPattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1).replaceAll(" ", "") + "." + getClassName(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (buffer != null) {
                try {
                    buffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    private String getClassName(final String file) {
        return new File(file).getName().replace(".java", "");
    }

    public List<String> getOutput() {
        return new ArrayList<>(sourceMap.values());
    }

}
