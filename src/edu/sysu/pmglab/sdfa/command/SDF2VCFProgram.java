package edu.sysu.pmglab.sdfa.command;

import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;

/**
 * @author Wenjie Peng
 * @create 2024-10-30 01:46
 * @description
 */
@Parser(
        usage = "sdf2vcf [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.SDFConcatProgram"),
                @UsageItem(key = "About", value = "Concat multiple SDF files.")
        }
)
public class SDF2VCFProgram extends ICommandProgram {
    @Option(names = "sdf2vcf", type = FieldType.NULL)
    Object gui;

    @Option(names = "-f", type = FieldType.string, required = true)
    String inputFile;

    @Option(names = "-o", type = FieldType.string, required = true)
    String outputFile;

    public static void main(String[] args) {

    }
}
