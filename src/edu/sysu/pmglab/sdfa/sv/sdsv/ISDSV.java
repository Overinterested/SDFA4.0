package edu.sysu.pmglab.sdfa.sv.sdsv;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.FixedIndexableMap;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.sdfa.sv.*;

/**
 * @author Wenjie Peng
 * @create 2024-09-04 02:37
 * @description an interface for detecting a standardized composition SV
 */
public interface ISDSV {
    String[] ATTRIBUTES = new String[]{ // LOCATION
            "contig", "pos", "end", "length", "type",
            // GENOTYPE
            "genotype", "metrics",
            // VCF_FIELD
            "id", "ref", "alt", "qual", "filter", "info",
            // CSV_INDEX
            "line", "chr"};

    LinkedSet<String> FIX_ATTRIBUTE = new LinkedSet<>(ATTRIBUTES);

    CSVLocation EMPTY_CSVLOCATION = new CSVLocation(-1);

    default int getFileID() {
        return -1;
    }

    default int getContigIndex() {
        return -1;
    }

    default int getPos() {
        return -1;
    }

    default int getEnd() {
        return -1;
    }

    default int indexInFile() {
        return -1;
    }

    default boolean spanContig() {
        return false;
    }

    default boolean isComplexType() {
        return getType().isComplex();
    }

    default int length() {
        return -1;
    }

    default Bytes getID() {
        return null;
    }

    default SVTypeSign getType() {
        return null;
    }

    default Bytes getRef() {
        return null;
    }

    default Bytes getAlt() {
        return null;
    }

    default Bytes getFilter() {
        return null;
    }

    default Bytes getQuality() {
        return null;
    }

    default SVGenotypes getSVGenotypes() {
        return null;
    }

    default Bytes getItemInInfoByIndex(int index) {
        return null;
    }

    default Bytes getPropertyByName(Bytes key) {
        return null;
    }

    default Bytes getPropertyByIndex(int index) {
        return null;
    }

    default ISDSV setFileID(int fileID) {
        return this;
    }

    default ISDSV setLength(int length) {
        return this;
    }

    default ISDSV setType(SVTypeSign type) {
        return this;
    }

    default ISDSV setID(Bytes ID) {
        return this;
    }

    default ISDSV setRef(Bytes ref) {
        return this;
    }

    default ISDSV setAlt(Bytes alt) {
        return this;
    }

    default ISDSV setFilter(Bytes filter) {
        return this;
    }

    default ISDSV setQuality(Bytes quality) {
        return this;
    }

    default ISDSV setGenotypes(SVGenotypes genotypes) {
        return this;
    }

    default ISDSV setCsvLocation(CSVLocation csvLocation) {
        return this;
    }

    default ISDSV setCoordinate(SVCoordinate coordinate) {
        return this;
    }

    default ISDSV setInfo(DynamicIndexableMap<Bytes, Bytes> info) {
        return this;
    }

    default ISDSV setProperties(FixedIndexableMap<Bytes, Bytes> properties) {
        return this;
    }

    default SVCoordinate getCoordinate() {
        return null;
    }

    default String getNameOfType() {
        return null;
    }

    default Bytes toVCFRecord(ByteStream cache) {
        return null;
    }

    /**
     * convert record to an instance record
     *
     * @param record            raw
     * @param conversionManager manager conversion function
     */
    void parseRecord(IRecord record, SDSVConversionManager conversionManager);

    default int numOfSubSVs() {
        return 1;
    }

    default CSVLocation getCsvLocation() {
        return EMPTY_CSVLOCATION;
    }

    default IntList getAnnotationIndexes() {
        return null;
    }

    default IntInterval getCoordinateInterval() {
        return null;
    }

    void writeTo(ByteStream cache);

    ISDSV setChrName(String contigName);

    default void updateAnnotPointer(int indexOfSource, int startPointer, int endPointer) {

    }

    default boolean existAnnot() {
        return false;
    }

    default ISDSV setFileIndex(int fileIndex) {
        return this;
    }

    default String nameOfContig() {
        return null;
    }

    default IRecord toRecord(IRecord record, int indexInFile) {
        return record;
    }

    default DynamicIndexableMap<Bytes, Bytes> getInfo() {
        return null;
    }

    default Object get(String attr) {
        if (FIX_ATTRIBUTE.contains(attr)) {
            switch (attr) {
                case "contig":
                    return nameOfContig();
                case "pos":
                    return getPos();
                case "end":
                    return getEnd();
                case "length":
                    return length();
                case "type":
                    return getType();
                case "genotype":
                    SVGenotypes genotypes = getSVGenotypes();
                    if (genotypes != null) {
                        return genotypes.getGtyBox();
                    }
                    return null;
//                case "metrics":
//                    SVGenotypes genotypes1 = getGenotypes();
//                    if (genotypes1 != null) {
//                        return genotypes1.getEncodeFieldValueSet();
//                    }
//                    return null;
                case "id":
                    return getID();
                case "ref":
                    return getRef();
                case "alt":
                    return getAlt();
                case "qual":
                    return getQuality();
                case "filter":
                    return getFilter();
                case "info":
                    return getInfo();
                case "line":
                    return indexInFile();
                case "chr":
                    CSVLocation csvLocation = getCsvLocation();
                    if (csvLocation != null) {
                        return csvLocation.getIndexesOfContig();
                    }
                    return null;
                default:
                    return getInfo(attr);
            }
        }
        DynamicIndexableMap<Bytes, Bytes> info = getInfo();
        if (info == null) {
            return null;
        }
        return info.get(new Bytes(attr));
    }

    default Object get(Bytes attr) {
        return get(attr.toString());
    }

    default Object getInfo(String infoItem) {
        DynamicIndexableMap<Bytes, Bytes> info = getInfo();
        if (info == null) {
            return null;
        }
        return info.get(new Bytes(infoItem));
    }

}
