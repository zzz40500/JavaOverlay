package com.dim.javaoverlay

import com.android.build.gradle.tasks.factory.AndroidJavaCompile
import com.dim.javaoverlay.common.L
import com.dim.javaoverlay.common.TaskUtils
import com.dim.javaoverlay.task.JavaOverlayTask
import com.dim.javaoverlay.task.JavaOverlayTransform

import com.dim.javaoverlay.task.process.SourceProcessor;
import org.gradle.api.Plugin;
import org.gradle.api.Project

/**
 * Created by dim on 17/1/24.
 */

public class JavaOverlayPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        L.verbose = true;
        Map<String, JavaOverlayExtension> map = new HashMap<>();
        project.android.sourceSets.all { sourceSet ->
            sourceSet.extensions.create("javaOverlays", JavaOverlayExtension);
            map.put(sourceSet.name.toLowerCase(), sourceSet.javaOverlays);
        }
        project.android.registerTransform(new JavaOverlayTransform());

        project.afterEvaluate({
            project.android.applicationVariants.all { variant ->
                def variantName = variant.name.capitalize();
                def flavorName = variant.getFlavorName();
                def buildType = variant.getBuildType().getName();
                List<JavaOverlayExtension.Exclude> list = new ArrayList<>();
                if (flavorName != null) {
                    list.addAll(map.get((flavorName + buildType).toLowerCase()).ruleList);
                    list.addAll((map.get((flavorName).toLowerCase())).ruleList);
                    list.addAll((map.get((buildType).toLowerCase())).ruleList);
                } else {
                    list.addAll((map.get((buildType).toLowerCase())).ruleList);
                }
                list.addAll(((JavaOverlayExtension) map.get("main")).ruleList);
                JavaOverlayTask javaOverlayTask = project.tasks.create("javaOverlay${variantName}", JavaOverlayTask);
                AndroidJavaCompile androidJavaCompile = variant.variantData.javacTask;
                javaOverlayTask.androidJavaCompile = androidJavaCompile;
                javaOverlayTask.sourceProcessor = new SourceProcessor(project);
                TaskUtils.injectBefore(androidJavaCompile, javaOverlayTask);
                def transformClassesWithJavaOverlayFor = project.tasks.findByName("transformClassesWithJavaOverlayFor${variant.name.capitalize()}");
                JavaOverlayTransform javaOverlayTransform = transformClassesWithJavaOverlayFor.transform;
                javaOverlayTransform.ruleMap.put(transformClassesWithJavaOverlayFor.getPath(), list);
            }
        });

    }

}
