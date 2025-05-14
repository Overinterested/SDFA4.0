package edu.sysu.pmglab.sdfa.sv;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.record.Record;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.type.basic.BytesBox;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.genotype.IGenotypes;
import edu.sysu.pmglab.gtb.genome.genotype.encoder.Encoder;
import edu.sysu.pmglab.sdfa.SDFTable;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFFormatManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFHeaderManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInstance;
import edu.sysu.pmglab.sdfa.sv.vcf.encode.DefaultAltAttributeBox;
import edu.sysu.pmglab.sdfa.sv.vcf.encode.DefaultRefAttributeBox;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-08-27 21:20
 * @description
 */
public class SDSVConversionManager {

    private IRecord record;
    private CCFWriter writer;
    // transfer box
    private Encoder genotypeEncode;
    private BytesBox idAttributeBox;
    private BytesBox refAttributeBox;
    private BytesBox altAttributeBox;
    private BytesBox qualAttributeBox;
    private BytesBox filterAttributeBox;
    private final List<IRecord> records = new List<>();
    // info keys
    private SVContig contig;
    private LinkedSet<Bytes> fixedInfoKeys;
    private LinkedSet<Bytes> fixedFormatKeys;

    private static final int[] EMPTY_CSV_CHR_INDEXES = new int[0];
    private static final List<Bytes> EMPTY_INFO = List.wrap(new Bytes[0]);
    private static final IntList EMPTY_ANNOTATION_INDEXES = IntList.wrap();


    public SDSVConversionManager() {
        idAttributeBox = new BytesBox();
        refAttributeBox = new DefaultRefAttributeBox();
        altAttributeBox = new DefaultAltAttributeBox();
        qualAttributeBox = new BytesBox();
        filterAttributeBox = new BytesBox();
        genotypeEncode = new Encoder();
        record = new Record(SDFReadType.FULL.getReaderMode().getMandatoryFields());
    }


    public void parseToRecordsAndStore(Bytes id, Bytes ref, Bytes alt, Bytes qual, Bytes filter,
                                       VCFInfoManager infoManager, SVGenotypes svGenotypes,
                                       List<SVCoordinate> coordinateList, int length, int indexOfFile) throws IOException {
        int numOfSV = coordinateList.size();
        id = idAttributeBox.set(id).encode();
        ref = refAttributeBox.set(ref).encode();
        alt = altAttributeBox.set(alt).encode();
        qual = qualAttributeBox.set(qual).encode();
        filter = filterAttributeBox.set(filter).encode();
        int typeIndex = infoManager.getType().getIndex();
        int[] indexesOfChr;
        if (numOfSV != 1) {
            indexesOfChr = new int[numOfSV];
            for (int i = 0; i < numOfSV; i++) {
                indexesOfChr[i] = coordinateList.fastGet(i).getIndexOfChr();
            }
        } else {
            indexesOfChr = EMPTY_CSV_CHR_INDEXES;
        }
        List<Bytes> infoValues = infoManager.getEssentialValues();
        // gty
        IGenotypes genotypes = svGenotypes == null || svGenotypes.getGtyBox() == null ? GenotypeConstant.EMPTY_ENUMERATED_GENOTYPES : svGenotypes.getGtyBox().get();
        List<Bytes> gtyMetricValues = svGenotypes.getEncodedAttrs();
        Bytes gtyEncode = null;
        gtyEncode = genotypeEncode.encode(genotypes);
        // start end length check
        for (int i = 0; i < numOfSV; i++) {
            SVCoordinate coordinate = coordinateList.fastGet(i);
            if (length < -1 && coordinate.getPos() - coordinate.getEnd() == length) {
                int tmp = coordinate.pos;
                coordinate.setEnd(coordinate.end);
                coordinate.setPos(tmp);
                length = -length;
            }
            id = id.detach();
            ref = ref.detach();
            alt = alt.detach();
            qual = qual.detach();
            filter = filter.detach();
            gtyEncode = gtyEncode.detach();
            infoValues = detachNewByteCodeList(infoValues);
            gtyMetricValues = detachNewByteCodeList(gtyMetricValues);
            record = record.clone();
            records.add(record);
            svGenotypes.clear();

            record.set(0, IntList.wrap(coordinateList.fastGet(i).encode()))
                    .set(1, length)
                    .set(2, typeIndex)
                    .set(3, gtyEncode)
                    .set(4, gtyMetricValues)
                    .set(5, id)
                    .set(6, ref)
                    .set(7, alt)
                    .set(8, qual)
                    .set(9, filter)
                    .set(10, infoValues)
                    .set(11, indexOfFile)
                    .set(12, IntList.wrap(getIndexesOfChr(indexesOfChr, i)))
                    .set(13, EMPTY_ANNOTATION_INDEXES);
        }
    }

    public IRecord unsafeEncodeRecord(IRecord record) {
        return record.set(5, idAttributeBox.set((Bytes) record.get(5)).encode())
                .set(6, refAttributeBox.set((Bytes) record.get(6)).encode())
                .set(7, altAttributeBox.set((Bytes) record.get(7)).encode())
                .set(8, qualAttributeBox.set((Bytes) record.get(8)).encode())
                .set(9, filterAttributeBox.set((Bytes) record.get(9)).encode());
    }

    private int[] getIndexesOfChr(int[] rawIndexesOfChr, int index) {
        if (rawIndexesOfChr == EMPTY_CSV_CHR_INDEXES) {
            return EMPTY_CSV_CHR_INDEXES;
        }
        int[] res = new int[rawIndexesOfChr.length];
        res[index] = -1;
        return res;
    }

    public void initWriter(File output) throws IOException {
        writer = CCFWriter.setOutput(output).addFields(SDFReadType.FULL.getReaderMode().getMandatoryFields()).instance();
    }

    public void close(VCFInstance vcfInstance) throws IOException {
        if (writer != null && !writer.isClosed()) {
            // contig: names and ranges
            SVContig contig = vcfInstance.getContig();
            List<CCFMetaItem> contigRangeItems = contig.save();
            for (CCFMetaItem contigRangeItem : contigRangeItems) {
                writer.addMeta(contigRangeItem);
            }
            // individuals
            writer.addMeta(new CCFMetaItem(SDFTable.SDF_INDIVIDUAL_TAG, FieldType.stringIndexableSet, vcfInstance.getIndividuals()));
            // header
            List<Bytes> header = vcfInstance.getHeaderManager().getHeader();
            if (header != null && !header.isEmpty()) {
                CCFMetaItem headerMeta = CCFMetaItem.of(
                        VCFHeaderManager.name(),
                        header.apply(Bytes::toString).toArray(new String[0])
                );
                writer.addMeta(headerMeta);
            }
            // info
            VCFInfoManager infoManager = vcfInstance.getInfoManager();
            writer.addMeta(infoManager.save());
            // format
            VCFFormatManager formatManager = vcfInstance.getFormatManager();
            writer.addMeta(formatManager.save());
            // data
            records.sort((o1, o2) -> {
                SVCoordinate var1 = SVCoordinate.decode(o1.get(0));
                SVCoordinate var2 = SVCoordinate.decode(o2.get(0));
                return var1.compareTo(var2);
            });
            while (!records.isEmpty()) {
                IRecord tmpRecord = records.popFirst();
                writer.write(tmpRecord);
                tmpRecord.clear();
            }
            // close
            writer.close();
        }
    }

    public BytesBox getIdAttributeBox() {
        return idAttributeBox;
    }

    public BytesBox getRefAttributeBox() {
        return refAttributeBox;
    }

    public BytesBox getAltAttributeBox() {
        return altAttributeBox;
    }

    public BytesBox getQualAttributeBox() {
        return qualAttributeBox;
    }

    public BytesBox getFilterAttributeBox() {
        return filterAttributeBox;
    }

    public SDSVConversionManager setIdAttributeBox(BytesBox idAttributeBox) {
        this.idAttributeBox = idAttributeBox;
        return this;
    }

    public SDSVConversionManager setRefAttributeBox(BytesBox refAttributeBox) {
        this.refAttributeBox = refAttributeBox;
        return this;
    }

    public SDSVConversionManager setAltAttributeBox(BytesBox altAttributeBox) {
        this.altAttributeBox = altAttributeBox;
        return this;
    }

    public SDSVConversionManager setQualAttributeBox(BytesBox qualAttributeBox) {
        this.qualAttributeBox = qualAttributeBox;
        return this;
    }

    public SDSVConversionManager setFilterAttributeBox(BytesBox filterAttributeBox) {
        this.filterAttributeBox = filterAttributeBox;
        return this;
    }

    private List<Bytes> detachByteCodeList(List<Bytes> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                Bytes item = list.fastGet(i);
                list.set(i, item == null ? null : item.detach());
            }
            return list;
        } else {
            return EMPTY_INFO;
        }
    }

    private List<Bytes> detachNewByteCodeList(List<Bytes> list) {
        if (list == null) {
            return EMPTY_INFO;
        }
        List<Bytes> returns = new List<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Bytes item = list.fastGet(i);
            returns.set(i, item == null ? null : item.detach());
        }
        return returns;
    }

    public IRecord getRecord() {
        return record;
    }


    public SDSVConversionManager setFixedInfoKeys(LinkedSet<Bytes> fixedInfoKeys) {
        this.fixedInfoKeys = fixedInfoKeys;
        return this;
    }

    public LinkedSet<Bytes> getFixedInfoKeys() {
        return fixedInfoKeys;
    }

    public LinkedSet<Bytes> getFixedFormatKeys() {
        return fixedFormatKeys;
    }

    public SDSVConversionManager setFixedFormatKeys(LinkedSet<Bytes> fixedFormatKeys) {
        this.fixedFormatKeys = fixedFormatKeys;
        return this;
    }

    public SDSVConversionManager setContig(SVContig contig) {
        this.contig = contig;
        return this;
    }

    public String getContigName(int index) {
        return contig.getContigNameByIndex(index);
    }
}
