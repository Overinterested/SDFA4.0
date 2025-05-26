package edu.sysu.pmglab.sdfa.command;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.StringSplitter;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.r.RConnectionPool;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;
import edu.sysu.pmglab.sdfa.nagf.analyze.gene.MultiThreadAdaptThresholdRegression;
import org.rosuda.REngine.REngineException;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-05-24 07:51
 * @description
 */
@Parser(
        usage = "pstr [options]",
        usage_item = {
                @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.command.PanAnnotationProgram"),
                @UsageItem(key = "About", value = "Use pan-annotation step threshold regression to solve multiple SV samples.")
        }
)
public class PanAnnotationProgram extends ICommandProgram {
    @Option(names = "pstr", type = FieldType.NULL)
    Object pstr;

    @Option(names = {"--input-file", "-f"}, type = FieldType.file, required = true)
    File inputFile;

    @Option(names = {"--output-file", "-o"}, type = FieldType.file, required = true)
    File outputFile;

    @Option(names = {"--port-list", "-pl"}, type = FieldType.string, required = true)
    String portList;

    @Option(names = {"--ped-file"}, type = FieldType.file, required = true)
    File pedFile;

    @Option(names = {"--rna-level"}, type = FieldType.NULL)
    Object rnaLevel;

    public static void main(String[] args) throws IOException, REngineException, InterruptedException {
        Logger logger = LogBackOptions.getRootLogger();
        PanAnnotationProgram program = new PanAnnotationProgram();
        CommandOptions options = program.parse(args.length == 1 && args[0].equals("pstr") ? new String[]{"--help"} : args);
        if (options.isHelp()) {
            logger.info("\n{}", options.usage());
            return;
        } else {
            logger.info("\n{}", options);
        }

        // init
        File numericRes = options.value("-f");
        File rawPedFile = options.value("--ped-file");
        File pstrRes = options.value("-o");
        String ports = options.value("-pl");
        StringSplitter splitter = new StringSplitter(',');
        splitter.init(ports);
        IntList portList = new IntList();
        while (splitter.hasNext()) {
            String next = splitter.next().trim();
            portList.add(Integer.parseInt(next));
        }
        // obtain PED file
        File modifiedPedFile = FileUtils.getSubFile(pstrRes.getParentFile(), "true_fam.ped");
        File pedFile = PEDFile.PEDEasyProducer.produceWithNGAAHeaderAndPEDFile(
                numericRes.toString(),
                rawPedFile.toString(),
                modifiedPedFile.toString()
        );
        // add R serves
        for (int i = 0; i < portList.size(); i++) {
            RConnectionPool.addConnectionWithLibraries("localhost", portList.fastGet(i), "chngpt");
        }
        MultiThreadAdaptThresholdRegression.getInstance().setZeroAFFilter(0.9f).setThread(portList.size());
        MultiThreadAdaptThresholdRegression.analyze(
                numericRes.toString(),
                pstrRes.getPath(),
                pedFile.toString(),
                options.passed("--rna-level") ? "rna" : "gene"
        );
    }
}
