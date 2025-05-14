package edu.sysu.pmglab.sdfa.command;

import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.viewer.CCFViewer;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.sdfa.toolkit.SDFViewerReader;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-23 23:58
 * @description
 */

@Parser(
        usage = "gui [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.GUIProgram"),
                @UsageItem(key = "About", value = "Display readable text SDF structure graphical interface.")
        }
)
public class GUIProgram extends ICommandProgram {
    @Option(names = "gui", type = FieldType.NULL)
    Object gui;

    @Option(names = "-f", type = FieldType.string, required = true)
    String file;

    @Option(names = "--page-size", type = FieldType.varInt32, defaultTo = "100")
    int pageSize = 100;

    @Option(names = "--compatible", type = FieldType.bool)
    boolean compatible;

    public static void main(String[] args) throws IOException {
        GUIProgram guiProgram = new GUIProgram();
        CommandOptions options = guiProgram.parse(args);
        new CCFViewer(new SDFViewerReader(guiProgram.file),guiProgram.pageSize, guiProgram.compatible);
    }
}
