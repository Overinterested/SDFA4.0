package edu.sysu.pmglab.easytools.annotation.genome.seqfiller;

import edu.sysu.pmglab.easytools.annotation.genome.KggFileSeqFiller;
import edu.sysu.pmglab.easytools.annotation.genome.prefix.GEncodeGTFParser;
import edu.sysu.pmglab.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Wenjie Peng
 * @create 2024-11-10 23:28
 * @description
 */
public class GEncode2KggSeqProgram extends RefSeq2KggSeqProgram {
    public GEncode2KggSeqProgram(){
        super();
    }
    public void submit() throws IOException {
        check();
        String tmpKggFile = FileUtils.getSubFile(outputDir.toString(), UUID.randomUUID() +".txt");
        GEncodeGTFParser gEncodeGTFParser = new GEncodeGTFParser()
                .setGtfFile(inputGTFFile)
                .setStoredContigName(storedContigName)
                .setOutputKggFile(tmpKggFile);
        gEncodeGTFParser.submit();
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

    public static void main(String[] args) throws IOException {
        new GEncode2KggSeqProgram().setInputGTFFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/GEncode/gencode.v47.annotation.gtf.gz")
                .setOutputDir("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome")
                .setInputFNAFile("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/annotation/resource/genome/refGene/GRCh38_latest_genomic.fna.gz")
                .submit();
    }
}