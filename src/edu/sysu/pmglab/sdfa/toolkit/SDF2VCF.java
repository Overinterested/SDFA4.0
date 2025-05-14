package edu.sysu.pmglab.sdfa.toolkit;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.sdsv.CompleteSDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-10-17 21:24
 * @description
 */
public class SDF2VCF {
    LiveFile sdfPath;
    File outputVCFPath;

    public void submit() throws IOException {
        ByteStream cache = new ByteStream();
        SDFReader reader = new SDFReader(sdfPath);
        WriterStream writer = new WriterStream(outputVCFPath, WriterStream.Option.DEFAULT);
        CompleteSDSV read = (CompleteSDSV) reader.read();
    }

    public static void main(String[] args) throws IOException {
        LiveFile sdfPath = LiveFile.of("");
        SDFReader reader = new SDFReader(sdfPath.getPath(), SDFReadType.FULL);
        SDSVConversionManager conversion = reader.getConversion();
    }

}
