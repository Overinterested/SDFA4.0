package edu.sysu.pmglab.easytools.annotation.genome.seqfiller;

import edu.sysu.pmglab.easytools.annotation.genome.KggFileSeqFiller;
import edu.sysu.pmglab.easytools.annotation.genome.prefix.KnownGeneFileParser;
import edu.sysu.pmglab.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Wenjie Peng
 * @create 2024-11-11 00:07
 * @description
 */
public class KnownGene2KggSeqProgram extends RefSeq2KggSeqProgram {
    protected File extraGeneFile;
    protected File inputKnownGeneFile;

    @Override
    public void submit() throws IOException {
        check();
        String tmpKggFile = FileUtils.getSubFile(outputDir.toString(), UUID.randomUUID() + ".txt");
        KnownGeneFileParser refSeqGTFParser = new KnownGeneFileParser()
                .setKnownGeneFile(inputKnownGeneFile)
                .setExtraGeneFile(extraGeneFile)
                .setStoredContigName(storedContigName)
                .setOutputPath(tmpKggFile);
        refSeqGTFParser.submit();
        if (inputFNAFile != null) {
            String resFile = FileUtils.getSubFile(outputDir.toString(), "kggseq_version.txt");
            new KggFileSeqFiller().setKggFile(tmpKggFile)
                    .setSeqPath(inputFNAFile.toString())
                    .setResFile(resFile)
                    .submit();
            FileUtils.delete(tmpKggFile);
        } else {
            String resFile = FileUtils.getSubFile(outputDir.toString(), "kggseq_version_non_seq.txt");
            new File(tmpKggFile).renameTo(new File(resFile));
        }
    }

    @Override
    protected void check() {
        if (extraGeneFile == null) {
            throw new UnsupportedOperationException("KnownGene gene map file isn't assigned");
        }
        if (inputKnownGeneFile == null) {
            throw new UnsupportedOperationException("KnownGene file isn't assigned");
        }
        if (outputDir == null) {
            throw new UnsupportedOperationException("Output directory isn't assigned");
        }
        if (inputFNAFile == null) {
            throw new UnsupportedOperationException("Input sequence file(.fna) isn't assigned");
        }
    }

    public KnownGene2KggSeqProgram setInputKnownGeneFile(Object inputKnownGeneFile) {
        this.inputKnownGeneFile = new File(inputKnownGeneFile.toString());
        return this;
    }

    public KnownGene2KggSeqProgram setExtraGeneFile(Object extraGeneFile) {
        this.extraGeneFile = new File(extraGeneFile.toString());
        return this;
    }

    @Override
    public KnownGene2KggSeqProgram setOutputDir(Object outputDir) {
        super.setOutputDir(outputDir);
        return this;
    }

    @Override
    public KnownGene2KggSeqProgram setInputGTFFile(Object inputGTFFile) {
        super.setInputGTFFile(inputGTFFile);
        return this;
    }

    @Override
    public KnownGene2KggSeqProgram setInputFNAFile(Object inputFNAFile) {
        super.setInputFNAFile(inputFNAFile);
        return this;
    }

    public static void main(String[] args) throws IOException {
        new KnownGene2KggSeqProgram().setInputKnownGeneFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/knownGene/knownGene.txt")
                .setOutputDir("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome")
                .setInputFNAFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/refGene/GRCh38_latest_genomic.fna.gz")
                .setExtraGeneFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/knownGene/mart_export-3.txt")
                .submit();
    }
}
