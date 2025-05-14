package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.EntryOption;
import edu.sysu.pmglab.commandParser.annotation.rule.Counter;
import edu.sysu.pmglab.commandParser.annotation.rule.Rule;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.sdfa.SDFFilter;
import edu.sysu.pmglab.sdfa.nagf.NAGFProgram;
import edu.sysu.pmglab.sdfa.toolkit.SDFConcat;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-23 01:52
 * @description
 */
@Parser(
        usage = "<entry> [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.SDFAProgram"),
                @UsageItem(key = "About", value = "Entry pointer for the SDFA program.")
        },
        rule = @Rule(
                counter = {
                        @Counter(item = {
                                "annotate", "merge", "nagf", "vcf2sdf", "integrate", "gui",
                                "concat", "filter", "extract"
                        }, rule = Counter.Type.EQUAL, count = 1)
                }
        )
)
public class SDFAProgram extends ICommandProgram {
    @EntryOption({"annotate"})
    String[] annotate;

    @EntryOption({"merge"})
    String[] merge;

    @EntryOption({"nagf"})
    String[] nagf;

    @EntryOption({"vcf2sdf"})
    String[] vcf2sdf;

    @EntryOption({"gwas"})
    String[] gwas;

    @EntryOption({"integrate"})
    String[] integrate;

    @EntryOption({"gui"})
    String[] gui;

    @EntryOption({"concat"})
    String[] concat;

    @EntryOption({"filter"})
    String[] filter;

    @EntryOption({"extract"})
    String[] extract;

    public static void main(String[] args) throws IOException {
        SDFAProgram program = new SDFAProgram();
        CommandOptions options = program.parse(args);

        LogBackOptions.init();
        Logger logger = LogBackOptions.getRootLogger();

        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
        } else if (options.passed("annotate")) {
            AnnotationProgram.main(options.value("annotate"));
        } else if (options.passed("merge")) {
            MergeProgram.main(options.value("merge"));
        } else if (options.passed("nagf")) {
            NAGFProgram.main(options.value("nagf"));
        } else if (options.passed("vcf2sdf")) {
            VCF2SDFProgram.main(options.value("vcf2sdf"));
        } else if (options.passed("gwas")) {
            SVBasedGWASProgram.main(options.value("gwas"));
        } else if (options.passed("integrate")) {
            IntegrateProgram.main(options.value("integrate"));
        } else if (options.passed("gui")) {
            GUIProgram.main(options.value("gui"));
        } else if (options.passed("concat")) {
            SDFConcatProgram.main(options.value("concat"));
        } else if (options.passed("filter")){
            SDFFilterProgram.main(options.value("filter"));
        } else if(options.passed("extract")){
            SDFExtractProgram.main(options.value("extract"));
        }
    }
}
