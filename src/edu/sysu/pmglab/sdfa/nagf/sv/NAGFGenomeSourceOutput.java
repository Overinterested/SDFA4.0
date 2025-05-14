package edu.sysu.pmglab.sdfa.nagf.sv;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.easytools.container.circularqueue.ReusableCircularQueue;
import edu.sysu.pmglab.sdfa.annotation.output.GenomeSourceOutput;
import edu.sysu.pmglab.sdfa.annotation.source.GenomeSource;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.AffectedNumericConvertor;
import edu.sysu.pmglab.sdfa.nagf.numeric.process.conversion.AffectedStringConvertor;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-11-15 06:28
 * @description
 */
public class NAGFGenomeSourceOutput extends GenomeSourceOutput {
    static boolean geneLevel;
    private static Bytes RNA_LEVEL_HEADER = new Bytes("RNA_NAGF_Values");
    private static Bytes GENE_LEVEL_HEADER = new Bytes("Gene_NAGF_Values");

    public static NAGFGenomeSourceOutput of(GenomeSource source) throws IOException {
        AffectedNumericConvertor.add("full", new AffectedStringConvertor());
        NAGFGenomeSourceOutput sourceOutput = new NAGFGenomeSourceOutput();
        sourceOutput.setFile(source.getFile());
        sourceOutput.setMeta(source.getSourceMeta());
        sourceOutput.setReusableCircularQueue(new ReusableCircularQueue<SourceRNARecord>() {
            @Override
            protected void reuseElement(SourceRNARecord element) {
                element.clear();
            }
        });
        sourceOutput.setReader(new CCFReader(sourceOutput.file));
        sourceOutput.setFrame(new NAGFGenomeOutputFrame(source.getSourceMeta()));
        sourceOutput.reader.close();
        return sourceOutput;
    }

    @Override
    public boolean writeAnnotation(ISDSV sdsv, ByteStream cache, int startPointer, int endPointer) {
        return super.writeAnnotation(sdsv, cache, startPointer, endPointer);
    }

    @Override
    public Bytes getHeader() {
        return geneLevel ? GENE_LEVEL_HEADER : RNA_LEVEL_HEADER;
    }

    public static void geneLevel(boolean geneLevel) {
        NAGFGenomeOutputFrame.geneLevel = geneLevel;
        NAGFGenomeSourceOutput.geneLevel = geneLevel;
    }

    public GenomeSourceOutput setFrame(NAGFGenomeOutputFrame frame) {
        this.frame = frame;
        return this;
    }
}
