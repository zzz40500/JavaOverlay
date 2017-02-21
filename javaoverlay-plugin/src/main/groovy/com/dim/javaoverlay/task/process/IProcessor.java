package com.dim.javaoverlay.task.process;

import java.util.List;

/**
 * Created by dim on 17/2/19.
 */

public interface IProcessor {

    void process(String file);

    List<String> getOutput();
}
