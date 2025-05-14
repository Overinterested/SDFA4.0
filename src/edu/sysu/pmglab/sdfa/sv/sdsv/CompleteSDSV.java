package edu.sysu.pmglab.sdfa.sv.sdsv;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.FixedIndexableMap;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.sv.*;

/**
 * @author Wenjie Peng
 * @create 2024-08-25 20:21
 * @description standardized decomposition SV
 */
public class CompleteSDSV implements ISDSV {
    int fileID;
    int length;
    Bytes ID;
    Bytes ref;
    Bytes alt;
    SVTypeSign type;
    Bytes filter;
    Bytes quality;
    SVGenotypes genotypes;
    CSVLocation csvLocation;
    SVCoordinate coordinate;
    IntList annotationIndexes;
    DynamicIndexableMap<Bytes, Bytes> info;
    FixedIndexableMap<Bytes, Bytes> properties;
    private static ByteStream cacheForInfoPresent = new ByteStream();

    public int getFileID() {
        return fileID;
    }

    public int getPos() {
        return coordinate.getPos();
    }

    public int getEnd() {
        return coordinate.getEnd();
    }

    public int indexInFile() {
        return csvLocation.indexInFile();
    }

    public boolean spanContig() {
        return type.spanContig();
    }

    public int length() {
        return length;
    }

    public Bytes getID() {
        return ID;
    }

    public SVTypeSign getType() {
        return type;
    }

    public Bytes getRef() {
        return ref;
    }

    public Bytes getAlt() {
        return alt;
    }

    public Bytes getFilter() {
        return filter;
    }

    public Bytes getQuality() {
        return quality;
    }

    public SVGenotypes getSVGenotypes() {
        return genotypes;
    }

    public Bytes getItemInInfoByIndex(int index) {
        return info.get(index);
    }

    public Bytes getPropertyByName(Bytes key) {
        return properties.get(key);
    }

    public Bytes getPropertyByIndex(int index) {
        return properties.getByIndex(index);
    }

    public CompleteSDSV setFileID(int fileID) {
        this.fileID = fileID;
        return this;
    }

    public CompleteSDSV setLength(int length) {
        this.length = length;
        return this;
    }

    public CompleteSDSV setType(SVTypeSign type) {
        this.type = type;
        return this;
    }

    public CompleteSDSV setID(Bytes ID) {
        this.ID = ID;
        return this;
    }

    public CompleteSDSV setRef(Bytes ref) {
        this.ref = ref;
        return this;
    }

    public CompleteSDSV setAlt(Bytes alt) {
        this.alt = alt;
        return this;
    }

    public CompleteSDSV setFilter(Bytes filter) {
        this.filter = filter;
        return this;
    }

    public CompleteSDSV setQuality(Bytes quality) {
        this.quality = quality;
        return this;
    }

    public CompleteSDSV setGenotypes(SVGenotypes genotypes) {
        this.genotypes = genotypes;
        return this;
    }

    public CompleteSDSV setCsvLocation(CSVLocation csvLocation) {
        this.csvLocation = csvLocation;
        return this;
    }

    public CompleteSDSV setCoordinate(SVCoordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }

    public CompleteSDSV setInfo(DynamicIndexableMap<Bytes, Bytes> info) {
        this.info = info;
        return this;
    }

    public CompleteSDSV setProperties(FixedIndexableMap<Bytes, Bytes> properties) {
        this.properties = properties;
        return this;
    }

    public SVCoordinate getCoordinate() {
        return coordinate;
    }

    public String getNameOfType() {
        return type.getName();
    }

    public Bytes toVCFRecord(ByteStream cache) {
        cache.write(ASCIIUtility.toASCII(coordinate.getChr(), Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(ASCIIUtility.toASCII(coordinate.getPos()));
        cache.write(Constant.TAB);
        // ID
        if (this.ID == null) {
            cache.write(Constant.PERIOD);
        } else {
            cache.write(this.ID);
        }
        cache.write(Constant.TAB);
        // ref
        if (ref == null) {
            cache.write(Constant.N);
        } else {
            cache.write(ref);
        }
        cache.write(Constant.TAB);
        // alt
        if (alt == null) {
            cache.write(Constant.LESS_THAN_SIGN);
            cache.write(ASCIIUtility.toASCII(getNameOfType(), Constant.CHAR_SET));
            cache.write(Constant.GREATER_THAN_SIGN);
        } else {
            cache.write(alt);
        }
        cache.write(Constant.TAB);
        // quality
        if (quality == null) {
            cache.write(Constant.PERIOD);
        } else {
            cache.write(quality);
        }
        cache.write(Constant.TAB);
        // filter
        if (filter == null) {
            cache.write(Constant.PERIOD);
        } else {
            cache.write(filter);
        }
        cache.write(Constant.TAB);

        // INFO
        if (info == null || info.isEmpty()) {
            cache.write(Constant.PERIOD);
            cache.write(Constant.TAB);
        } else {
            int size = info.size();
            for (int i = 0; i < size; i++) {
                Bytes value = info.getByIndex(i);
                if (value == null) {
                    continue;
                }
                cache.write(info.keyOfIndex(i));
                cache.write(Constant.EQUAL);
                cache.write(info.getByIndex(i));
                cache.write(Constant.SEMICOLON);
            }
            cache.write(Constant.TAB);
        }
        // FORMAT
        cache.write(GenotypeConstant.GT);
        cache.write(Constant.TAB);
        // GENOTYPES
        if (genotypes == null || genotypes.numOfSamples() == 0) {
            cache.write(Constant.PERIOD);
        } else {
            int sizeOfSample = genotypes.numOfSamples();
            for (int i = 0; i < sizeOfSample; i++) {
                cache.write(ASCIIUtility.toASCII(genotypes.getGty(i).toString(), Constant.CHAR_SET));
                if (i != sizeOfSample - 1) {
                    cache.write(Constant.TAB);
                }
            }
        }
        Bytes res = cache.toBytes().detach();
        cache.clear();
        return res;
    }

    @Override
    public void parseRecord(IRecord record, SDSVConversionManager conversionManager) {
        this.coordinate = SVCoordinate.decode(record.get(0));
        this.coordinate.setChr(conversionManager.getContigName(coordinate.getIndexOfChr()));
        this.length = record.get(1);
        this.type = SVTypeSign.getByIndex(record.get(2));
        this.genotypes = new SVGenotypes((Bytes) record.get(3));
        this.genotypes.setEncodedAttrs(record.get(4));
        this.ID = conversionManager.getIdAttributeBox().decode(record.get(5)).get();
        this.ref = conversionManager.getRefAttributeBox().decode(record.get(6)).get();
        this.alt = conversionManager.getAltAttributeBox().decode(record.get(7)).get();
        this.quality = conversionManager.getQualAttributeBox().decode(record.get(8)).get();
        this.filter = conversionManager.getFilterAttributeBox().decode(record.get(9)).get();
        this.info = new DynamicIndexableMap<>(conversionManager.getFixedInfoKeys());
        List<Bytes> infoItem = record.get(10);
        for (int i = 0; i < infoItem.size(); i++) {
            info.putByIndex(i, infoItem.fastGet(i));
        }
        this.csvLocation = new CSVLocation(record.get(11), (IntList) record.get(12));
        this.annotationIndexes = record.get(13);
    }

    public int getLength() {
        return length;
    }

    public CSVLocation getCsvLocation() {
        return csvLocation;
    }

    public DynamicIndexableMap<Bytes, Bytes> getInfo() {
        return info;
    }

    public IntList getAnnotationIndexes() {
        return annotationIndexes;
    }

    @Override
    public int getContigIndex() {
        return coordinate.getIndexOfChr();
    }

    @Override
    public IntInterval getCoordinateInterval() {
        return coordinate.getCoordinateInterval();
    }

    @Override
    public String toString() {
        return "CompleteSDSV{" +
                "fileID=" + fileID +
                ", length=" + length +
                ", ID=" + ID +
                ", ref=" + ref +
                ", alt=" + alt +
                ", type=" + type +
                ", filter=" + filter +
                ", quality=" + quality +
                ", genotypes=" + genotypes +
                ", csvLocation=" + csvLocation +
                ", coordinate=" + coordinate +
                ", itemsInInfo=" + info +
                ", annotationIndexes=" + annotationIndexes +
                ", properties=" + properties +
                '}';
    }

    @Override
    public void writeTo(ByteStream cache) {
        cache.write(ASCIIUtility.toASCII(toString(), Constant.CHAR_SET));
    }

    @Override
    public ISDSV setChrName(String contigName) {
        if (coordinate != null) {
            coordinate.setChr(contigName);
        } else {
            coordinate = new SVCoordinate(-1, -1, contigName);
        }
        return this;
    }

    public Object[] toGuiObject(long index) {
        Object[] returns = new Object[13];
        returns[0] = index - 1;
        returns[1] = coordinate.toString();
        returns[2] = length <= 0 ? Constant.BYTES_PERIOD : length;
        returns[3] = type.getName();
        returns[4] = genotypes.getGtyBox().get().counter().toString();
//        returns[5] = "Encoded Binary";
        returns[5] = ID;
        returns[6] = ref;
        returns[7] = alt;
        returns[8] = quality;
        returns[9] = filter;
        returns[10] = presentInfo();
//        returns[11] = indexInFile();
        IntList indexesOfContig = csvLocation.getIndexesOfContig();
        returns[11] = indexesOfContig == null || indexesOfContig.isEmpty() ? Constant.BYTES_PERIOD : indexesOfContig;
        returns[12] = annotationIndexes == null || annotationIndexes.isEmpty() ? Constant.BYTES_PERIOD : annotationIndexes;
        return returns;
    }

    public String presentInfo() {
        cacheForInfoPresent.clear();
        int size = info.size();
        for (int i = 0; i < size; i++) {
            Bytes value = info.getByIndex(i);
            if (value != null) {
                cacheForInfoPresent.write(info.keyOfIndex(i));
                cacheForInfoPresent.write(Constant.EQUAL);
                cacheForInfoPresent.write(value);
                cacheForInfoPresent.write(Constant.SEMICOLON);
            }
        }
        cacheForInfoPresent.write("Others=NULL;".getBytes());
        return cacheForInfoPresent.toBytes().toString();
    }

    public CompleteSDSV setInfoItemByIndex(int index, Bytes value) {
        info.putByIndex(index, value);
        return this;
    }

    public CompleteSDSV setPos(int pos) {
        if (coordinate == null) {
            coordinate = new SVCoordinate(-1, -1, null);
        }
        coordinate.setPos(pos);
        return this;
    }

    public CompleteSDSV setEnd(int end) {
        if (coordinate == null) {
            coordinate = new SVCoordinate(-1, -1, null);
        }
        coordinate.setEnd(end);
        return this;
    }

    public CompleteSDSV setChrIndex(int chrIndex) {
        if (coordinate != null) {
            coordinate.setIndexOfChr(chrIndex);
        } else {
            coordinate = new SVCoordinate(-1, -1, chrIndex);
        }
        return this;
    }


}