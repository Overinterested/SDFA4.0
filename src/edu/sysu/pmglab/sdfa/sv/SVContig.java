package edu.sysu.pmglab.sdfa.sv;

import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.meta.ICCFMeta;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.ccf.type.interval.IntIntervalBox;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.NamedSet;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;

import java.util.Set;

/**
 * @author Wenjie Peng
 * @create 2024-08-25 20:25
 * @description
 */
public class SVContig {
    IntList contigCount;
    boolean built = false;
    boolean closed = false;
    private NamedSet<Chromosome> contigs = new NamedSet<>();
    DynamicIndexableMap<String, IntInterval> contigRanges = new DynamicIndexableMap<>();

    //    private final ByteListBox contigNamesBox = new ByteListBox();
    public static final String SDF_CONTIG_NAMES = "$CONTIG_NAMES";
    private final IntIntervalBox contigRangeBox = new IntIntervalBox();
    private static final NamedSet<Chromosome> HUMAN_CONTIG = new NamedSet<>();

    static {
        HUMAN_CONTIG.adds(new Chromosome(0, "1"), new String[]{"1", "chr1", "CM000663.1", "CM000663.2", "NC_000001.10", "NC_000001.11"});
        HUMAN_CONTIG.adds(new Chromosome(1, "2"), new String[]{"2", "chr2", "CM000664.1", "CM000664.2", "NC_000002.11", "NC_000002.12"});
        HUMAN_CONTIG.adds(new Chromosome(2, "3"), new String[]{"3", "chr3", "CM000665.1", "CM000665.2", "NC_000003.11", "NC_000003.12"});
        HUMAN_CONTIG.adds(new Chromosome(3, "4"), new String[]{"4", "chr4", "CM000666.1", "CM000666.2", "NC_000004.11", "NC_000004.12"});
        HUMAN_CONTIG.adds(new Chromosome(4, "5"), new String[]{"5", "chr5", "CM000667.1", "CM000667.2", "NC_000005.10", "NC_000005.11"});
        HUMAN_CONTIG.adds(new Chromosome(5, "6"), new String[]{"6", "chr6", "CM000668.1", "CM000668.2", "NC_000006.10", "NC_000006.11"});
        HUMAN_CONTIG.adds(new Chromosome(6, "7"), new String[]{"7", "chr7", "CM000669.1", "CM000669.2", "NC_000007.10", "NC_000007.11"});
        HUMAN_CONTIG.adds(new Chromosome(7, "8"), new String[]{"8", "chr8", "CM000670.1", "CM000670.2", "NC_000008.10", "NC_000008.11"});
        HUMAN_CONTIG.adds(new Chromosome(8, "9"), new String[]{"9", "chr9", "CM000671.1", "CM000671.2", "NC_000009.10", "NC_000009.11"});
        HUMAN_CONTIG.adds(new Chromosome(9, "10"), new String[]{"10", "chr10", "CM000672.1", "CM000672.2", "NC_000010.10", "NC_000010.11"});
        HUMAN_CONTIG.adds(new Chromosome(10, "11"), new String[]{"11", "chr11", "CM000673.1", "CM000673.2", "NC_000011.10", "NC_000011.11"});
        HUMAN_CONTIG.adds(new Chromosome(11, "12"), new String[]{"12", "chr12", "CM000674.1", "CM000674.2", "NC_000012.10", "NC_000012.11"});
        HUMAN_CONTIG.adds(new Chromosome(12, "13"), new String[]{"13", "chr13", "CM000675.1", "CM000675.2", "NC_000013.10", "NC_000013.11"});
        HUMAN_CONTIG.adds(new Chromosome(13, "14"), new String[]{"14", "chr14", "CM000676.1", "CM000676.2", "NC_000014.10", "NC_000014.11"});
        HUMAN_CONTIG.adds(new Chromosome(14, "15"), new String[]{"15", "chr15", "CM000677.1", "CM000677.2", "NC_000015.10", "NC_000015.11"});
        HUMAN_CONTIG.adds(new Chromosome(15, "16"), new String[]{"16", "chr16", "CM000678.1", "CM000678.2", "NC_000016.10", "NC_000016.11"});
        HUMAN_CONTIG.adds(new Chromosome(16, "17"), new String[]{"17", "chr17", "CM000679.1", "CM000679.2", "NC_000017.10", "NC_000017.11"});
        HUMAN_CONTIG.adds(new Chromosome(17, "18"), new String[]{"18", "chr18", "CM000680.1", "CM000680.2", "NC_000018.10", "NC_000018.11"});
        HUMAN_CONTIG.adds(new Chromosome(18, "19"), new String[]{"19", "chr19", "CM000681.1", "CM000681.2", "NC_000019.10", "NC_000019.11"});
        HUMAN_CONTIG.adds(new Chromosome(19, "20"), new String[]{"20", "chr20", "CM000682.1", "CM000682.2", "NC_000020.10", "NC_000020.11"});
        HUMAN_CONTIG.adds(new Chromosome(20, "21"), new String[]{"21", "chr21", "CM000683.1", "CM000683.2", "NC_000021.10", "NC_000021.11"});
        HUMAN_CONTIG.adds(new Chromosome(21, "22"), new String[]{"22", "chr22", "CM000684.1", "CM000684.2", "NC_000022.10", "NC_000022.11"});
        HUMAN_CONTIG.adds(new Chromosome(22, "X"), new String[]{"x", "X", "chrX", "23", "chr23", "CM000685.1", "CM000685.2", "NC_000023.10", "NC_000023.11"});
        HUMAN_CONTIG.adds(new Chromosome(23, "Y"), new String[]{"y", "Y", "chrY", "24", "chr24", "CM000686.1", "CM000686.2", "NC_000024.9", "NC_000024.10"});
    }

    private SVContig() {
        int size = HUMAN_CONTIG.size();
        contigCount = new IntList(size);
        for (int i = 0; i < size; i++) {
            Chromosome chromosome = HUMAN_CONTIG.valueOf(i);
            contigs.adds(chromosome, HUMAN_CONTIG.aliasesOf(i));
            contigCount.add(0);
        }
    }

    public SVContig(NamedSet<Chromosome> contigs, DynamicIndexableMap<String, IntInterval> contigRanges) {
        this.contigs = contigs;
        this.contigRanges = contigRanges;
    }

    public static SVContig init() {
        return new SVContig();
    }

    public static SVContig init(IndexableSet<String> contigNames) {
        SVContig svContig = new SVContig();
        svContig.contigs.clear();
        svContig.contigCount.clear();
        for (String contigName : contigNames) {
            svContig.addContigName(contigName);
        }
        return svContig;
    }

    /**
     * add the number of record current contig
     *
     * @param contigName input chromosome name
     */
    public void countContigRecord(String contigName) {
        int index;
        Chromosome chromosome = contigs.valueOf(contigName);
        if (chromosome == null) {
            contigCount.add(0);
            index = contigs.size();
            contigs.adds(new Chromosome(contigs.size(), contigName), contigName);
        } else {
            index = chromosome.getIndex();
        }
        contigCount.fastSet(index, contigCount.fastGet(index) + 1);
    }

    public void countContigByIndex(int indexOfContig) {
        contigCount.fastSet(indexOfContig, contigCount.fastGet(indexOfContig) + 1);
    }

    public void countContigByIndex(int indexOfContig, int step) {
        contigCount.fastSet(indexOfContig, contigCount.fastGet(indexOfContig) + step);
    }

    public DynamicIndexableMap<String, IntInterval> getContigRanges() {
        if (built) {
            return contigRanges;
        }
        buildRanges();
        closed = true;
        return contigRanges;
    }


    public IntInterval getRange(Chromosome chromosome) {
        return contigRanges.get(chromosome.getName());
    }

    /**
     * register the contig name
     *
     * @param name
     */
    public void addContigName(String name) {
        Chromosome indexOfContig = contigs.valueOf(name);
        if (indexOfContig == null) {
            contigCount.add(0);
            contigs.adds(new Chromosome(contigs.size(), name), name);
        }
    }


    public int getContigIndexByName(String name) {
        Chromosome contig = contigs.valueOf(name);
        if (contig == null) {
            contigCount.add(0);
            Chromosome newContig = new Chromosome(contigs.size(), name);
            contigs.adds(newContig, name);
            return newContig.getIndex();
        }
        return contig.getIndex();
    }

    public String getContigNameByIndex(int indexOfContig) {
        return contigs.valueOf(indexOfContig).getName();
    }

    public static String name() {
        return SVContig.class.getName();
    }

    public static class Chromosome {
        final int index;
        final String name;

        public Chromosome(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * save contig names and contig ranges into meta items
     *
     * @return a list of items
     */
    public List<CCFMetaItem> save() {
        if (!built) {
            contigRanges.clear();
            // init range and result
            buildRanges();
        }
        List<CCFMetaItem> res = new List<>(contigRanges.size() + 1);
        // record metas of all contig names
        List<String> allContigNames = new List<>(contigRanges.keySet());
        res.add(CCFMetaItem.of(SDF_CONTIG_NAMES, allContigNames));
        // record metas of all contig ranges
        for (String validContigName : allContigNames) {
            IntInterval tmpInterval = contigRanges.get(validContigName);
            res.add(new CCFMetaItem(validContigName, FieldType.intInterval, tmpInterval));
        }
        return res;
    }

    private void buildRanges() {
        if (built) {
            return;
        }
        built = true;
        contigRanges.clear();
        int currIndex = 0;
        // init range
        int size = contigs.size();
        for (int i = 0; i < size; i++) {
            Chromosome chromosome = contigs.valueOf(i);
            String name = chromosome.getName();
            int numOfCurr = contigCount.fastGet(i);
            IntInterval intInterval = new IntInterval(currIndex, currIndex += numOfCurr);
            contigRanges.put(name, intInterval);
        }
    }

    public static synchronized SVContig load(ICCFMeta metas) {
        List<CCFMetaItem> encodedContigNames = metas.get(SDF_CONTIG_NAMES);

        List<String> contigNames = null;
        if (encodedContigNames != null && !encodedContigNames.isEmpty()) {
            CCFMetaItem contigNamesMeta = encodedContigNames.fastGet(0);
            contigNames = contigNamesMeta.getValue();
        }

        if (contigNames == null || contigNames.isEmpty()) {
            SVContig init = SVContig.init();
            init.clear();
            return init;
        }

        SVContig svContig = new SVContig();
        // load contig ranges
        for (int i = 0; i < contigNames.size(); i++) {
            String contigName = contigNames.fastGet(i);
            svContig.addContigName(contigName);
            List<CCFMetaItem> encodedContigRange = metas.get(contigName);
            IntInterval contigNameRange = encodedContigRange.fastGet(0).getValue();
            svContig.contigRanges.put(contigName, new IntInterval(contigNameRange.start(), contigNameRange.end()));
            svContig.contigCount.fastSet(i, contigNameRange.end() - contigNameRange.start());
        }
        svContig.built = true;
        return svContig;
    }

    /**
     * Get sv contigs and sv ranges from the CCFTable
     *
     * @param table input ccf table
     * @return sv contig
     */
    public static SVContig load(CCFTable table) {
        return load(table.getMeta());
    }

    public void clear() {
        closed = false;
        contigs.clear();
        contigCount.clear();
        contigRanges.clear();
    }

    public Set<String> support() {
        return contigRanges.keySet();
    }

    public IntInterval getRangeByName(String contigName) {
        IntInterval range = contigRanges.get(contigName);
        if (range == null || range.end() - range.start() == 0) {
            return null;
        }
        return range;
    }

    public void built(boolean isBuilt) {
        this.built = true;
    }

    public SVContig clone() {
        SVContig svContig = new SVContig();
        svContig.built = false;
        svContig.contigs = contigs;
        svContig.contigRanges = contigRanges;
        svContig.contigCount = contigCount.clone();
        return svContig;
    }


    public List<String> getContigNames() {
        List<String> validContigNames = new List<>();
        for (int i = 0; i < contigRanges.size(); i++) {
            validContigNames.add(contigRanges.keyOfIndex(i));
        }
        return validContigNames;
    }

    public SVContig merge(SVContig other) {
        List<String> contigNames = other.getContigNames();
        built = false;
        for (String contigName : contigNames) {
            int indexOfCurr = getContigIndexByName(contigName);
            int indexOfOther = other.getContigIndexByName(contigName);
            countContigByIndex(indexOfCurr, other.getCount(indexOfOther));
        }
        buildRanges();
        built = true;
        return this;
    }

    public int getCount(int indexOfContig) {
        IntInterval range = contigRanges.getByIndex(indexOfContig);
        return range.end() - range.start();
    }

    /**
     * merge two SVContig class with summing contig names and each count of them
     * @param var1
     * @param var2
     * @return
     */
    public static SVContig merge(SVContig var1, SVContig var2) {
        SVContig res = SVContig.init();
        SVContig other = var1;
        List<String> contigNames = other.getContigNames();
        for (String contigName : contigNames) {
            int indexOfCurr = res.getContigIndexByName(contigName);
            int indexOfOther = other.getContigIndexByName(contigName);
            res.countContigByIndex(indexOfCurr, other.getCount(indexOfOther));
        }
        other = var2;
        contigNames = other.getContigNames();
        for (String contigName : contigNames) {
            int indexOfCurr = res.getContigIndexByName(contigName);
            int indexOfOther = other.getContigIndexByName(contigName);
            res.countContigByIndex(indexOfCurr, other.getCount(indexOfOther));
        }
        res.buildRanges();
        res.built = true;
        return res;
    }

    public void decreaseCount(int index, int count) {
        built = false;
        contigCount.fastSet(index, contigCount.fastGet(index) - count);
    }

}
