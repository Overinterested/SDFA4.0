package edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.gty;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;

import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2024-10-21 21:39
 * @description
 */
public class GenotypeFilterManager {
    boolean filter;

    IntList fixedGtyFilterCounter;
    final List<String> fixedGtyFilterNameSet;
    final List<Function<String, Boolean>> fixedGtyFilterList;

    public GenotypeFilterManager(List<String> fixedGtyFilterNameSet,
                                 List<Function<String, Boolean>> fixedGtyFilterList) {
        this.fixedGtyFilterList = fixedGtyFilterList;
        this.fixedGtyFilterNameSet = fixedGtyFilterNameSet;
    }

    public GenotypeFilterManager(boolean filter,
                                 IntList fixedGtyFilterCounter, List<String> fixedGtyFilterNameSet,
                                 List<Function<String, Boolean>> fixedGtyFilterList) {
        this.filter = filter;
        this.fixedGtyFilterCounter = fixedGtyFilterCounter;
        this.fixedGtyFilterNameSet = fixedGtyFilterNameSet;
        this.fixedGtyFilterList = fixedGtyFilterList;
    }

    public GenotypeFilterManager setFilter(boolean filter) {
        this.filter = filter;
        return this;
    }

    public boolean filter() {
        return filter;
    }

    public IntList getFixedGtyFilterCounter() {
        return fixedGtyFilterCounter;
    }

    public List<String> getFixedGtyFilterNameSet() {
        return fixedGtyFilterNameSet;
    }

    public List<Function<String, Boolean>> getFixedGtyFilterList() {
        return fixedGtyFilterList;
    }


    public GenotypeFilterManager setFixedGtyFilterCounter(IntList fixedGtyFilterCounter) {
        this.fixedGtyFilterCounter = fixedGtyFilterCounter;
        return this;
    }


    public boolean filterBuiltinGty() {
        return fixedGtyFilterList != null && !fixedGtyFilterList.isEmpty();
    }

    public boolean filter(int indexOfBuiltIn, Bytes fieldValue) {
        return fixedGtyFilterList.fastGet(indexOfBuiltIn)
                .apply(fieldValue.toString());
    }

    public GenotypeFilterManager newInstance() {
        return new GenotypeFilterManager(
                filter,
                filter ? IntList.wrap(new int[fixedGtyFilterCounter.size()]) : null,
                filter ? fixedGtyFilterNameSet : null,
                filter ? fixedGtyFilterList : null
        );
    }
}
