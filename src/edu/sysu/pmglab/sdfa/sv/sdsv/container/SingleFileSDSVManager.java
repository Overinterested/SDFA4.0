package edu.sysu.pmglab.sdfa.sv.sdsv.container;

import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.record.BoxRecord;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.SDFHeader;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.annotation.source.SourceManager;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.SimpleSDSVForAnnotation;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-08 20:11
 * @description
 */
public class SingleFileSDSVManager {
    int index;
    SDFReader reader;
    CCFWriter writer;
    File sdfFile;
    final LiveFile file;
    SDFReadType readerMode;
    LinkedSet<String> individuals;
    DynamicIndexableMap<String, List<ISDSV>> recordsOfContigs;

    public SingleFileSDSVManager(LiveFile file) {
        this.file = file;
        this.recordsOfContigs = new DynamicIndexableMap<>();
    }

    public void loadWithInit() throws IOException {
        ISDSV sv;
        getReader().open();
        int contigIndex = -1;
        List<ISDSV> svs = new List<>();
        String contigName;
        while ((sv = reader.read()) != null) {
            int tmpContigIndex = sv.getContigIndex();
            if (contigIndex != tmpContigIndex) {
                svs = new List<>();
                contigIndex = tmpContigIndex;
                contigName = reader.getContigByIndex(tmpContigIndex);
                recordsOfContigs.put(contigName, svs);
            }
            svs.add(sv);
            ((SimpleSDSVForAnnotation) sv).init(SourceManager.numOfSource());
        }
        reader.close();
    }

    public void load() throws IOException {
        int contigIndex = -1;
        reader.open();
        ISDSV sv;
        List<ISDSV> svs = new List<>();
        String contigName;
        while ((sv = reader.read()) != null) {
            int tmpContigIndex = sv.getContigIndex();
            if (contigIndex != tmpContigIndex) {
                contigIndex = tmpContigIndex;
                contigName = reader.getContigByIndex(tmpContigIndex);
                svs = new List<>();
                recordsOfContigs.put(contigName, svs);
            }
            svs.add(sv);
        }
        reader.close();
    }

    public void writeTo(File file) throws IOException {
        IRecord record;
        reader.reopenAllFields();
        BoxRecord tmpRecord = reader.getReader().getRecord();
        writer = CCFWriter.setOutput(file).addFields(SDFReadType.FULL.getReaderMode().getMandatoryFields()).instance();
        int index = tmpRecord.indexOf(SDFHeader.ANNOTATION_INDEX_GROUP.getMetas().getField(0));
        for (int i = 0; i < recordsOfContigs.size(); i++) {
            List<ISDSV> svs = recordsOfContigs.getByIndex(i);
            for (ISDSV sv : svs) {
                record = reader.readRecord();
                if (sv.existAnnot()) {
                    record.set(index, sv.getAnnotationIndexes());
                }
                writer.write(record);
            }
            svs.clear();
        }
        recordsOfContigs = null;
        writer.addMeta(reader.getReaderOption().getSDFTable().getMeta());
        reader.close();
        writer.close();
    }

    public void clear() {
        reader = null;
    }

    public SDFReader getReader() {
        if (reader == null) {
            try {
                reader = new SDFReader(sdfFile,readerMode);
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return reader;
    }

    public boolean needParse() {
        if (sdfFile == null) {
            if (file.getName().endsWith(".sdf")) {
                sdfFile = new File(file.getPath());
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public LiveFile getFile() {
        return file;
    }

    public SingleFileSDSVManager setIndex(int indexOfFile) {
        this.index = indexOfFile;
        return this;
    }

    public SingleFileSDSVManager setReader(SDFReader reader) {
        this.reader = reader;
        this.individuals = reader.getIndividuals();
        return this;
    }

    public DynamicIndexableMap<String, List<ISDSV>> getRecordsOfContigs() {
        return recordsOfContigs;
    }

    public List<ISDSV> getSVsByContig(String contigName) {
        return recordsOfContigs.get(contigName);
    }

    public LinkedSet<String> getIndividuals() {
        return individuals;
    }

    public File getSdfFile() {
        return sdfFile;
    }

    public SingleFileSDSVManager setSdfFile(File sdfFile) {
        this.sdfFile = sdfFile;
        return this;
    }

    public SingleFileSDSVManager setReaderMode(SDFReadType readerMode) {
        this.readerMode = readerMode;
        return this;
    }
}
