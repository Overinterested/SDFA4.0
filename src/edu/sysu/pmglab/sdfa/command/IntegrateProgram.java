package edu.sysu.pmglab.sdfa.command;

import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;

/**
 * @author Wenjie Peng
 * @create 2024-10-23 23:57
 * @description
 */
@Parser(
        usage = "gui [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.IntegrateProgram"),
                @UsageItem(key = "About", value = "Integrate SV results from a single sample across multiple callers to identify SVs that recur across tools, which potentially indicates high-confidence SVs.")
        }
)
public class IntegrateProgram extends ICommandProgram {
    public static void main(String[] args) {

    }
}
