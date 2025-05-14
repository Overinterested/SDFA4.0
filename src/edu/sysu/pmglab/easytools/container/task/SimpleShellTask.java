package edu.sysu.pmglab.easytools.container.task;

import java.io.File;

/**
 * @author Wenjie Peng
 * @create 2024-10-25 01:51
 * @description
 */
public class SimpleShellTask {
    String name;
    File workspace;
    String commandLines;

    public String toCommand() {
        String prefix;
        prefix = "cd " + workspace.getPath() + ";";
        prefix += "file=\"" + name + "\";";
        return prefix + commandLines;
    }

    public SimpleShellTask(File workspace, String commandLines) {
        this.workspace = workspace;
        this.commandLines = commandLines;
    }

    public SimpleShellTask setName(String name) {
        this.name = name;
        return this;
    }


}
