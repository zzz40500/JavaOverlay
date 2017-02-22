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
import com.dim.javaoverlay.JavaOverlayExtension;
import com.dim.javaoverlay.common.L;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES;

/**
 * Created by dim on 17/2/20.
 */

public class JavaOverlayTransform extends Transform {


    public Map<String, List<JavaOverlayExtension.Exclude>> ruleMap = new HashMap<>();

    @Override
    public String getName() {
        return "JavaOverlay";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.<QualifiedContent.ContentType>of(CLASSES);
    }

    public Set<QualifiedContent.Scope> getScopes() {
        return Sets.immutableEnumSet(
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES);
    }

    @Override
    public Map<String, Object> getParameterInputs() {
        HashMap<String, Object> map = new HashMap<>();
        for (String key : ruleMap.keySet()) {
            map.put(key, Joiner.on("-").join(ruleMap.get(key)));
        }
        return map;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(final TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        List<JavaOverlayExtension.Exclude> excludes = ruleMap.get(transformInvocation.getContext().getPath());
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
                File targetPath = transformInvocation.getOutputProvider().getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                incrementalHandleFile(excludes, jarInput.getFile(), jarInput.getStatus(), targetPath);
            }
            for (DirectoryInput directoryInput : directoryInputs) {
                File dir = transformInvocation.getOutputProvider().getContentLocation("class", directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                for (File file : directoryInput.getChangedFiles().keySet()) {
                    Status status = directoryInput.getChangedFiles().get(file);
                    String targetPath = file.getAbsolutePath().replaceFirst(directoryInput.getFile().getAbsolutePath(), dir.getAbsolutePath());
                    incrementalHandleFile(excludes, file, status, new File(targetPath));
                }
            }
        } else {
            transformInvocation.getOutputProvider().deleteAll();
            for (JarInput jarInput : jarInputs) {
                File contentLocation = transformInvocation.getOutputProvider().getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                copy(jarInput.getFile(), contentLocation, excludes);
            }

            for (DirectoryInput directoryInput : directoryInputs) {
                File contentLocation = transformInvocation.getOutputProvider().getContentLocation("class", directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                FileUtils.copyDirectory(directoryInput.getFile(), contentLocation);
            }
        }
    }


    private void incrementalHandleFile(final List<JavaOverlayExtension.Exclude> excludes, File file, Status status, File targetFile) {
        try {
            switch (status) {
                case ADDED:
                    copy(file, targetFile, excludes);
                    break;
                case REMOVED:
                    FileUtils.forceDelete(targetFile);
                    break;
                case CHANGED:
                    FileUtils.forceDelete(targetFile);
                    copy(file, targetFile, excludes);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copy(File srcFile, File destFile, List<JavaOverlayExtension.Exclude> excludes) {
        for (JavaOverlayExtension.Exclude exclude : excludes) {
            if (exclude.isMatch(srcFile.getAbsolutePath())) {
                processJar(srcFile, destFile, exclude);
            }
        }
    }

    private void processJar(final File srcFile, final File destFile, final JavaOverlayExtension.Exclude exclude) {
        try {
            if (!destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }
            filterCopy(srcFile, destFile, exclude);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void filterCopy(File zipFile, File destFile, final JavaOverlayExtension.Exclude exclude) throws IOException {
        CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(destFile), new CRC32());
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
}
