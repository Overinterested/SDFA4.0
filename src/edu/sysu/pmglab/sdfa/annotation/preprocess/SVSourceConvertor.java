package edu.sysu.pmglab.sdfa.annotation.preprocess;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.ReaderOption;
import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.field.FieldMeta;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.toolkit.Sorter;
import edu.sysu.pmglab.ccf.toolkit.output.CCFOutputOption;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.annotation.source.SVSource;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;

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
public class SVSourceConvertor implements SourceConvertor {
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
        for (int i = 0; i < 4; i++) {
            int status = Integer.compare(o1.get(i), o2.get(i));
            if (status != 0) {
                return status;
            }
        }
        return 0;
    };

    private SVSourceConvertor(LiveFile file, File outputDir) {
        this.file = file;
        this.outputDir = outputDir;
        this.contig = SVContig.init();
    }

    public static SVSourceConvertor of(LiveFile file, File outputDir) {
        return new SVSourceConvertor(file, outputDir);
    }

    @Override
    public SVSource convert() throws IOException {
        if (file.getName().endsWith(".ccf")) {
            // converted file
            CCFReader reader = new CCFReader(file);
            meta = SourceMeta.load(reader.getTable().getMeta());
            reader.close();
            return new SVSource(file, meta);
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
        Iterator<Bytes> tmpIterator = cache.toBytes().detach().split(Constant.TAB);
        while (tmpIterator.hasNext()) columns.add(tmpIterator.next().detach());

        this.columns = new LinkedSet<>(columns.apply(Bytes::toString).toArray(new String[0]));
        if (this.columns.size() < 5) {
            throw new UnsupportedEncodingException("SV Source needs 5 columns at least");
        }
        cache.clear();
        List<IRecord> records = new List<>();
        File outputFile = FileUtils.getSubFile(outputDir, file.getName() + ".ccf");
        CCFWriter writer = CCFWriter.setOutput(outputFile).addFields(new FieldGroupMetas(buildFieldMetas())).instance();
        IRecord temRecord = writer.getRecord();
        int columnSize = columns.size();
        int indexOfContig = -1, pos, end, len, type;
        readerStream.readline(cache);
        int count;
        do {
            count = 0;
            IRecord item = temRecord.clone();
            Iterator<Bytes> iterator = cache.toBytes().detach().split(Constant.TAB);
            while (iterator.hasNext()) {
                switch (count++) {
                    case 0:
                        item.set(0, indexOfContig = contig.getContigIndexByName(iterator.next().toString()));
                        break;
                    case 1:
                        try {
                            item.set(1, iterator.next().toInt());
                        } catch (NumberFormatException e) {
                            throw new UnsupportedEncodingException("The first 5 columns of " + indexOfFile + "'th line can't be parsed to integer");
                        }
                        break;
                    case 2:
                        try {
                            item.set(2, iterator.next().toInt());
                        } catch (NumberFormatException e) {
                            throw new UnsupportedEncodingException("The first 5 columns of " + indexOfFile + "'th line can't be parsed to integer");
                        }
                        break;
                    case 3:
                        try {
                            item.set(3, iterator.next().toInt());
                        } catch (NumberFormatException e) {
                            throw new UnsupportedEncodingException("The first 5 columns of " + indexOfFile + "'th line can't be parsed to integer");
                        }
                        break;
                    case 4:
                        item.set(4, SVTypeSign.getByName(iterator.next()).getIndex());
                        break;
                    default:
                        item.set(count, iterator.next());
                        break;
                }
            }
            cache.clear();
            writer.write(item);
            contig.countContigByIndex(indexOfContig);
            indexOfFile++;
        } while (readerStream.readline(cache) != -1);
        // write data
        comparator = comparator == null ? defaultComparator : comparator;
        records.sort(comparator);
        readerStream.close();
        this.meta = new SourceMeta(contig.getContigRanges()).setColumns(new LinkedSet<>(this.columns));
        writer.addMeta(meta.save());
        writer.close();


        Sorter.SorterSetting<ReaderOption, Integer, SVCoordinate> sortProcess =
                Sorter.setInput(writer.getFile())
                        .getTagFrom(record -> (int) record.get(0))
                        .getValueFrom(
                                record -> SVCoordinate.decode(IntList.wrap((int) record.get(0), (int) record.get(1), (int) record.get(2)))
                        )
                        .projectValue(coordinate->coordinate.getPos());

        boolean ordered = sortProcess.isOrdered(1);

        if (!ordered) {
            sortProcess.bucketSort(new CCFOutputOption(writer.getFile()), 1);
        }
        return new SVSource(LiveFile.of(outputFile), meta);
    }

    public SVSourceConvertor setDefinedRecordConversion
            (Function<List<Bytes>, IRecord> definedRecordConversion) {
        this.definedRecordConversion = definedRecordConversion;
        return this;
    }

    private List<FieldMeta> buildFieldMetas() {
        List<FieldMeta> fields = new List<>(columns.size());
        fields.addAll(new FieldMeta[]{
                // chr
                FieldMeta.of(this.columns.valueOf(0), FieldType.varInt32),
                // pos
                FieldMeta.of(this.columns.valueOf(1), FieldType.varInt32),
                // end
                FieldMeta.of(this.columns.valueOf(2), FieldType.varInt32),
                // length
                FieldMeta.of(this.columns.valueOf(3), FieldType.varInt32),
                // type
                FieldMeta.of(this.columns.valueOf(4), FieldType.varInt32),
        });
        for (int i = 5; i < this.columns.size(); i++) {
            fields.add(FieldMeta.of(this.columns.valueOf(i), FieldType.bytecode));
        }
        return fields;
    }

    public SVSourceConvertor setComparator(Comparator<IRecord> comparator) {
        this.comparator = comparator;
        return this;
    }

    public SVSourceConvertor storeHeader(boolean storeHeader) {
        this.storeHeader = storeHeader;
        return this;
    }
}
