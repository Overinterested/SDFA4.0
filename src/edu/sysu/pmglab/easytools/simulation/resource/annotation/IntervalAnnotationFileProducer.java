package edu.sysu.pmglab.easytools.simulation.resource.annotation;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.sv.SVContig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

/**
 * @author Wenjie Peng
 * @create 2025-05-08 06:32
 * @description
 */
public class IntervalAnnotationFileProducer {
    int row;
    int seed;
    int minPos;
    int minLen;
    int maxLen;
    int maxEnd;
    File outputFile;
    SVContig contig;


    public void produce() throws IOException {
        Random random = new Random(seed);
        ByteStream cache = new ByteStream();
        int sizeOfContigs = contig.support().size();
        IntList sizeOfLineInChromosome = IntList.wrap(new int[sizeOfContigs]);
        sizeOfLineInChromosome.fill(row / sizeOfContigs, sizeOfContigs);
        sizeOfLineInChromosome.fastSet(0, sizeOfLineInChromosome.fastGet(0) + row - (int) sizeOfLineInChromosome.sum());
        WriterStream writerStream = new WriterStream(outputFile, WriterStream.Option.DEFAULT);

        for (int i = 0; i < sizeOfContigs; i++) {
            String contigNameByIndex = contig.getContigNameByIndex(i);
            int writeLines = sizeOfLineInChromosome.fastGet(i);
            int lastPos = minPos;
            int boundary = maxEnd-minPos;
            for (int j = 0; j < writeLines; j++) {
                cache.write(ASCIIUtility.toASCII(contigNameByIndex, Constant.CHAR_SET));
                cache.write(Constant.TAB);
                cache.write(ASCIIUtility.toASCII((lastPos=lastPos+random.nextInt(boundary))));
                cache.write(Constant.TAB);
            }
        }
    }

    private IntervalAnnotationFileProducer(File outputFile) {
        this.outputFile = outputFile;
    }

    public static class IntervalAnnotationFileBuilder {
        File output;
        int row = 100;
        int seed = 100;
        int minLen = 50;
        int minPos = 2000;
        int maxLen = 1000;
        int maxEnd = 200000000;
        SVContig contig = SVContig.init();

        private IntervalAnnotationFileBuilder(File output) {
            this.output = output;
        }

        public static IntervalAnnotationFileBuilder of(String outputFile) {
            return new IntervalAnnotationFileBuilder(new File(outputFile));
        }

        public IntervalAnnotationFileBuilder setRow(int row) {
            this.row = row;
            return this;
        }

        public IntervalAnnotationFileBuilder setMinPos(int minPos) {
            this.minPos = minPos;
            return this;
        }

        public IntervalAnnotationFileBuilder setMaxEnd(int maxEnd) {
            this.maxEnd = maxEnd;
            return this;
        }

        public IntervalAnnotationFileBuilder setMaxLen(int maxLen) {
            this.maxLen = maxLen;
            return this;
        }

        public void clearContig() {
            contig.clear();
        }

        public IntervalAnnotationFileBuilder addChromosomes(String... chromosomes) {
            for (String chromosome : chromosomes) {
                contig.addContigName(chromosome);
            }
            return this;
        }

        public IntervalAnnotationFileBuilder setSeed(int seed) {
            this.seed = seed;
            return this;
        }

        public IntervalAnnotationFileProducer build() {
            return new IntervalAnnotationFileProducer(output)
                    .setRow(row)
                    .setSeed(seed)
                    .setContig(contig)
                    .setMinLen(minLen)
                    .setMaxLen(maxLen)
                    .setMinPos(minPos)
                    .setMaxEnd(maxEnd);
        }
    }

    private IntervalAnnotationFileProducer setRow(int row) {
        this.row = row;
        return this;
    }

    private IntervalAnnotationFileProducer setSeed(int seed) {
        this.seed = seed;
        return this;
    }

    private IntervalAnnotationFileProducer setMinPos(int minPos) {
        this.minPos = minPos;
        return this;
    }

    private IntervalAnnotationFileProducer setMaxEnd(int maxEnd) {
        this.maxEnd = maxEnd;
        return this;
    }

    private IntervalAnnotationFileProducer setMaxLen(int maxLen) {
        this.maxLen = maxLen;
        return this;
    }

    private IntervalAnnotationFileProducer setContig(SVContig contig) {
        this.contig = contig;
        return this;
    }

    public IntervalAnnotationFileProducer setMinLen(int minLen) {
        this.minLen = minLen;
        return this;
    }
}
