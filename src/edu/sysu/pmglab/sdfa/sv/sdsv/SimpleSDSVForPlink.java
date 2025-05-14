package edu.sysu.pmglab.sdfa.sv.sdsv;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.sdfa.sv.*;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;

/**
 * @author Wenjie Peng
 * @create 2024-09-04 07:15
 * @description
 */
public class SimpleSDSVForPlink extends SimpleSDSV {
    Bytes ID;
    Bytes ref;
    Bytes alt;
    SVGenotypes genotypes;

    @Override
    public void parseRecord(IRecord record, SDSVConversionManager conversionManager) {
        this.coordinate = SVCoordinate.decode(record.get(0));
        this.length = record.get(1);
        this.svTypeSign = SVTypeSign.getByIndex(record.get(2));
        this.genotypes = new SVGenotypes(new GTBox((Bytes) record.get(3)));
        this.ID = conversionManager.getIdAttributeBox().decode(record.get(4)).get();
        this.ref = conversionManager.getRefAttributeBox().decode(record.get(5)).get();
        this.alt = conversionManager.getAltAttributeBox().decode(record.get(6)).get();

        this.csvLocation = new CSVLocation(record.get(7), (IntList) record.get(8));
    }

    @Override
    public Bytes getID() {
        return ID;
    }

    @Override
    public Bytes getRef() {
        return ref;
    }

    @Override
    public Bytes getAlt() {
        return alt;
    }

    @Override
    public SVGenotypes getSVGenotypes() {
        return genotypes;
    }
}
