package com.dim.javaoverlay.common

import org.gradle.api.DefaultTask;

/**
 * Created by dim on 17/2/20.
 */

public class TaskUtils {

    public static void injectBefore(DefaultTask task, DefaultTask injectTask) {
        def dependencies = task.getTaskDependencies().getDependencies(task);
        for (def dependence : dependencies) {
            if (dependence != injectTask)
                injectTask.dependsOn dependence;
        }
        task.dependsOn injectTask;
    }
}
