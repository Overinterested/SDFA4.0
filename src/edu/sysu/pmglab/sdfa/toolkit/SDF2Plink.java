package edu.sysu.pmglab.sdfa.toolkit;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.gtb.genome.genotype.Genotype;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.SVGenotypes;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.container.SDSVManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-10-11 03:17
 * @description
 */
public class SDF2Plink {
    File sdfFile;
    File pedFile;
    File outputDir;
    Bytes familyID;
    String outputName;
    Boolean controlSample;
    Bytes separatorForFamily;

    private SDF2Plink(File sdfFile, File outputDir) {
        this.sdfFile = sdfFile;
        this.outputDir = outputDir;
    }

    private static final byte[] BED_HEADER = {0x6C, 0x1B, 0x01};
    private static final byte[] FATHER_MOTHER_SEX_TAG = {Constant.ZERO, Constant.TAB, Constant.ZERO, Constant.TAB, Constant.ZERO};
    private static final byte[] default_family_id = new byte[]{Constant.F, Constant.a, Constant.m, Constant.I, Constant.D};

    public static SDF2Plink of(Object sdfFile, Object outputDir) {
        return new SDF2Plink(new File(sdfFile.toString()), new File(outputDir.toString())
        );
    }

    public void submit() throws IOException {
        outputDir.mkdirs();
        SDFReader sdfReader = new SDFReader(sdfFile, SDFReadType.PLINK);
        LinkedSet<String> individuals = sdfReader.getIndividuals();
        writeFamFile(individuals);
        int subjectSize = individuals.size();
        if (outputName == null) {
            Logger logger = LogBackOptions.getRootLogger();
            logger.warn("Output will be stored in a file starting with a dot, as no file name is specified.");
        }
        String bimFileName = outputName == null ? ".bim" : outputName + ".bim";
        String bedFileName = outputName == null ? ".bed" : outputName + ".bed";
        WriterStream bimFile = new WriterStream(FileUtils.getSubFile(outputDir, bimFileName), WriterStream.Option.DEFAULT);
        WriterStream bedFile = new WriterStream(FileUtils.getSubFile(outputDir, bedFileName), WriterStream.Option.DEFAULT);
        bedFile.write(BED_HEADER);
        ISDSV sv;
        ProgressBar bar = new ProgressBar.Builder().setTextRenderer("Conversion Speed", "SVs").setInitialMax(sdfReader.getReader().numOfRecords()).build();
        while ((sv = sdfReader.read()) != null) {
            // write bim: contig, ID, centiMorgan(0 by default), POS, MIN_AF_ALLELE, MAX_AF_ALLELE
            bimFile.write(ASCIIUtility.toASCII(sv.nameOfContig(), Constant.CHAR_SET));
            bimFile.write(Constant.TAB);
            bimFile.write(sv.getID());
            bimFile.write(Constant.TAB);
            bimFile.write(Constant.ZERO);
            bimFile.write(Constant.TAB);
            bimFile.write(ASCIIUtility.toASCII(sv.getPos()));
            bimFile.write(Constant.TAB);
            bimFile.write(sv.getAlt());
            bimFile.write(Constant.TAB);
            bimFile.write(sv.getRef());
            bimFile.write(Constant.NEWLINE);
            // write bed: genotypes
            bedFile.write(gty2BEDBytes(sv.getSVGenotypes(), subjectSize));
            bar.step(1);
        }
        bar.close();
        bimFile.close();
        bedFile.close();
    }

    private static byte[] gty2BEDBytes(SVGenotypes gtys, int subjectSize) {
        byte[] genotypeBytes = new byte[(subjectSize + 3) / 4];
        Arrays.fill(genotypeBytes, (byte) 0x00);
        Genotype hom1 = GenotypeConstant.Wild_TYPE_Homozygous;
        Genotype wildGty1 = GenotypeConstant.Mutant_Homozygous;
        Genotype het1 = GenotypeConstant.Heterozygous_0_1;
        Genotype het2 = GenotypeConstant.Heterozygous_1_0;
        byte genoCode;
        for (int i = 0; i < gtys.numOfSamples(); i++) {
            Genotype gty = gtys.getGty(i);
            if (gty.equals(hom1)) {
                // 0/0
                genoCode = 0b00;
            } else if (gty.equals(wildGty1)) {
                // 1/1
                genoCode = 0b11;
            } else if (gty.equals(het1) || gty.equals(het2)) {
                // 0/1
                genoCode = 0b10;
            } else {
                // missing
                genoCode = 0b01;
            }
            genotypeBytes[i / 4] |= (genoCode << ((i % 4) * 2));
        }
        return genotypeBytes;
    }

    private void writeFamFile(LinkedSet<String> subjects) throws IOException {
        Logger logger = LogBackOptions.getRootLogger();
        // init
        String famFileName = outputName == null ? ".fam" : outputName + ".fam";
        WriterStream famWriter = new WriterStream(FileUtils.getSubFile(outputDir, famFileName), WriterStream.Option.DEFAULT);

        if (pedFile != null) {
            List<FamRecord> individualsFromPed = new List<>();
            ByteStream cache = new ByteStream();
            ReaderStream pedReader = new ReaderStream(pedFile.toString(), ReaderStream.Option.DEFAULT);
            while (pedReader.readline(cache) != -1) {
                FamRecord famRecord = new FamRecord(cache.toBytes());
                int index = subjects.indexOf(famRecord.iid.toString());
                if (index == -1) {
                    cache.clear();
                    continue;
                }
                famRecord.ID = index;
                famRecord.asUnmodified();
                individualsFromPed.add(famRecord);
                famWriter.write(ASCIIUtility.toASCII(famRecord.toString(), Constant.CHAR_SET));
                famWriter.write(Constant.NEWLINE);
                cache.clear();
            }
            pedReader.close();
            individualsFromPed.sort(FamRecord::compareTo);
            logger.info("Load " + individualsFromPed.size() + " individuals from PED file.");
            File famFile = FileUtils.getSubFile(outputDir, ".fam");
            WriterStream writer = new WriterStream(famFile, WriterStream.Option.DEFAULT);
            for (FamRecord famRecord : individualsFromPed) {
                writer.write(ASCIIUtility.toASCII(famRecord.toString(), Charset.defaultCharset()));
                writer.write(Constant.NEWLINE);
            }
            pedReader.close();
            famWriter.close();
            return;
        }
        logger.warn("No ped file passed in and built the fam file with all individuals from SDF file with the phenotype at '0'.");
        ByteStream cache = new ByteStream();
        for (int i = 0; i < subjects.size(); i++) {
            Bytes name = new Bytes(subjects.valueOf(i));
            //region parse the family ID and sample ID
            if (familyID != null) {
                //region has defined family
                cache.write(familyID);
                cache.write(Constant.TAB);
                cache.write(name);
                //endregion
            } else {
                //region need parse for family id
                separatorForFamily = separatorForFamily == null ? new Bytes(new byte[]{Constant.UNDERLINE}) : separatorForFamily;
                List<Bytes> split = new List<>();
                Iterator<Bytes> iterator = name.split(Constant.UNDERLINE);
                while (iterator.hasNext()) split.add(iterator.next().detach());

                if (split.size() == 2) {
                    cache.write(split.get(0));
                    cache.write(Constant.TAB);
                    cache.write(split.get(1));
                } else {
                    if (split.size() > 2) {
                        cache.write(split.get(0));
                        cache.write(Constant.TAB);
                        cache.write(name.subBytes(split.size()));
                    } else {
                        cache.write(default_family_id);
                        cache.write(Constant.TAB);
                        cache.write(name);
                    }
                }
                //endregion
            }
            cache.write(Constant.TAB);
            cache.write(FATHER_MOTHER_SEX_TAG);
            cache.write(Constant.TAB);
            if (controlSample == null) {
                cache.write(Constant.ZERO);
            } else {
                if (controlSample) {
                    cache.write(Constant.ONE);
                } else {
                    cache.write(Constant.TWO);
                }
            }
            cache.write(Constant.NEWLINE);
            famWriter.write(cache.toBytes());
            cache.clear();
            //endregion
        }
        famWriter.close();
    }

    public SDF2Plink setFamilyID(Bytes familyID) {
        this.familyID = familyID;
        return this;
    }

    public SDF2Plink setControlSample(Boolean controlSample) {
        this.controlSample = controlSample;
        return this;
    }

    public SDF2Plink setSeparatorForFamily(Bytes separatorForFamily) {
        this.separatorForFamily = separatorForFamily;
        return this;
    }

    public SDF2Plink setPedFile(File pedFile) {
        this.pedFile = pedFile;
        return this;
    }

    public static void main(String[] args) throws IOException {
        Workflow workflow = new Workflow(4);
        List<Pipeline> pipelines = SDSVManager.of("/Users/wenjiepeng/Desktop/SDFA3.0/concat/vcf").setOutputDir("/Users/wenjiepeng/Desktop/SDFA3.0/concat/sdf_1").parseToSDFFileTask();
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }
        workflow.execute();
        workflow.clearTasks();
        CCFReader reader = new CCFReader("/Users/wenjiepeng/Desktop/SDFA3.0/concat/sdf_1/sdf/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf");
        reader.limit(reader.available());
        while (reader.read() != null) {
            int a = 1;
        }
        SDF2Plink.of(
                "/Users/wenjiepeng/Desktop/SDFA3.0/concat/sdf_1/sdf/HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf.sdf",
                "/Users/wenjiepeng/Desktop/SDFA3.0/concat/vcf"
        ).setPedFile(new File("/Users/wenjiepeng/Desktop/SDFA3.0/concat/vcf/01258.ped")).submit();

    }

    public SDF2Plink setOutputName(String outputName) {
        this.outputName = outputName;
        return this;
    }

}

class FamRecord implements Comparable<FamRecord> {
    int ID;
    Bytes famid;
    Bytes iid;
    Bytes fid;
    Bytes mid;
    int sex;
    int phenotype;

    @Override
    public int compareTo(FamRecord o) {
        return Integer.compare(ID, o.ID);
    }

    public FamRecord(Bytes eachRecordInPed) {
        Iterator<Bytes> split = eachRecordInPed.split(Constant.TAB);
        int count = 0;
        while (split.hasNext()) {
            switch (count++) {
                case 0:
                    famid = split.next();
                    break;
                case 1:
                    iid = split.next();
                    break;
                case 2:
                    fid = split.next();
                    break;
                case 3:
                    mid = split.next();
                    break;
                case 4:
                    sex = split.next().toInt();
                    break;
                case 5:
                    phenotype = split.next().toInt();
                    break;
            }
        }
    }

    public void asUnmodified() {
        famid = famid.detach();
        iid = iid.detach();
        fid = fid.detach();
        mid = mid.detach();
    }

    @Override
    public String toString() {
        return famid + "\t" + iid + "\t" + fid + "\t" + mid + "\t" + sex + "\t" + phenotype;
    }
}