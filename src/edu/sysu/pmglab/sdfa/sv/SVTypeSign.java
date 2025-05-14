package edu.sysu.pmglab.sdfa.sv;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.list.StringListBox;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.easytools.Constant;

import java.util.Collection;


/**
 * @author Wenjie Peng
 * @create 2024-08-26 00:23
 * @description manage SV types
 */
public class SVTypeSign {
    final int index;
    final String name;
    final boolean isComplex;
    final boolean spanContig;
    private static final DynamicIndexableMap<Bytes, SVTypeSign> typesOfSV = new DynamicIndexableMap<>();
    private static final DynamicIndexableMap<Bytes, SVTypeSign> typesOfSubCSV = new DynamicIndexableMap<>();
    public static final SVTypeSign unknown = new SVTypeSign("unknown", false, false, -1);

    public static SVTypeSign getByName(Bytes nameOfType) {
        SVTypeSign returns = typesOfSV.get(nameOfType);
        return returns == null ? unknown : returns;
    }

    public static String getStandardizedName(String rawTypeName) {
        SVTypeSign returns = getByName(rawTypeName);
        return returns == null ? null : returns.name;
    }
    public static SVTypeSign getByName(String nameOfType) {
        return getByName(new Bytes(nameOfType));
    }

    public static SVTypeSign getByIndex(int indexOfType) {
        SVTypeSign byIndex = typesOfSV.getByIndex(indexOfType);
        return byIndex == null ? unknown : byIndex;
    }

    public static SVTypeSign add(String nameOfType) {
        return add(nameOfType, false, false);
    }

    public static SVTypeSign add(String nameOfType, boolean isComplex, boolean spanChromosome) {
        if (nameOfType == null) {
            throw new UnsupportedOperationException("Can't add NULL value into SV types");
        }
        Bytes name = new Bytes(nameOfType);
        int index = typesOfSV.indexOfKey(name);
        if (index != -1) {
            return typesOfSV.getByIndex(index);
        } else {
            synchronized (typesOfSV) {
                SVTypeSign type = new SVTypeSign(nameOfType, isComplex, spanChromosome, typesOfSV.size());
                typesOfSV.put(new Bytes(type.getName()), type);
                return type;
            }
        }
    }

    public void alias(String... alternativeNames) {
        if (this == unknown) {
            throw new UnsupportedOperationException();
        } else {
            synchronized (typesOfSV) {
                for (String alternativeName : alternativeNames) {
                    if (alternativeName != null) {
                        typesOfSV.put(new Bytes(alternativeName), this);
                    }
                }
            }
        }
    }

    public static void clear() {
        synchronized (typesOfSV) {
            typesOfSV.clear();
            typesOfSubCSV.clear();
        }
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public String toString() {
        return this.name;
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            SVTypeSign that = (SVTypeSign) o;
            return this.name.equals(that.name);
        } else {
            return false;
        }
    }

    public static Collection<SVTypeSign> support() {
        return typesOfSV.values();
    }

    private SVTypeSign(String name, boolean isComplex, boolean spanContig, int index) {
        this.name = name;
        this.index = index;
        this.isComplex = isComplex;
        this.spanContig = spanContig;
    }

    public StringListBox getNames() {
        StringListBox names = new StringListBox();
        // TODO: check
        names.set(typesOfSV.values().stream().map(SVTypeSign::getName).toArray(String[]::new));
        return names;
    }

    static {
        add("BND", true, true).alias("BND", "Bnd");
        add("CNV").alias("CNV", "cnv", "mcnv", "MCNV");
        add("DEL").alias("del", "DEL", "Deletion", "Delete", "delete", "deletion");
        add("DUP").alias("dup", "DUP", "Duplication", "duplication", "Duplication/Triplication");
        add("INS").alias("ins", "INS", "Insertion", "Insert", "insert", "insertion");
        add("DUP/INS").alias("dup/ins");
        add("INV").alias("inv", "INV", "Inversion", "Inverse", "inverse", "inversion");
        add("MEI").alias("mei", "mobile element insertion", "mobile element insert", "mobile_element_insertion", "INS:ME", "INS:MEI");
        /**
         * LINE: Long Interspersed Nuclear Element 1 is a self-replicating and self-diffused DNA sequence that can be inserted into genomic DNA to produce structural variations. LINE transposons are also active and can transpose in different regions of the genome.
         * SVA: sine-r /VNTR/Alu is a complex transposon composed of two different transposons - SINE and Alu. SVA transposons are the most active in the human genome and can translocate in different regions of the genome, causing structural variation.
         * ALU: ALU sequences are a class of short interval repeats consisting of about 300 bp, belonging to SINEs(Short interval Repeats). ALU transposons can be inserted into genomic DNA, causing structural changes in the genome.
         */
        add("INS:LINE1").alias("line1", "INS:ME:LINE1", "line");
        add("INS:ALU").alias("alu", "INS:ME:ALU");
        add("INS:SVA").alias("sva", "INS:ME:SVA");
        add("DEL:LINE1").alias("DEL_LINE1");
        add("DEL:ALU").alias("DEL_ALU");
        add("DEL:SVA").alias("DEL_SVA");
        /**
         * CNV transfer:
         * 1. MCNV
         * 2. copy number gain
         * 3. copy number loss
         */
        add("MCNV");
        add("copy number loss").alias("copy_number_loss");
        add("copy number gain").alias("copy_number_gain", "Copy-Number Gain");
        /**
         * duplication transfer:
         * 1. DUP:TANDEM is an adjacent repeat, where genomic regions are repeated and arranged next to each other
         * 2. DUP:INT is an interval repeat, where regions of the genome are repeated but with other sequences inserted in between
         * 3. DUP:INV is an inverted duplication
         */
        add("DUP:TANDEM", false, false).alias("tDUP", "tandem_duplication", "tandem duplication");
        add("DUP:INT", false, false).alias("interval_duplication", "interval duplication");
        add("DUP:INV", false, false).alias("INVDUP", "INV/INVDUP", "DUPINV", "inverted_duplication", "inverted duplication");
        add("TRA", true, true).alias("deletion_insertion", "delins", "DELINS", "TRA", "TRA:delins", "tra", "traslocation", "Reciprocal translocation", "reciprocal translocation", "reciprocal_translocation");
        add("DEL/INV").alias("del/inv");
        add("CPX", true, true);
        add("CTX", true, true);
        // CSV::DEL
        add(new CSVSubType("DEL").fullType.toString(), true, false);
        typesOfSubCSV.put(new Bytes("DEL"), typesOfSV.get(new Bytes("CSV::DEL")));
        // CSV::INS
        add(new CSVSubType("INS").fullType.toString(), true, false);
        typesOfSubCSV.put(new Bytes("INS").detach(), typesOfSV.get(new Bytes("CSV::INS")));
        // CSV::INV
        add(new CSVSubType("INV").fullType.toString(), true, false);
        typesOfSubCSV.put(new Bytes("INV").detach(), typesOfSV.get(new Bytes("CSV::INV")));
        // CSV::DUP
        add(new CSVSubType("DUP").fullType.toString(), true, false);
        typesOfSubCSV.put(new Bytes("DUP").detach(), typesOfSV.get(new Bytes("CSV::DUP")));
        // CSV::tDUP
        add(new CSVSubType("tDUP").fullType.toString(), true, false);
        typesOfSubCSV.put(new Bytes("tDUP").detach(), typesOfSV.get(new Bytes("CSV::tDUP")));

        add("Triplication", false, false).alias("triplication");
        add("Amplification", false, false);

    }

    public boolean isComplex() {
        return isComplex;
    }

    public boolean spanChromosome() {
        return spanContig;
    }

    public static SVTypeSign decode(int encode) {
        return getByIndex(encode);
    }

    public static SVTypeSign getCSVSubType(Bytes CSVSubType) {
        return typesOfSubCSV.get(CSVSubType);
    }

    static class CSVSubType {
        final Bytes subType;
        final Bytes fullType;

        public CSVSubType(String subType) {
            this(new Bytes(subType));
        }

        public CSVSubType(Bytes subType) {
            this.subType = subType;
            ByteStream cache = new ByteStream();
            cache.write(new byte[]{Constant.C, Constant.S, Constant.V, Constant.COLON, Constant.COLON});
            cache.write(subType);
            this.fullType = cache.toBytes().detach();
            cache.close();
        }
    }

    public boolean spanContig() {
        return spanContig;
    }
}
