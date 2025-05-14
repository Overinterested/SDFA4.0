package edu.sysu.pmglab.sdfa.sv.csv;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.*;
import edu.sysu.pmglab.sdfa.sv.sdsv.CompleteSDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

/**
 * @author Wenjie Peng
 * @create 2024-10-17 01:00
 * @description
 */
public class SharedFieldComplexSV implements IComplexSV, ISDSV {
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
    IntList annotationIndexes;
    List<SVCoordinate> coordinates;
    DynamicIndexableMap<Bytes, Bytes> info;
    List<CompleteSDSV> wrappedSDSVList = new List<>();

    @Override
    public int compareTo(IComplexSV o) {
        int comparedSize = Math.min(coordinates.size(), o.sizeOfCoordinates());
        for (int i = 0; i < comparedSize; i++) {
            int status = coordinates.fastGet(i).compareTo(o.getCoordinate(i));
            if (status != 0) {
                return status;
            }
        }
        return Integer.compare(coordinates.size(), o.sizeOfCoordinates());
    }

    @Override
    public int sizeOfCoordinates() {
        return coordinates.size();
    }

    @Override
    public SVCoordinate getCoordinate(int index) {
        return coordinates.fastGet(index);
    }

    public SharedFieldComplexSV setFileID(int fileID) {
        this.fileID = fileID;
        return this;
    }

    public SharedFieldComplexSV setLength(int length) {
        if (length < -1) {
            this.length = -length;
        } else {
            this.length = length;
        }
        return this;
    }

    public SharedFieldComplexSV setID(Bytes ID) {
        this.ID = ID;
        return this;
    }

    public SharedFieldComplexSV setRef(Bytes ref) {
        this.ref = ref;
        return this;
    }

    public SharedFieldComplexSV setAlt(Bytes alt) {
        this.alt = alt;
        return this;
    }

    public SharedFieldComplexSV setType(SVTypeSign type) {
        this.type = type;
        return this;
    }

    public SharedFieldComplexSV setFilter(Bytes filter) {
        this.filter = filter;
        return this;
    }

    public SharedFieldComplexSV setQuality(Bytes quality) {
        this.quality = quality;
        return this;
    }

    public SharedFieldComplexSV setGenotypes(SVGenotypes genotypes) {
        this.genotypes = genotypes;
        return this;
    }

    public SharedFieldComplexSV setCsvLocation(CSVLocation csvLocation) {
        this.csvLocation = csvLocation;
        return this;
    }

    @Override
    public void parseRecord(IRecord record, SDSVConversionManager conversionManager) {
        return;
    }


    public SharedFieldComplexSV setAnnotationIndexes(IntList annotationIndexes) {
        this.annotationIndexes = annotationIndexes;
        return this;
    }

    public SharedFieldComplexSV setInfo(DynamicIndexableMap<Bytes, Bytes> info) {
        this.info = info;
        return this;
    }

    public SharedFieldComplexSV setCoordinates(List<SVCoordinate> coordinates) {
        this.coordinates = coordinates;
        return this;
    }

    public List<CompleteSDSV> wrap() {
        wrappedSDSVList.clear();
        for (int i = 0; i < coordinates.size(); i++) {
            wrappedSDSVList.add(
                    new CompleteSDSV().setCoordinate(coordinates.fastGet(i))
                            .setID(ID)
                            .setAlt(alt)
                            .setRef(ref)
                            .setFilter(filter)
                            .setQuality(quality)
                            .setLength(length)
                            .setInfo(info)
                            .setGenotypes(genotypes)
                            .setFileID(fileID)
                            .setType(type)
            );
        }
        return wrappedSDSVList;
    }

    public int getFileID() {
        return fileID;
    }

    public int getLength() {
        return length;
    }

    public Bytes getID() {
        return ID;
    }

    public Bytes getRef() {
        return ref;
    }

    public Bytes getAlt() {
        return alt;
    }

    public SVTypeSign getType() {
        return type;
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

    public CSVLocation getCsvLocation() {
        return csvLocation;
    }

    public IntList getAnnotationIndexes() {
        return annotationIndexes;
    }

    @Override
    public void writeTo(ByteStream cache) {

    }

    @Override
    public ISDSV setChrName(String contigName) {
        return null;
    }

    public List<SVCoordinate> getCoordinates() {
        return coordinates;
    }

    public DynamicIndexableMap<Bytes, Bytes> getInfo() {
        return info;
    }

    public List<CompleteSDSV> getWrappedSDSVList() {
        return wrappedSDSVList;
    }
}
