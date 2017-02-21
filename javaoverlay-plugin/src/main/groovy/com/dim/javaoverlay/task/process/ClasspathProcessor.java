package com.dim.javaoverlay.task.process;

import com.dim.javaoverlay.JavaOverlayExtension;
import com.dim.javaoverlay.common.L;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by dim on 17/2/19.
 */

public class ClasspathProcessor implements IProcessor {
    private List<JavaOverlayExtension.Exclude> ruleList;
    private List<String> output = new ArrayList<>();
    private Project project;
    private Map<String, String> map = new HashMap<>();

    public ClasspathProcessor(Project project, final List<JavaOverlayExtension.Exclude> list) {
        this.project = project;
        this.ruleList = list;
    }

    @Override
    public void process(final String file) {
        String filePath = file;
        for (JavaOverlayExtension.Exclude exclude : ruleList) {
            if (exclude.isMatch(file)) {
                filePath = processJar(exclude, file);
            }
        }
        output.add(filePath);
    }

    private String processJar(final JavaOverlayExtension.Exclude exclude, final String file) {
        File classJar = new File(project.getBuildDir().getAbsolutePath() + "/intermediates/java-overlay/" + exclude.group + exclude.jarName + ".jar");
            if (!classJar.getParentFile().exists()) {
            classJar.getParentFile().mkdirs();
        }
        try {
            filterCopy(new File(file), classJar, exclude);
            map.put(file, classJar.getAbsolutePath());
            return classJar.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public String map(String file) {
        return map.get(file);
    }

    static void filterCopy(File zipFile, File targetDirectory, final JavaOverlayExtension.Exclude exclude) throws IOException {
        CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(targetDirectory), new CRC32());
        ZipOutputStream zos = new ZipOutputStream(cos);
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory())
                    continue;
                boolean excludeFile = exclude.isMatchClass(ze.getName());
                if (!excludeFile) {
                    zos.putNextEntry(ze);
                } else {
                    L.d(ze.getName() + " (remove) ");
                }
                while ((count = zis.read(buffer)) != -1) {
                    if (!excludeFile) {
                        zos.write(buffer, 0, count);
                    }
                }
                zis.closeEntry();
            }
        } finally {
            zis.close();
            zos.close();
        }
    }

    @Override
    public List<String> getOutput() {
        return output;
    }
}
