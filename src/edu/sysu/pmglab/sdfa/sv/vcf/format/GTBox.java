package edu.sysu.pmglab.sdfa.sv.vcf.format;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.genotype.Genotype;
import edu.sysu.pmglab.gtb.genome.genotype.IGenotypes;
import edu.sysu.pmglab.gtb.genome.genotype.container.SparseGenotypes;
import edu.sysu.pmglab.gtb.genome.genotype.encoder.Encoder;

/**
 * @author Wenjie Peng
 * @create 2024-12-19 21:48
 * @description
 */
public class GTBox extends FormatAttrBox<IGenotypes, GTBox> {

    final Encoder encoder = new Encoder();
    public static final GTBox instance = new GTBox(0);


    public GTBox() {

    }

    public GTBox(Bytes encode) {
        value = IGenotypes.load(encode);
    }

    public GTBox(int sampleSize) {
        if (sampleSize <= 0) {
            value = GenotypeConstant.EMPTY_ENUMERATED_GENOTYPES;
        } else {
            value = new SparseGenotypes(sampleSize);
        }
    }

    public GTBox(IGenotypes genotypes) {
        this.value = genotypes;
    }

    public GTBox(int sampleSize, Genotype genotype) {
        if (sampleSize <= 0) {
            value = GenotypeConstant.EMPTY_ENUMERATED_GENOTYPES;
        } else {
            value = new SparseGenotypes(sampleSize);
            for (int i = 0; i < sampleSize; i++) {
                value.set(i, genotype.bytecode());
            }
        }
    }

    public GTBox init(int sampleSize) {
        if (sampleSize <= 0) {
            value = GenotypeConstant.EMPTY_ENUMERATED_GENOTYPES;
        } else {
            value = new SparseGenotypes(sampleSize);
        }
        return this;
    }

    @Override
    public String getBriefName() {
        return "GT";
    }

    @Override
    public String getDescription() {
        return "Genotypes";
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.bytecode;
    }

    @Override
    public int sizeOfItemInEachIndividual() {
        return 1;
    }

    @Override
    public GTBox newInstance() {
        GTBox res = new GTBox();
        return value == null ? res : res.set(new SparseGenotypes(value.size()));
    }

    @Override
    public GTBox char2Object(String s) {
        throw new UnsupportedOperationException("Two components can make up the genotypes: counter and GT");
    }

    @Override
    public GTBox char2Object(Bytes bytes, boolean b) {
        return decode(bytes);
    }

    @Override
    public Bytes encode() {
        return encoder.encode(value);
    }

    @Override
    public GTBox decode(Bytes iByteCode) {
        return new GTBox(IGenotypes.load(iByteCode));
    }

    public GTBox decodeSelf(Bytes iByteCode) {
        value = IGenotypes.load(iByteCode);
        return this;
    }

    @Override
    public Bytes toBytes() {
        return new Bytes(value.counter().toString());
    }

    @Override
    public int get(int sampleIndex, int valueIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int get(int sampleIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadOne(Bytes item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forceLoadOne(Object item) {
        throw new UnsupportedOperationException("Can't add a item without index");
    }

    @Override
    public Object getByIndex(int index) {
        return value.get(index);
    }

    public void loadOne(int index, Bytes item) {
        value.set(index, item.toGenotype());
    }

    public void loadOne(int index, Genotype genotype) {
        value.set(index, genotype);
    }

    public void loadOne(int index, int intGenotype) {
        value.set(index, intGenotype);
    }

    public void clear() {
        if (value instanceof SparseGenotypes) {
            ((SparseGenotypes) value).clear();
        }
    }

    public Genotype getGenotype(int sampleIndex) {
        return value.get(sampleIndex);
    }

    public int size() {
        return value.size();
    }

    @Override
    public int sizeOfIndividual() {
        return value.size();
    }

    @Override
    public Object getObjectByIndividualIndex(int index) {
        return get(index);
    }

    public GTBox cloneTo(GTBox box) {
        int size = value.size();
        if (box == null) {
            box = new GTBox(new SparseGenotypes(size));
        }else if (box.size() != size) {
            box.value = new SparseGenotypes(size);
        }
        for (int i = 0; i < size; i++) {
            box.value.set(i, value.get(i));
        }
        return box;
    }

    public Bytes encodeBy(Encoder encoder){
        return encoder.encode(value);
    }
}
