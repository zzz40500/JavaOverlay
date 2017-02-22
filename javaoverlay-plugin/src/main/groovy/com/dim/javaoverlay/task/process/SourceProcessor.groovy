package com.dim.javaoverlay.task.process;

import com.dim.javaoverlay.common.L
import com.dim.javaoverlay.common.TextFileUtils
import okio.BufferedSink
import org.gradle.api.Project;

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
    private File classMapFile;
    private Map<String, String> classMap = new HashMap<>();
    private Map<String, String> sourceMap = new HashMap<>();

    public SourceProcessor(final Project project) {
        classMapFile = new File(project.getBuildDir().getAbsolutePath() + "/intermediates/java-overlay/classMap");
        if (!classMapFile.getParentFile().exists()) {
            classMapFile.getParentFile().mkdirs();
        } else {
            getLocalClassMap();
        }
    }

    public void getLocalClassMap() {
        TextFileUtils.visit(classMapFile, new TextFileUtils.TextVisitor() {
            @Override
            void visit(final String line) {
                String[] info = line.split(":");
                if (info.length == 2) {
                    classMap.put(info[0], info[1]);
                }
            }
        });
    }

    public void process(String file) {
        String packageClass = getQualifiedName(file);
        String filePath = sourceMap.put(packageClass, file);
        if (filePath != null) {
            L.d(filePath + " (remove)");
        }
    }

    private String getQualifiedName(final String file) {
        String qualifiedName = classMap.get(file);
        if (qualifiedName == null) {

            TextFileUtils.visit(new File(file), new TextFileUtils.TextVisitor() {
                @Override
                void visit(final String line) {
                    Matcher matcher = sPattern.matcher(line);
                    if (matcher.find()) {
                        qualifiedName = matcher.group(1).replaceAll(" ", "") + "." + getClassName(file);
                        BufferedSink bufferedSink = Okio.buffer(Okio.appendingSink(classMapFile)).writeUtf8(file + ":" + qualifiedName + "\n");
                        bufferedSink.flush();
                        bufferedSink.close();
                    }
                }
            });
        }
        if (qualifiedName == null) {
            qualifiedName = file;
        }

        return qualifiedName;
    }

    private String getClassName(final String file) {
        return new File(file).getName().replace(".java", "");
    }

    public List<String> getOutput() {
        return new ArrayList<>(sourceMap.values());
    }

}
