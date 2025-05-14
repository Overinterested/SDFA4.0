package edu.sysu.pmglab.sdfa.sv.vcf.calling;

import edu.sysu.pmglab.sdfa.sv.vcf.calling.parser.*;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 20:52
 * @description
 */
public enum CallingType implements ICallingType {
    CUTESV("cutesv", new CuteSVParser()),
    CUTESV2("cutesv2", new CuteSV2Parser()),
    DEBREAK("debreak", new DebreakParser()),
    DELLY("delly", new DellyParser()),
    NANOSV("nanosv", new NanoSVParser()),
    NANOVAR("nanovar", new NanovarParser()),
    PBSV("pbsv", new PbsvParser()),
    PICKY("picky", new PickyParser()),
    SNIFFLES("sniffles", new SnifflesParser()),
    SNIFFLES2("sniffles2", new Sniffles2Parser()),
    SVIM("svim", new SvimParser()),
    SVISION("svision", new SvisionParser()),
    UKBB("ukbb", new UkbbParser()),
    VCF4_3("vcf4.3", new StandardVCFParser());
    final String name;
    final AbstractCallingParser parser;

    private CallingType(String name, AbstractCallingParser parser) {
        this.name = name;
        this.parser = parser;
    }

    @Override
    public String getName() {
        return name;
    }

    public AbstractCallingParser getParser() {
        return parser;
    }
}
