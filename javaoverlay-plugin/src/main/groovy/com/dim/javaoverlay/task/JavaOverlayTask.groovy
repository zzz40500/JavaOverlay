package com.dim.javaoverlay.task

import com.android.build.gradle.tasks.factory.AndroidJavaCompile

import com.dim.javaoverlay.task.process.SourceProcessor;
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.collections.FileCollectionAdapter
import org.gradle.api.internal.file.collections.MinimalFileSet
import org.gradle.api.tasks.TaskAction

/**
 * Created by dim on 17/2/19.
 */

public class JavaOverlayTask extends DefaultTask {

    FileTree source;
    FileCollection classPath;
    AndroidJavaCompile androidJavaCompile;
    SourceProcessor sourceProcessor;

    @TaskAction
    def process() {
        source = androidJavaCompile.getSource();
        classPath = androidJavaCompile.getClasspath();
        processSource();
    }

    def processSource() {
        source.forEach({
            file ->
                sourceProcessor.process(file.toString());
        })
        List list = sourceProcessor.getOutput();
        if (list.size() > 0) {
            androidJavaCompile.setSource(list.get(0));
            for (int i = 1; i < list.size(); i++) {
                androidJavaCompile.source(list.get(i));
            }
        }

    }
}
