package edu.sysu.pmglab.sdfa.sv.sdsv;

import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.sdfa.annotation.source.Source;
import edu.sysu.pmglab.sdfa.sv.CSVLocation;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;

/**
 * @author Wenjie Peng
 * @create 2024-09-04 06:49
 * @description
 */
public class SimpleSDSVForAnnotation extends SimpleSDSV {
    protected boolean existAnnot = false;
    protected IntList indexablePointerRange;

    public SimpleSDSVForAnnotation() {
        super();
    }

    @Override
    public void parseRecord(IRecord record, SDSVConversionManager conversionManager) {
        this.coordinate = SVCoordinate.decode(record.get(0));
        this.length = record.get(1);
        this.svTypeSign = SVTypeSign.getByIndex(record.get(2));
        this.csvLocation = new CSVLocation(record.get(3), (IntList) record.get(4));
        this.indexablePointerRange = record.get(5);
    }

    public void init(int numOfResource) {
        int sourcePointers = 2 * numOfResource;
        indexablePointerRange = new IntList(sourcePointers);
        indexablePointerRange.fill(-1, sourcePointers);
    }

    public void addAnnotationIndexes(int indexOfResource, IntList indexes) {
        if (indexes == Source.NULL_ANNOTATION || indexes == null || indexes.isEmpty()) {
            indexablePointerRange.set(2 * indexOfResource, -1);
            indexablePointerRange.set(2 * indexOfResource + 1, -1);
            return;
        }
        int minPointer = Integer.MAX_VALUE;
        int maxPointer = Integer.MIN_VALUE;
        for (int i = 0; i < indexes.size(); i++) {
            int pointer = indexes.fastGet(i);
            if (pointer < minPointer) {
                minPointer = pointer;
            }
            if (pointer > maxPointer) {
                maxPointer = pointer;
            }
        }
        indexablePointerRange.set(2 * indexOfResource, minPointer);
        indexablePointerRange.set(2 * indexOfResource + 1, maxPointer);
        indexes.clear();
    }


    public IntInterval getCoordinateInterval() {
        int pos = coordinate.getPos();
        int end = coordinate.getEnd();
        return new IntInterval(pos, end == -1 || end == pos ? pos + 1 : end);
    }

    public IntList getAnnotationIndexes() {
        return indexablePointerRange;
    }

    public void updateAnnotPointer(int indexOfSource, int startPointer, int endPointer) {
        existAnnot = true;
        indexablePointerRange.set(2 * indexOfSource, startPointer);
        indexablePointerRange.set(2 * indexOfSource + 1, endPointer);
    }

    public boolean existAnnot(){
        return existAnnot;
    }

}
