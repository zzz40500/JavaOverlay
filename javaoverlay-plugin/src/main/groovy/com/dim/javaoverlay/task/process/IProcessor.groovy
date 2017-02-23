package com.dim.javaoverlay.task.process;


/**
 * Created by dim on 17/2/19.
 */

public interface IProcessor {

    void process(String file);

    List<String> getOutput();
}
