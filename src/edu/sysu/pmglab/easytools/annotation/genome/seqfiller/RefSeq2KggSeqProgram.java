package edu.sysu.pmglab.easytools.annotation.genome.seqfiller;

import edu.sysu.pmglab.easytools.annotation.genome.KggFileSeqFiller;
import edu.sysu.pmglab.easytools.annotation.genome.prefix.RefSeqGTFParser;
import edu.sysu.pmglab.gtb.genome.coordinate.Chromosome;
import edu.sysu.pmglab.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author Wenjie Peng
 * @create 2024-11-07 00:23
 * @description
 */
public class RefSeq2KggSeqProgram {
    protected File outputDir;
    protected File inputGTFFile;
    protected File inputFNAFile;
    protected static HashSet<Chromosome> storedContigName;

    public RefSeq2KggSeqProgram() {
        // add MT by default
        Chromosome.get("chrMT").addAlias("NC_012920.1", "MT", "chrM");
        this.storedContigName = new HashSet<>(Chromosome.values());
    }

    public void submit() throws IOException {
        check();
        String tmpKggFile = FileUtils.getSubFile(outputDir.toString(), UUID.randomUUID() + ".txt");
        RefSeqGTFParser refSeqGTFParser = new RefSeqGTFParser()
                .setGtfFile(inputGTFFile)
                .setStoredContigName(storedContigName)
                .setOutputKggFile(tmpKggFile);
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


    public RefSeq2KggSeqProgram setOutputDir(Object outputDir) {
        this.outputDir = new File(outputDir.toString());
        return this;
    }

    public RefSeq2KggSeqProgram setInputGTFFile(Object inputGTFFile) {
        this.inputGTFFile = new File(inputGTFFile.toString());
        return this;
    }

    public RefSeq2KggSeqProgram setInputFNAFile(Object inputFNAFile) {
        this.inputFNAFile = new File(inputFNAFile.toString());
        return this;
    }

    protected void check() {
        if (inputGTFFile == null) {
            throw new UnsupportedOperationException("No gtf file is passed.");
        }
        if (outputDir == null) {
            throw new UnsupportedOperationException("Output directory file is not assigned.");
        }
    }

    public static void addStoredContig(String... contigNames) {
        for (String contigName : contigNames) {
            Chromosome.get(contigName);
        }

        storedContigName.addAll(Chromosome.values());
    }

    public static void clearAllStoredContigs() {
        storedContigName.clear();
    }

    public static void main(String[] args) throws IOException {
        new RefSeq2KggSeqProgram().setInputGTFFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/refGene/GRCh38_latest_genomic.gtf.gz")
                .setOutputDir("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome")
                .setInputFNAFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/refGene/GRCh38_latest_genomic.fna.gz")
                .submit();
    }
}
