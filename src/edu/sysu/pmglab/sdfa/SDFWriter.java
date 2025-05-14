package edu.sysu.pmglab.sdfa;//package edu.sysu.pmglab.sdfa;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.StringSplitter;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.meta.CCFMeta;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.toolkit.Sorter;
import edu.sysu.pmglab.ccf.toolkit.output.CCFOutputOption;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.viewer.CCFViewer;
import edu.sysu.pmglab.ccf.viewer.CCFViewerReader;
import edu.sysu.pmglab.container.indexable.*;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.coordinate.Coordinate;
import edu.sysu.pmglab.sdfa.base.SDFFormatManager;
import edu.sysu.pmglab.sdfa.base.SDFInfoManager;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.*;
import edu.sysu.pmglab.sdfa.sv.sdsv.CompleteSDSV;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFFormatManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrBox;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrType;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;
import edu.sysu.pmglab.sdfa.toolkit.SDFViewerReader;

import java.io.File;
import java.io.IOException;

/**
 * @author Yingsi Peng
 * @create 2024-08-30 00:45
 * @description
 */
public class SDFWriter {
    int fileID;
    int lineIndex;
    IRecord record;
    int thread = 4;
    File outputFile;
    final GTBox gtyBox;
    private CCFMeta meta;
    final SVContig contig;
    final SDFWriterRecord sv;
    private CCFWriter writer;
    final SDFInfoManager infoManager;
    final LinkedSet<String> individuals;
    final SDFFormatManager formatManager;
    SDSVConversionManager sdsvConversionManager;
    private static final IntList EMPTY_ANNOTATION = new IntList(new int[0]);

    private SDFWriter(int fileID, File outputFile, SVContig contig, SDFInfoManager infoManager, LinkedSet<String> individuals, SDFFormatManager formatManager) {
        this.fileID = fileID;
        this.contig = contig;
        this.outputFile = outputFile;
        this.infoManager = infoManager;
        this.individuals = individuals;
        this.gtyBox = new GTBox(individuals.size());
        this.formatManager = formatManager;
        this.writer = CCFWriter.setOutput(outputFile).addFields(SDFReadType.FULL.getReaderMode().getMandatoryFields()).instance();
        this.record = writer.getRecord();
        this.sdsvConversionManager = new SDSVConversionManager();
        this.sv = new SDFWriterRecord(gtyBox, this.infoManager, this.formatManager, this.infoManager.sizeOfInfo());
    }

    public void write(SDFWriterRecord sv) throws IOException {
        parse(sv);
        writer.write(record);
        contig.countContigByIndex(sv.getContigIndex());
        lineIndex++;
    }

    private void parse(SDFWriterRecord sv) {
        SVTypeSign type = sv.getType();
        if (type == null) {
            throw new UnsupportedOperationException("No SV type is assigned.");
        }
        formatManager.encodeTo(sv.formatList);
        record.set(0, IntList.wrap(sv.getCoordinate().encode()))
                .set(1, sv.length())
                .set(2, sv.getType().getIndex())
                .set(3, gtyBox.encode())
                .set(4, sv.formatList)
                .set(5, sdsvConversionManager.getIdAttributeBox().set(sv.getID()).encode())
                .set(6, sdsvConversionManager.getRefAttributeBox().set(sv.getRef()).encode())
                .set(7, sdsvConversionManager.getAltAttributeBox().set(sv.getAlt()).encode())
                .set(8, sdsvConversionManager.getQualAttributeBox().set(sv.getQuality()).encode())
                .set(9, sdsvConversionManager.getFilterAttributeBox().set(sv.getFilter()).encode())
                .set(10, sv.infoList)
                .set(11, lineIndex)
                .set(12, sv.getEncodeCSVLocation(lineIndex))
                .set(13, SDFWriter.EMPTY_ANNOTATION);
    }

    public static class SDFWriterBuild {
        int fileID;
        File outputFile;
        SVContig contig = SVContig.init();
        final LinkedSet<String> individuals = new LinkedSet<>();
        final IndexableSet<String> infoKeys = new LinkedSet<>();
        final IndexableMap<String, FormatAttrBox> formatDetails = new DynamicIndexableMap<>();

        private SDFWriterBuild(File outputFile) {
            this.outputFile = outputFile;
        }

        public static SDFWriterBuild of(File outputFile) {
            return new SDFWriterBuild(outputFile);
        }

        public SDFWriterBuild addIndividuals(String... individualNames) {
            individuals.addAll(individualNames);
            return this;
        }

        public SDFWriterBuild addInfoKeys(String... infoKeys) {
            this.infoKeys.addAll(infoKeys);
            return this;
        }

        public SDFWriterBuild addFormat(String formatKey) {
            if (formatKey.equals("GT")) {
                return this;
            }
            this.formatDetails.put(formatKey, FormatAttrType.getByName(formatKey));
            return this;
        }

        public SDFWriterBuild addFormatItem(String formatKey, FormatAttrBox formatType) {
            this.formatDetails.put(formatKey, formatType);
            return this;
        }

        public SDFWriter build() {
            Logger logger = LogBackOptions.getRootLogger();
            if (individuals.isEmpty()) {
                logger.warn("Init individual names is empty");
            }

            if (infoKeys.isEmpty()) {
                logger.warn("Init individual names is empty");
            }

            if (formatDetails.isEmpty()) {
                logger.warn("Init individual names is empty");
            }

            SDFWriter res = new SDFWriter(fileID, outputFile, contig, new SDFInfoManager(infoKeys), individuals, new SDFFormatManager(formatDetails));
            return res;
        }

        public SDFWriterBuild setFileID(int fileID) {
            this.fileID = fileID;
            return this;
        }
    }

    public void close() throws IOException, InterruptedException {
        writer.addMeta(getMeta());
        writer.close();
        Sorter.SorterSetting<ReaderOption, Integer, SVCoordinate> sort = Sorter.setInput(writer.getFile())
                .getTagFrom(record -> ((IntList) record.get(0)).fastGet(0))
                .getValueFrom(record -> SVCoordinate.decode(record.get(0)))
                .projectValue(coordinate -> coordinate.getPos());
        boolean ordered = sort.isOrdered(1);
        if (!ordered) {
            sort.memorySort(
                    new CCFOutputOption(writer.getFile())
                            .addFields(
                                    SDFReadType.FULL
                                            .getReaderMode()
                                            .getMandatoryFields()
                            ).addMeta(getMeta()),
                    thread
            );
        }

    }

    public static class SDFWriterRecord extends CompleteSDSV {
        private final GTBox gtyBox;
        private final List<Bytes> infoList;
        private final List<Bytes> formatList;
        private final SDFInfoManager infoManager;
        private final SDFFormatManager formatManager;
        private final static int[] INDEX_LINE = new int[1];
        // for csv
        boolean csv = false;
        List<Coordinate> coordinates = new List<>();
        List<CSVLocation> csvLocations = new List<>();

        private static final StringSplitter valueSplitter = new StringSplitter(';');

        private SDFWriterRecord(GTBox gtyBox, SDFInfoManager infoManager, SDFFormatManager formatManager, int infoSize) {
            this.gtyBox = gtyBox;
            this.infoManager = infoManager;
            this.formatManager = formatManager;
            this.infoList = new List<>(infoManager.sizeOfInfo());
            infoList.fill(null, infoManager.sizeOfInfo());
            this.formatList = new List<>(formatManager.size());
            formatList.fill(null, formatManager.size());
        }

        public SDFWriterRecord addInitFormatAttrs(int sampleIndex, String values) {
            int index = 0;
            valueSplitter.init(values);
            while (valueSplitter.hasNext()) {
                String next = valueSplitter.next();
                if (index == 0) {
                    gtyBox.loadOne(sampleIndex, new Bytes(next));
                } else {
                    formatManager.getBox(index - 1).loadOne(new Bytes(next));
                }
                index++;
            }
            return this;
        }

        public SDFWriterRecord addFormatAttr(String formatAttrKey, String formatAttrValue) {
            return addFormatAttr(formatAttrKey, new Bytes(formatAttrValue));
        }

        @Override
        public SDFWriterRecord setFileID(int fileID) {
            throw new UnsupportedOperationException("File ID can't be override.");
        }

        @Override
        public SDFWriterRecord setLength(int length) {
            super.setLength(length);
            return this;
        }

        @Override
        public SDFWriterRecord setType(SVTypeSign type) {
            super.setType(type);
            return this;
        }

        @Override
        public SDFWriterRecord setID(Bytes ID) {
            super.setID(ID);
            return this;
        }

        @Override
        public SDFWriterRecord setRef(Bytes ref) {
            super.setRef(ref);
            return this;
        }

        @Override
        public SDFWriterRecord setAlt(Bytes alt) {
            super.setAlt(alt);
            return this;
        }

        @Override
        public SDFWriterRecord setFilter(Bytes filter) {
            super.setFilter(filter);
            return this;
        }

        @Override
        public SDFWriterRecord setQuality(Bytes quality) {
            super.setQuality(quality);
            return this;
        }

        public SDFWriterRecord addFormatAttr(String formatAttrKey, Bytes formatAttrValue) {
            if (formatAttrKey.equals("GT")) {
                return this;
            }
            int index = formatManager.indexOf(formatAttrKey);
            if (index == -1) {
                throw new UnsupportedOperationException("No format key named " + formatAttrKey);
            }
            formatManager.getBox(index).loadOne(formatAttrValue);
            return this;
        }

        public SDFWriterRecord addFormatAttr(Bytes formatAttrKey, Bytes formatAttrValue) {
            if (formatAttrKey.equals(GenotypeConstant.GT)) {
                return this;
            }
            int index = formatManager.indexOf(formatAttrKey);
            if (index == -1) {
                throw new UnsupportedOperationException("No format key named " + formatAttrKey);
            }
            formatManager.getBox(index).loadOne(formatAttrValue);
            return this;
        }

        public SDFWriterRecord setInfo(Bytes infoKey, String infoValue) {
            return setInfo(infoKey, new Bytes(infoValue));
        }

        public SDFWriterRecord setInfo(String infoKey, String infoValue) {
            return setInfo(infoKey, new Bytes(infoValue));
        }

        public SDFWriterRecord setInfo(String infoKey, Bytes infoValue) {
            int index = infoManager.indexOf(infoKey);
            if (index == -1) {
                throw new UnsupportedOperationException("No info key named " + infoKey);
            }
            infoList.fastSet(index, infoValue);
            return this;

        }

        @Override
        public SDFWriterRecord setPos(int pos) {
            super.setPos(pos);
            return this;
        }

        @Override
        public SDFWriterRecord setEnd(int end) {
            super.setEnd(end);
            return this;
        }

        @Override
        public SDFWriterRecord setChrName(String contigName) {
            super.setChrName(contigName);
            return this;
        }

        @Override
        public SDFWriterRecord setChrIndex(int chrIndex) {
            super.setChrIndex(chrIndex);
            return this;
        }

        public SDFWriterRecord setInfo(Bytes infoKey, Bytes infoValue) {
            int index = infoManager.indexOf(infoKey);
            if (index == -1) {
                throw new UnsupportedOperationException("No info key named " + infoKey);
            }
            infoList.fastSet(index, infoValue);
            return this;
        }

        public IntList getEncodeCSVLocation(int lineIndex) {
            CSVLocation csvLocation = super.getCsvLocation();
            if (csvLocation == null) {
                INDEX_LINE[0] = lineIndex;
                return IntList.wrap(INDEX_LINE);
            } else {
                return IntList.wrap(csvLocation.encodeIndexesOfCSV());
            }
        }

        public SDFWriterRecord setLineIndexOfFile(int lineIndexOfFile) {
            CSVLocation csvLocation = getCsvLocation();
            if (csvLocation == null) {
                setCsvLocation(new CSVLocation(lineIndexOfFile, EMPTY_ANNOTATION));
            } else {
                csvLocation.setIndexInFile(lineIndexOfFile);
            }
            return this;
        }

        public SDFWriterRecord setCoordinate(String chrName, int pos, int end) {
            return setChrName(chrName).setPos(pos).setEnd(end);
        }
//        public SDFWriterRecord setCoordinates(String[] chrNames, int[] posList, int[] endList, boolean retainAllInfo) {
//            if (chrNames != null && posList != null && endList != null) {
//                if (chrNames.length == posList.length && chrNames.length == endList.length) {
//                    int size = chrNames.length;
//                    if (size == 1) {
//                        return setCoordinate(chrNames[0], posList[0], endList[0]);
//                    }
//                    int[] chrIndexes = new int[size];
//                    IndexedSort sort = IndexedSort.sort(chrIndexes);
//
//                    return this;
//                }
//                throw new UnsupportedOperationException("The sizes of three does not match.");
//            } else {
//                throw new UnsupportedOperationException("Exist NULL value.");
//            }
//        }
    }

    public CCFMeta getMeta() {
        if (meta == null) {
            CCFMeta meta = new CCFMeta();
            List<CCFMetaItem> contigMetas = contig.save();
            for (CCFMetaItem contigMeta : contigMetas) {
                meta.add(contigMeta);
            }
            meta.add(new CCFMetaItem(SDFTable.SDF_INDIVIDUAL_TAG, FieldType.stringIndexableSet, individuals));
            // info
            meta.add(CCFMetaItem.of(VCFInfoManager.name(), getInfoKeys()));
            // format
            meta.add(CCFMetaItem.of(VCFFormatManager.name(), getFormatNames()));
            this.meta = meta;
        }
        return meta;
    }

    private String[] getInfoKeys() {
        int count = 0;
        if (infoManager != null) {
            IndexableSet<Bytes> infoKeys = infoManager.getInfoKeys();
            if (infoKeys != null && !infoKeys.isEmpty()) {
                int size = infoKeys.size();
                String[] infoNames = new String[size];
                for (Bytes infoKey : infoKeys) {
                    infoNames[count++] = infoKey.toString();
                }
                return infoNames;
            }
            return new String[0];
        }
        return new String[0];
    }

    private String[] getFormatNames() {
        int count = 0;
        if (formatManager == null) {
            IndexableSet<Bytes> formatAttrNameList = formatManager.getFormatAttrNameList();
            if (formatAttrNameList != null && !formatAttrNameList.isEmpty()) {
                int size = formatAttrNameList.size();
                String[] formatNames = new String[size];
                for (int i = 0; i < size; i++) {
                    formatNames[count++] = formatAttrNameList.valueOf(i).toString();
                }
                return formatNames;
            }
            return new String[0];
        }
        return new String[0];
    }

    public SDFWriterRecord getTemplateSV() {
        return sv;
    }

}
