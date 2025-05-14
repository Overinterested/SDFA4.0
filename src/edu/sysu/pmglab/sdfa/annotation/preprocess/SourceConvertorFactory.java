package edu.sysu.pmglab.sdfa.annotation.preprocess;

import edu.sysu.pmglab.io.file.LiveFile;

import java.io.File;

/**
 * @author Wenjie Peng
 * @create 2024-09-08 07:14
 * @description
 */
public class SourceConvertorFactory {
    private static final String GTF_REGULAR_EXPRESSION = ".*.gtf.*";

    public static SourceConvertor createSourceConvertor(String sourceType, LiveFile file, File outputDir) {
        switch (sourceType.toLowerCase()) {
            case "interval":
                return IntervalSourceConvertor.of(file, outputDir);
            case "sv":
                return SVSourceConvertor.of(file, outputDir);
            case "gene":
                String fileName = file.getName().toLowerCase();
                if (fileName.contains("kggseq")) {
                    return GenomeSourceConvertor.KGGGenomeSourceConvertor.of(file, outputDir);
                }
                if (fileName.matches(GTF_REGULAR_EXPRESSION)) {
                    return GenomeSourceConvertor.KGGGenomeSourceConvertor.of(file, outputDir).isGTF();
                }
                throw new UnsupportedOperationException("Unknown genome frame which is not listed in {GTF, KGGSeq}");
            default:
                throw new UnsupportedOperationException("Unknown file type which is not listed in {gene, interval, sv}");
        }
    }

}
