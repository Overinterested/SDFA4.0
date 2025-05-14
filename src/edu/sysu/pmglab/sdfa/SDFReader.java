package edu.sysu.pmglab.sdfa;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.filter.CCFFilter;
import edu.sysu.pmglab.ccf.loader.CCFChunk;
import edu.sysu.pmglab.ccf.loader.CCFLoader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.interval.LongInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.base.SDFFormatManager;
import edu.sysu.pmglab.sdfa.base.SDFInfoManager;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.sdsv.*;
import edu.sysu.pmglab.sdfa.sv.vcf.SVFilterManager;
import edu.sysu.pmglab.utils.ValueUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2025-02-24 02:33
 * @description
 */
public class SDFReader {
    // template record
    private IRecord record;
    // last read pointer
    private long lastPointer;
    // true reader
    private CCFReader reader;
    // reader range
    private LongInterval range;
    private final SDFReaderOption option;
    // record conversion manager
    private final SDSVConversionManager conversion;


    public SDFReader(Object input) throws IOException {
        this(new SDFReaderOption(input, null));
    }


    public SDFReader(SDFReaderOption option) throws IOException {
        this.option = option;
        SDFTable sdfTable = option.getSDFTable();
        conversion = new SDSVConversionManager()
                .setContig(sdfTable.contig)
                .setFixedInfoKeys(sdfTable.infoManager.getInfoKeys())
                .setFixedFormatKeys(sdfTable.formatManager.getFormatAttrNameList());
        CCFTable table = new CCFTable(
                option.getFile(),
                SDFReaderOption.DATA_LOADER
        );
        reader = new CCFReader(new ReaderOption(sdfTable, option.getMandatoryFields()));
        record = reader.getRecord();
    }

    public SDFReader(String input, SDFReadType readerMode) throws IOException {
        this(new SDFReaderOption(input, readerMode));
    }

    public SDFReader(File input, SDFReadType readerMode) throws IOException {
        this(new SDFReaderOption(input, readerMode));
    }

    public SDFReader(LiveFile input, SDFReadType readerMode) throws IOException {
        this(new SDFReaderOption(input, readerMode));
    }

    public CCFTable getTable() {
        return this.option.getTable();
    }

    public long numOfRecords() {
        return this.reader.numOfRecords();
    }

    public SDFReaderOption getReaderOption() {
        return this.option;
    }

    public SDFFilter.SDFFilterBuilder createFilter() throws IOException {
        return new SDFFilter.SDFFilterBuilder();
    }

    public LongInterval range() {
        return this.reader.available();
    }

    private ISDSV parseToSV(IRecord record) {
        ISDSV sv;
        switch (option.getReaderMode()) {
            case FULL:
                sv = new CompleteSDSV();
                break;
            case PLINK:
                sv = new SimpleSDSVForPlink();
                break;
            case MERGE:
                sv = new SimpleSDSVForMerge();
                break;
            case COORDINATE:
                sv = new SimpleSDSV();
                break;
            case ANNOTATION:
                sv = new SimpleSDSVForAnnotation();
                break;
            case ANNOTATION_GT:
                sv = new SimpleSDSVForAnnotationWithGty();
                break;
            default:
                throw new UnsupportedOperationException("Undefined reader mode" );
        }
        sv.parseRecord(record, conversion);
        sv.setChrName(getContigByIndex(sv.getContigIndex()));
        return sv;
    }

    public ISDSV read() throws IOException {
        ISDSV sv = null;
        record = reader.getRecord();
        if (this.reader.read(record)) {
            sv = parseToSV(record);
        }
        return sv;
    }

    public IRecord readRecord() throws IOException {
        if (reader.read(record)) {
            return record;
        } else {
            return null;
        }
    }

    public long tell() {
        return this.reader.tell();
    }

    public ISDSV read(SDFFilter filter) throws IOException {
        record = record == null ? reader.getRecord() : record;
        boolean read = reader.read(record);
        if (read) {
            IRecord filteredRecord = filter.filter(record);
            if (filteredRecord == null) {
                return null;
            }
            return parseToSV(record);
        }
        return null;
    }

    public ISDSV read(SVFilterManager filterManager) throws IOException {
        ISDSV sv = read();
        if (filterManager != null && filterManager.filterSV()) {
            boolean filter = filterManager.getSVLevelFilterManager().filter(sv);
            return filter ? sv : null;
        }
        return sv;
    }

    public boolean seek(long variantIndex) throws IOException {
        boolean var3;
        var3 = this.reader.seek(variantIndex);
        return var3;
    }

    public SDFReader limit(LongInterval ranges) throws IOException {
        this.reader.limit(ranges);
        return this;
    }

    public SDFReader limit(long minPointer, long maxPointer) throws IOException {
        this.reader.limit(minPointer, maxPointer);
        return this;
    }


    public long remaining() {
        return this.reader.remaining();
    }

    public LinkedSet<String> getIndividuals() {
        return option.getSDFTable().individuals;
    }

    public int numOfIndividuals() {
        return this.getIndividuals().size();
    }

    public Iterator<ISDSV> iterator() {
        return new Iterator<ISDSV>() {
            public boolean hasNext() {
                return SDFReader.this.remaining() > 0L;
            }

            public ISDSV next() {
                try {
                    return SDFReader.this.read();
                } catch (IOException var2) {
                    throw new RuntimeException(var2);
                }
            }
        };
    }

    public void close() throws IOException {
        this.range = reader.available();
        this.lastPointer = reader.tell();
        this.record.clear();
        this.reader.close();
    }

    public boolean isClosed() {
        return this.reader.isClosed();
    }

    public List<SDFReader> part(int nParts) throws IOException {
        if (this.reader.isClosed()) {
            throw new IOException("IO Stream closed" );
        } else {
            try {
                List marks;
                List var11;
                if (this.remaining() <= 1L) {
                    marks = new List(1);
                    SDFReader reader = new SDFReader(this.option);
                    reader.limit(this.reader.available());
                    reader.seek(this.reader.tell());
                    marks.add(reader);
                    var11 = marks;
                    return var11;
                } else {
                    nParts = ValueUtils.valueOf(nParts, 1, 128);
                    marks = this.reader.available().getOverlaps(new LongInterval(this.reader.tell(), Long.MAX_VALUE)).divide(nParts, false);
                    List<SDFReader> readers = new List<>(marks.size());

                    for (Object o : marks) {
                        LongInterval mark = (LongInterval) o;
                        SDFReader reader = new SDFReader(this.option);
                        reader.limit(mark);
                        reader.seek(this.reader.tell());
                        readers.add(reader);
                    }

                    var11 = readers;
                    return var11;
                }
            } finally {
                this.close();
            }
        }
    }

    public String getContigByIndex(int index) {
        return option.getSDFTable().contig.getContigNameByIndex(index);
    }

    public void open() throws IOException {
        if (reader.isClosed()) {
            HashSet<CCFChunk.Type> types = new HashSet<>();
            types.add(CCFChunk.Type.FIELD_GROUP_META);
            types.add(CCFChunk.Type.FIELD_GROUP_DATA);
            CCFTable table = new CCFTable(
                    reader.getReaderOption().getFile(),
                    new CCFLoader(types)
            );
            reader = new CCFReader(
                    new ReaderOption(
                            table, option.getMandatoryFields().asUnmodifiable()
                    )
            );
            record = record == null ? reader.getRecord() : record;
        }
    }

    public void openLast() throws IOException {
        if (reader.isClosed()) {
            reader = new CCFReader(new ReaderOption(new CCFTable(reader.getReaderOption().getFile(), SDFReaderOption.DATA_LOADER)));
            reader.seek(lastPointer);
        }
    }

    public void openLastWithLimit() throws IOException {
        if (reader.isClosed()) {
            CCFTable table = new CCFTable(
                    option.getFile(),
                    SDFReaderOption.DATA_LOADER
            );
            reader = new CCFReader(
                    new ReaderOption(
                            table, option.getMandatoryFields().asUnmodifiable()
                    )
            );
            reader = reader.limit(range);
            reader.seek(lastPointer);
        }
    }

    public void reopenAllFields() throws IOException {
        if (!reader.isClosed()) {
            reader.seek(0);
            return;
        }
        reader.close();
        CCFTable.gc();
        reader = new CCFReader(
                new ReaderOption(
                        // TODO: whether can simplify
                        new CCFTable(reader.getReaderOption().getFile(), SDFReaderOption.DATA_LOADER),
                        SDFReadType.FULL.getReaderMode().getMandatoryFields()
                )
        );
        record = reader.getRecord();
        reader.seek(0);
    }

    public FieldGroupMetas getRawFields() {
        return (FieldGroupMetas) reader.getAllFields();
    }

    public SDFReader limit(String nameOfContig) throws IOException {
        IntInterval rangeByName = option.getSDFTable().contig.getRangeByName(nameOfContig);
        if (rangeByName == null) {
            return null;
        }
        if (reader.isClosed()) {
            reader = new CCFReader(reader.getReaderOption());
        }
        long l = rangeByName.end() - rangeByName.start();
        if (l == 0) {
            return null;
        }
        reader.limit(rangeByName.start(), rangeByName.end());
        return this;
    }

    public List<String> getValidContigNames() {
        List<String> res = new List<>();
        SVContig contig = option.getSDFTable().getContig();
        DynamicIndexableMap<String, IntInterval> contigRanges = contig.getContigRanges();
        int size = contigRanges.size();
        for (int i = 0; i < size; i++) {
            IntInterval range = contigRanges.getByIndex(i);
            if ((range.end() - range.start()) != 0) {
                res.add(contigRanges.keyOfIndex(i));
            }
        }
        return res;
    }

    public CCFReader getReader() {
        return reader;
    }

    public SDSVConversionManager getConversion() {
        return conversion;
    }

    public LiveFile getFile() {
        return reader.getReaderOption().getFile();
    }


    public void closeAll() throws IOException {
        reader.close();
        reader = null;
        option.clear();
        record = null;
    }

    public SDFFormatManager getFormatManager() {
        return option.getSDFTable().getFormatManager();
    }

    public SDFInfoManager getInfoManager() {
        return option.getSDFTable().getInfoManager();
    }

}
