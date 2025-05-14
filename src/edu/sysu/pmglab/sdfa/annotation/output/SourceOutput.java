package edu.sysu.pmglab.sdfa.annotation.output;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.sdfa.annotation.source.GenomeSource;
import edu.sysu.pmglab.sdfa.annotation.source.Source;
import edu.sysu.pmglab.sdfa.nagf.sv.NAGFGenomeSourceOutput;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-09 01:14
 * @description
 */
public interface SourceOutput {
    void mapPointer() throws IOException;

    int numOfNeededRecords();

    boolean writeAnnotation(ISDSV sdsv, ByteStream cache, int startPointer, int endPointer);

    void expand(int startPointer, int endPointer);

    boolean accept(IntInterval pointerPair) throws IOException;

    static SourceOutput of(Source source) throws IOException {
        if (source instanceof GenomeSource.NAGFGenomeSource){
            return NAGFGenomeSourceOutput.of((GenomeSource) source);
        } else if (source instanceof GenomeSource) {
            return GenomeSourceOutput.of((GenomeSource) source);
        } else {
            return IntervalSourceOutput.of(source);
        }
    }

    Bytes getEmptyAnnotationResult();

    Bytes getHeader();
}
