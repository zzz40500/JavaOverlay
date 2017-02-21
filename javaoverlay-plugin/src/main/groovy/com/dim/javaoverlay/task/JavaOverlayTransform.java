package com.dim.javaoverlay.task;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.dim.javaoverlay.common.L;
import com.dim.javaoverlay.task.process.ClasspathProcessor;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES;
import static com.android.build.api.transform.Status.*;

/**
 * Created by dim on 17/2/20.
 */

public class JavaOverlayTransform extends Transform {


    public Map<String, ClasspathProcessor> mapClasspathProcessor = new HashMap<>();

    public void putClassPathProcessor(String path, ClasspathProcessor classpathProcessor) {
        mapClasspathProcessor.put(path, classpathProcessor);
    }

    @Override
    public String getName() {
        return "JavaOverlay";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.<QualifiedContent.ContentType>of(CLASSES);
    }

    public Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(final TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        ClasspathProcessor classpathProcessor = mapClasspathProcessor.get(transformInvocation.getContext().getPath());
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        List<JarInput> jarInputs = new ArrayList<>();
        List<DirectoryInput> directoryInputs = new ArrayList<>();
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                jarInputs.add(jarInput);
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                directoryInputs.add(directoryInput);
            }
        }

        if (transformInvocation.isIncremental()) {

            for (JarInput jarInput : jarInputs) {
                String map = classpathProcessor.map(jarInput.getFile().getAbsolutePath());
                File targetPath = transformInvocation.getOutputProvider().getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                if (map != null) {
                    targetPath = new File(map);
                }
                incrementalHandleFile(jarInput.getFile(), jarInput.getStatus(), targetPath);

            }

            for (DirectoryInput directoryInput : directoryInputs) {
                File dir = transformInvocation.getOutputProvider().getContentLocation("class", directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                for (File file : directoryInput.getChangedFiles().keySet()) {
                    Status status = directoryInput.getChangedFiles().get(file);
                    String targetPath = file.getAbsolutePath().replaceFirst(directoryInput.getFile().getAbsolutePath(), dir.getAbsolutePath());
                    incrementalHandleFile(file, status, new File(targetPath));
                }
            }

        } else {
            transformInvocation.getOutputProvider().deleteAll();

            for (JarInput jarInput : jarInputs) {
                String map = classpathProcessor.map(jarInput.getFile().getAbsolutePath());

                File contentLocation = transformInvocation.getOutputProvider().getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                if (map != null) {
                    FileUtils.copyFile(new File(map), contentLocation);
                } else {
                    FileUtils.copyFile(jarInput.getFile(), contentLocation);
                }
            }

            for (DirectoryInput directoryInput : directoryInputs) {
                File contentLocation = transformInvocation.getOutputProvider().getContentLocation("class", directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                FileUtils.copyDirectory(directoryInput.getFile(), contentLocation);
            }
        }
    }


    private void incrementalHandleFile(File file, Status status, File targetFile) {
        try {
            switch (status) {
                case ADDED:
                    FileUtils.copyFile(file, targetFile);
                    break;
                case REMOVED:
                    FileUtils.forceDelete(targetFile);
                    break;
                case CHANGED:
                    FileUtils.forceDelete(targetFile);
                    try {
                        FileUtils.copyFile(file, targetFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
