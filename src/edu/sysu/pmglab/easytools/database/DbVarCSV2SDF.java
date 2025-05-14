//package edu.sysu.pmglab.easytools.database;
//
//import edu.sysu.pmglab.LogBackOptions;
//import edu.sysu.pmglab.bytecode.ByteStream;
//import edu.sysu.pmglab.bytecode.Bytes;
//import edu.sysu.pmglab.bytecode.BytesSplitter;
//import edu.sysu.pmglab.ccf.CCFWriter;
//import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
//import edu.sysu.pmglab.ccf.record.IRecord;
//import edu.sysu.pmglab.container.indexable.LinkedSet;
//import edu.sysu.pmglab.container.list.IntList;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.easytools.Constant;
//import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
//import edu.sysu.pmglab.io.FileUtils;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.reader.ReaderStream;
//import edu.sysu.pmglab.sdfa.SDFReaderMode;
//import edu.sysu.pmglab.sdfa.SDFTable;
//import edu.sysu.pmglab.sdfa.sv.SVContig;
//import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
//import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
//import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;
//import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Iterator;
//import java.util.UUID;
//
///**
// * @author Wenjie Peng
// * @create 2024-12-05 23:35
// * @description
// */
//public class DbVarCSV2SDF {
//    String file;
//    String outputDir;
//    boolean buildPhenotype;
//    private LinkedSet<Bytes> header;
//
//    private static final Bytes N = new Bytes("N");
//
//    public void convert() throws IOException {
//        check();
//        SVContig contig = SVContig.init();
//        ByteStream cache = new ByteStream();
//        ReaderStream readerStream = LiveFile.of(file).openAsText();
//
//        String tmpOutput = FileUtils.getSubFile(outputDir, UUID.randomUUID().toString());
//        CCFWriter writer = new CCFWriter(new File(tmpOutput), SDFReaderMode.ALL_MODE.getMandatoryFields());
//        IRecord record = writer.getRecord();
//        List<IRecord> encodeSDSVs = new List<>();
//
//        while (readerStream.readline(cache) != -1) {
//            Bytes line = cache.toBytes();
//            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
//                cache.clear();
//                continue;
//            }
//            header = new LinkedSet();
//            BytesSplitter splitter = new BytesSplitter(Constant.TAB);
//            splitter.init(line);
//            while (splitter.hasNext()) {
//                Bytes item = splitter.next().detach();
//                header.add(item);
//            }
//            break;
//        }
//        cache.clear();
//        int indexOfFile = 0;
//        IntList emptyAnnotationIndexes = new IntList();
//        List<Bytes> emptyGtyFields = List.wrap(new Bytes[0]);
//        Bytes genotype = new GTBox(GenotypeConstant.EMPTY_ENUMERATED_GENOTYPES).encode();
//        record.set(3, genotype);
//        record.set(4, emptyGtyFields);
//        record.set(6, N);
//        record.set(7, new Bytes("."));
//        record.set(8, new Bytes("."));
//        record.set(9, new Bytes("PASS"));
//        record.set(12, emptyAnnotationIndexes);
//        record.set(13, emptyAnnotationIndexes);
//        SVCoordinate coordinate;
//        // contains 6 levels:
//        // 1. SAMPLESET_ID
//        // 2. VALIDATION
//        // 3. PHENOTYPE
//        // 4. CLINIC_INTERPRETATION
//        // 5. CI_POS
//        // 6. CI_END
//        List<Bytes> infoField;
//        while (readerStream.readline(cache) != -1) {
//            infoField = new List<>(6);
//            indexOfFile++;
//            List<Bytes> items = new List<>();
//            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
//            while (iterator.hasNext()) items.add(iterator.next().detach());
//
//            Bytes variantID = items.fastGet(1);
//            Bytes variantType = items.fastGet(3);
//            Bytes sampleSetID = items.fastGet(4);
//            Bytes valid = items.fastGet(7);
//            Bytes phenotype = items.fastGet(9);
//            Bytes clinicInterpretation = items.fastGet(10);
//            Bytes chromosome = items.fastGet(13);
//            Bytes outerStart = items.fastGet(14);
//            Bytes start = items.fastGet(15);
//            Bytes innerStart = items.fastGet(16);
//            Bytes innerEnd = items.fastGet(17);
//            Bytes end = items.fastGet(18);
//            Bytes outerEnd = items.fastGet(19);
//            IntList startInfo = calcPos(innerStart, start, outerStart, true);
//            IntList endInfo = calcPos(innerEnd, end, outerEnd, false);
//            if (endInfo == null) {
//                endInfo = IntList.wrap(-1, 0, 0);
//            }
//            if (startInfo == null) {
//                cache.clear();
//                continue;
//            }
//            int chrIndex = contig.getContigIndexByName(chromosome.toString());
//            coordinate = new SVCoordinate(
//                    startInfo.fastGet(0),
//                    endInfo.fastGet(0),
//                    chrIndex
//            );
//            contig.countContigByIndex(chrIndex);
//            int length = coordinate.getEnd() == -1 ? -1 : Math.abs(coordinate.getEnd() - coordinate.getPos());
//            SVTypeSign type = SVTypeSign.getByName(variantType);
//            infoField.set(0, sampleSetID.detach());
//            infoField.set(1, valid.detach());
//            infoField.set(2, phenotype.detach());
//            infoField.set(3, clinicInterpretation.detach());
//            infoField.set(4, new Bytes(startInfo.fastGet(1) + "," + startInfo.fastGet(2)));
//            infoField.set(5, new Bytes(endInfo.fastGet(1) + "," + endInfo.fastGet(2)));
//
//            record.set(0, IntList.wrap(coordinate.encode()));
//            record.set(1, length);
//            record.set(2, type.getIndex());
//            record.set(5, variantID.detach());
//            record.set(10, infoField);
//            record.set(11, indexOfFile);
//            encodeSDSVs.add(record.clone());
//            record.clear();
//            cache.clear();
//        }
//        readerStream.close();
//        encodeSDSVs.sort(SVCoordinate.encoderSDSVComparator);
//        for (int i = 0; i < encodeSDSVs.size(); i++) {
//            writer.write(encodeSDSVs.fastGet(i));
//        }
//        // add contig info
//        List<CCFMetaItem> contigInfoSet = contig.save();
//        for (CCFMetaItem contigInfo : contigInfoSet) {
//            writer.addMeta(contigInfo);
//        }
//        // add individual
//        writer.addMeta(CCFMetaItem.of(SDFTable.SDF_INDIVIDUAL_TAG, List.wrap("sample")));
//        // info
//        String[] infoKeys = new String[]{
//                // contains 6 levels:
//                "SAMPLESET_ID",
//                "VALIDATION",
//                "PHENOTYPE",
//                "CLINIC_INTERPRETATION",
//                "CI_POS",
//                "CI_END"
//        };
//        writer.addMeta(CCFMetaItem.of(VCFInfoManager.name(), infoKeys));
//        writer.close();
//        File trueOutputFile = new File(FileUtils.getSubFile(outputDir, new File(file).getName() + ".sdf"));
//        if (trueOutputFile.exists()) {
//            LogBackOptions.getRootLogger().warn("The output file is located at " + tmpOutput);
//        } else {
//            writer.getFile().renameTo(trueOutputFile);
//            LogBackOptions.getRootLogger().info("The output file is located at " + trueOutputFile.getPath());
//        }
//    }
//
//    private void check() {
//        // CSV extension check
//    }
//
//    public DbVarCSV2SDF setFile(String file) {
//        this.file = file;
//        return this;
//    }
//
//    public DbVarCSV2SDF setOutputDir(String outputDir) {
//        this.outputDir = outputDir;
//        return this;
//    }
//
//    public static void main(String[] args) throws IOException {
//        DbVarCSV2SDF instance = new DbVarCSV2SDF()
//                .setFile("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar/supporting_variants_for_nstd102.txt")
//                .setOutputDir("/Users/wenjiepeng/Desktop/SDFA3.0/SV_resource/public_database/dbVar");
//        instance.convert();
//    }
//
//    IntList calcPos(Bytes inner, Bytes pos, Bytes outer, boolean isStart) {
//        int tmpInner = -1;
//        int tmpPos = -1;
//        int tmpOuter = -1;
//        try {
//            tmpInner = inner.toInt();
//        } catch (NumberFormatException e) {
//            tmpInner = -1;
//        }
//        try {
//            tmpPos = pos.toInt();
//        } catch (NumberFormatException e) {
//            tmpPos = -1;
//        }
//        try {
//            tmpOuter = outer.toInt();
//        } catch (NumberFormatException e) {
//            tmpOuter = -1;
//        }
//        int truePos = tmpPos != -1 ? tmpPos : (tmpInner != -1 ? tmpInner : tmpOuter);
//        if (truePos == -1) {
//            return null;
//        }
//        int forwardCI, backwardCI;
//        if (isStart) {
//            forwardCI = tmpOuter == -1 ? 0 : (truePos - tmpOuter);
//            backwardCI = tmpInner == -1 ? 0 : (tmpInner - truePos);
//        } else {
//            forwardCI = tmpInner == -1 ? 0 : (truePos - tmpInner);
//            backwardCI = tmpOuter == -1 ? 0 : (tmpOuter - truePos);
//        }
//        return IntList.wrap(truePos, forwardCI, backwardCI);
//    }
//}
