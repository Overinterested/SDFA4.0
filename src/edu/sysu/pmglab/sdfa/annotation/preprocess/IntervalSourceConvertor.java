package edu.sysu.pmglab.sdfa.annotation.preprocess;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.field.FieldMeta;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.annotation.source.IntervalSource;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.sv.SVContig;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 20:27
 * @description
 */
class IntervalSourceConvertor implements SourceConvertor {
    SourceMeta meta;
    SVContig contig;
    final LiveFile file;
    boolean storeHeader;
    final File outputDir;
    IndexableSet<String> columns;
    Comparator<IRecord> comparator;
    private List<Bytes> header = new List<>();
    Function<List<Bytes>, IRecord> definedRecordConversion;

    private static final Comparator<IRecord> defaultComparator = (o1, o2) -> {
        int status = Integer.compare(o1.get(0), o2.get(0));
        if (status == 0) {
            status = Integer.compare(o1.get(1), o2.get(1));
            if (status == 0) {
                status = Integer.compare(o1.get(2), o2.get(2));
            }
            return status;
        }
        return status;
    };

    private IntervalSourceConvertor(LiveFile file, File outputDir) {
        this.file = file;
        this.outputDir = outputDir;
        this.contig = SVContig.init();
    }

    public static IntervalSourceConvertor of(LiveFile file, File outputDir) {
        return new IntervalSourceConvertor(file, outputDir);
    }

    @Override
    public IntervalSource convert() throws IOException {
        if (file.getName().endsWith(".ccf")) {
            // converted file
            CCFReader reader = new CCFReader(file);
            meta = SourceMeta.load(reader.getTable().getMeta());
            reader.close();
            return new IntervalSource(file, meta);
        }
        // initial
        int indexOfFile = 1;
        boolean containHeader = false;
        ByteStream cache = new ByteStream();
        ReaderStream readerStream = file.openAsText();
        while (readerStream.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                if (line.byteAt(1) == Constant.NUMBER_SIGN) {
                    // header
                    if (storeHeader) {
                        header.add(cache.toBytes().detach());
                    }
                    indexOfFile++;
                    cache.clear();
                    continue;
                }
                // column
                containHeader = true;
                break;
            }
            break;
        }
        // column parse
        if (!containHeader) {
            throw new UnsupportedEncodingException(file.getName() + " has no column line");
        }

        List<Bytes> columns = new List<>();
        Iterator<Bytes> iterator = cache.toBytes().detach().split(Constant.TAB);
        while (iterator.hasNext()) columns.add(iterator.next().detach());

        this.columns = new LinkedSet<>(columns.apply(Bytes::toString).toArray(new String[0]));
        if (this.columns.size() <= 2) {
            throw new UnsupportedEncodingException("Interval Source needs 3 columns at least");
        }
        cache.clear();
        List<IRecord> records = new List<>();
        File outputFile = new File(outputDir.getPath() + File.separator + file.getName() + ".ccf");
        CCFWriter writer = CCFWriter.setOutput(outputFile).addFields(new FieldGroupMetas(buildFieldMetas())).instance();
        IRecord temRecord = writer.getRecord();
        int columnSize = columns.size();
        int indexOfContig, pos, end;
        readerStream.readline(cache);
        do {

            List<Bytes> line = new List<>();
            Iterator<Bytes> iterator1 = cache.toBytes().detach().split(Constant.TAB);
            while (iterator1.hasNext()) line.add(iterator1.next().detach());

            cache.clear();
            indexOfContig = contig.getContigIndexByName(line.get(0).toString());
            try {
                pos = line.get(1).toInt();
                end = line.get(2).toInt();
            } catch (NumberFormatException e) {
                throw new UnsupportedEncodingException("The coordinate of " + indexOfFile + "'th line can't be parsed to integer");
            }
            IRecord item = temRecord.clone();
            if (definedRecordConversion != null) {
                item = definedRecordConversion.apply(line);
            } else {
                // default transfer
                item.set(0, indexOfContig)
                        .set(1, pos)
                        .set(2, end);
                for (int i = 3; i < columnSize; i++) {
                    item.set(i, line.get(i));
                }
            }
            records.add(item);
            contig.countContigByIndex(indexOfContig);
            indexOfFile++;
        } while (readerStream.readline(cache) != -1);
        // write data
        comparator = comparator == null ? defaultComparator : comparator;
        records.sort(comparator);
        while (!records.isEmpty()) {
            IRecord record = records.popFirst();
            writer.write(record);
            record.clear();
        }
        this.meta = new SourceMeta(contig.getContigRanges()).setColumns(new LinkedSet<>(this.columns));
        writer.addMeta(meta.save());
        writer.close();
        return new IntervalSource(LiveFile.of(outputFile), meta);
    }

    public IntervalSourceConvertor setDefinedRecordConversion(Function<List<Bytes>, IRecord> definedRecordConversion) {
        this.definedRecordConversion = definedRecordConversion;
        return this;
    }

    private List<FieldMeta> buildFieldMetas() {
        List<FieldMeta> fields = new List<>(columns.size());
        fields.addAll(new FieldMeta[]{
                FieldMeta.of(this.columns.valueOf(0), FieldType.varInt32),
                FieldMeta.of(this.columns.valueOf(1), FieldType.varInt32),
                FieldMeta.of(this.columns.valueOf(2), FieldType.varInt32)
        });
        for (int i = 3; i < this.columns.size(); i++) {
            fields.add(FieldMeta.of(this.columns.valueOf(i), FieldType.bytecode));
        }
        return fields;
    }

    public IntervalSourceConvertor setComparator(Comparator<IRecord> comparator) {
        this.comparator = comparator;
        return this;
    }

    public IntervalSourceConvertor storeHeader(boolean storeHeader) {
        this.storeHeader = storeHeader;
        return this;
    }
}
