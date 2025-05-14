package edu.sysu.pmglab.sdfa.toolkit;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.genotype.IGenotypes;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.base.SDFFormatManager;
import edu.sysu.pmglab.sdfa.base.SDFInfoManager;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrBox;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;
import gnu.trove.procedure.TObjectProcedure;

import javax.jws.Oneway;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2025-03-10 02:19
 * @description
 */
public class SDFRecordWrapper {
    protected boolean initGT;
    protected boolean modifyGT;

    private int sampleSize;
    protected IRecord record;
    protected GTBox genotypes;
    protected GTBox modifiedGT;
    protected final SDFReader reader;
    protected SDFInfoManager infoManager;
    protected SDFFormatManager formatManager;
    protected SDSVConversionManager conversionManager;

    public SDFRecordWrapper(SDFReader reader) {
        this.reader = reader;
        this.sampleSize = reader.numOfIndividuals();
        this.infoManager = reader.getInfoManager();
        this.formatManager = reader.getFormatManager();
        this.conversionManager = reader.getConversion();
    }

    public SDFRecordWrapper init(IRecord record) {
        this.initGT = true;
        this.record = record;
        this.modifyGT = false;
        return this;
    }

    public IRecord filter() {
        return null;
    }

    public boolean filterGT(Bytes formatAttr, TObjectProcedure filter) {
        int index = formatManager.indexOf(formatAttr);
        return filterGT(index, filter);
    }

    public boolean filterGT(int indexOfFormatAttr, TObjectProcedure filter) {
        if (initGT) {
            genotypes = new GTBox(IGenotypes.load(record.get(3)));
            initGT = false;
        }
        for (int i = 0; i < sampleSize; i++) {
            Object formatValueByIndex = getFormatValueByIndex(i, indexOfFormatAttr);
            boolean apply = filter.execute(formatValueByIndex);
            if (!apply) {
                if (!modifyGT) {
                    modifiedGT = genotypes.cloneTo(modifiedGT);
                    modifyGT = true;
                }
                modifiedGT.loadOne(i, GenotypeConstant.MISSING_GTY);
            }
        }
        return !modifyGT;
    }

    public void filterGT(String formatAttr, TObjectProcedure filter) {
        filterGT(new Bytes(formatAttr), filter);
    }

    public boolean filterSV(String attr, TObjectProcedure<Object> filter) {
        Object v = getV(attr);
        return filter.execute(v);
    }

    /**
     * get specific format value of an indexed sample in curr SV record:
     *
     * @param sampleIndex
     * @param attr
     * @return
     */
    public Object getFormatValueByName(int sampleIndex, String attr) {
        int index = formatManager.getFormatAttrNameList().indexOf(new Bytes(attr));
        if (index == -1) {
            return null;
        }
        return getFormatValueByIndex(sampleIndex, index);
    }

    public Object getFormatValueByName(int sampleIndex, Bytes attr) {
        int index = formatManager.getFormatAttrNameList().indexOf(attr);
        if (index == -1) {
            return null;
        }
        return getFormatValueByIndex(sampleIndex, index);
    }

    public Object getFormatValueByIndex(int sampleIndex, int attrIndex) {
        FormatAttrBox box = formatManager.getBox(attrIndex);
        if (box.sizeOfIndividual() == 0) {
            formatManager.decode(record.get(4), attrIndex);
        }
        return box.getObjectByIndividualIndex(sampleIndex);
    }

    /**
     * get attr value of curr SV record
     *
     * @param attr
     * @return
     */
    public Object getV(String attr) {
        Object res;
        switch (attr) {
            case "CHR":
                res = reader.getContigByIndex(record.get(0));
                return res;
            case "ID":
                res = conversionManager.getIdAttributeBox().decode(record.get(5)).get();
                return res;
            case "REF":
                res = conversionManager.getRefAttributeBox().decode(record.get(6)).get();
                return res;
            case "ALT":
                res = conversionManager.getAltAttributeBox().decode(record.get(7)).get();
                return res;
            case "QUAL":
                res = conversionManager.getQualAttributeBox().decode(record.get(8)).get();
                return res;
            case "FILTER":
                res = conversionManager.getFilterAttributeBox().decode(record.get(9)).get();
                return res;
            case "INFO":
                res = record.get(10);
                return res;
            case "FORMAT":
                res = formatManager.getFormatAttrNameList();
                return res;
            case "GT":
                res = IGenotypes.load(record.get(3));
                return res;
            case "LEN":
                res = record.get(1);
                return res;
        }
        int index = record.indexOf(attr);
        if (index == -1) {
            Bytes attrBytes = new Bytes(attr);
            index = infoManager.indexOf(attrBytes);
            if (index != -1) {
                List<Bytes> infoValues = record.get(10);
                res = infoValues.fastGet(index);
                return res;
            }

            index = reader.getFormatManager().indexOf(attrBytes);
            if (index != -1) {
                List<Bytes> gtyMetrics = record.get(4);
                res = formatManager.decode(gtyMetrics, index);
                return res;
            }
        } else {
            return record.get(index);
        }
        throw new UnsupportedOperationException(attr + " can't be found in record.");
    }

    public IRecord getRecord() {
        return record;
    }

}
