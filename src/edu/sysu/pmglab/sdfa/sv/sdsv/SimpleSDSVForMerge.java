package edu.sysu.pmglab.sdfa.sv.sdsv;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.sdfa.sv.*;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;

/**
 * @author Wenjie Peng
 * @create 2024-09-04 07:08
 * @description
 */
public class SimpleSDSVForMerge extends SimpleSDSV {
    int fileIndex;
    SVGenotypes svGenotypes;

    @Override
    public void parseRecord(IRecord record, SDSVConversionManager conversionManager) {
        this.coordinate = SVCoordinate.decode(record.get(0));
        this.length = record.get(1);
        this.svTypeSign = SVTypeSign.getByIndex(record.get(2));
        this.svGenotypes = new SVGenotypes(new GTBox((Bytes) record.get(3)));
        this.csvLocation = new CSVLocation(record.get(4), (IntList) record.get(5));
    }

    public CSVLocation getCsvLocation() {
        return csvLocation;
    }

    @Override
    public SimpleSDSVForMerge setCsvLocation(CSVLocation csvLocation) {
        this.csvLocation = csvLocation;
        return this;
    }

    public SimpleSDSVForMerge setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
        return this;
    }

    @Override
    public int getFileID() {
        return fileIndex;
    }


    @Override
    public SVGenotypes getSVGenotypes() {
        return svGenotypes;
    }
}
