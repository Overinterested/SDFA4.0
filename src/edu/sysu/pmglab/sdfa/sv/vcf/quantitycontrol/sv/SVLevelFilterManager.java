package edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.sv;

import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.csv.SharedFieldComplexSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.util.HashSet;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2024-10-21 21:48
 * @description
 */
public class SVLevelFilterManager {
    boolean filter;

    int validContigCounter;
    final HashSet<String> validContigNameSet;

    int locationFilterCounter;
    final IntInterval locationFilterInterval;

    int validTypeFilterCounter;
    final HashSet<SVTypeSign> validSVTypeSet;


    IntList fieldFilterCounter;
    final List<Function<ISDSV, Boolean>> fieldFilterList;

    IntList extraFilterCounter;
    final List<Function<ISDSV, Boolean>> extraFilterList;


    public SVLevelFilterManager(boolean filter,
                                int validContigCounter, HashSet<String> validContigNameSet,
                                int locationFilterCounter, IntInterval locationFilterInterval,
                                int validTypeFilterCounter, HashSet<SVTypeSign> validSVTypeSet,
                                IntList fieldFilterCounter, List<Function<ISDSV, Boolean>> fieldFilterList,
                                IntList extraFilterCounter, List<Function<ISDSV, Boolean>> extraFilterList) {
        this.filter = filter;
        this.validContigCounter = validContigCounter;
        this.validContigNameSet = validContigNameSet;
        this.locationFilterCounter = locationFilterCounter;
        this.locationFilterInterval = locationFilterInterval;
        this.validTypeFilterCounter = validTypeFilterCounter;
        this.validSVTypeSet = validSVTypeSet;
        this.fieldFilterCounter = fieldFilterCounter;
        this.fieldFilterList = fieldFilterList;
        this.extraFilterCounter = extraFilterCounter;
        this.extraFilterList = extraFilterList;
    }

    public boolean filter() {
        return filter;
    }

    public SVLevelFilterManager setFilter(boolean filter) {
        this.filter = filter;
        return this;
    }

    public boolean filter(ISDSV sv) {
        if (sv == null){
            return false;
        }
        if (validSVTypeSet != null) {
            SVTypeSign type = sv.getType();
            filter = validSVTypeSet.contains(type);
            if (!filter) {
                validContigCounter++;
                return false;
            }
        }

        if (validContigNameSet != null) {
            SVCoordinate coordinate = sv.getCoordinate();
            String chr = coordinate.getChr();
            if (!validContigNameSet.contains(chr)) {
                validTypeFilterCounter++;
                return false;
            }
        }

        if (locationFilterInterval != null) {
            int length = sv.length();
            boolean contains = locationFilterInterval.contains(length);
            if (!contains) {
                locationFilterCounter++;
                return false;
            }
        }

        if (fieldFilterList != null && !fieldFilterList.isEmpty()) {
            int index = 0;
            for (Function<ISDSV, Boolean> isdsvBooleanFunction : fieldFilterList) {
                Boolean apply = isdsvBooleanFunction.apply(sv);
                if (!apply) {
                    fieldFilterCounter.fastSet(index, fieldFilterCounter.fastGet(index) + 1);
                    return false;
                }
                index++;
            }
        }

        if (extraFilterList != null && !extraFilterList.isEmpty()) {
            int index = 0;
            for (Function<ISDSV, Boolean> isdsvBooleanFunction : extraFilterList) {
                Boolean apply = isdsvBooleanFunction.apply(sv);
                if (!apply) {
                    extraFilterCounter.fastSet(index, fieldFilterCounter.fastGet(index) + 1);
                    return false;
                }
                index++;
            }
        }
        return true;
    }

    public boolean filter(SharedFieldComplexSV sharedFieldComplexSV) {
        if (validSVTypeSet != null) {
            SVTypeSign type = sharedFieldComplexSV.getType();
            filter = validSVTypeSet.contains(type);
            if (!filter) {
                validContigCounter++;
                return false;
            }
        }

        if (validContigNameSet != null) {
            List<SVCoordinate> coordinates = sharedFieldComplexSV.getCoordinates();
            for (SVCoordinate coordinate : coordinates) {
                String chr = coordinate.getChr();
                if (validContigNameSet.contains(chr)) {
                    continue;
                }
                validTypeFilterCounter++;
                return false;
            }
        }

        if (locationFilterInterval != null) {
            int length = sharedFieldComplexSV.getLength();
            boolean contains = locationFilterInterval.contains(length);
            if (!contains) {
                locationFilterCounter++;
                return false;
            }
        }

        if (fieldFilterList != null && !fieldFilterList.isEmpty()) {
            int index = 0;
            for (Function<ISDSV, Boolean> isdsvBooleanFunction : fieldFilterList) {
                Boolean apply = isdsvBooleanFunction.apply(sharedFieldComplexSV);
                if (!apply) {
                    fieldFilterCounter.fastSet(index, fieldFilterCounter.fastGet(index) + 1);
                    return false;
                }
                index++;
            }
        }

        if (extraFilterList != null && !extraFilterList.isEmpty()) {
            int index = 0;
            for (Function<ISDSV, Boolean> isdsvBooleanFunction : extraFilterList) {
                Boolean apply = isdsvBooleanFunction.apply(sharedFieldComplexSV);
                if (!apply) {
                    extraFilterCounter.fastSet(index, extraFilterCounter.fastGet(index) + 1);
                    return false;
                }
                index++;
            }
        }
        return true;
    }

    public SVLevelFilterManager newInstance() {
        return new SVLevelFilterManager(
                filter, 0, validContigNameSet,
                0, locationFilterInterval,
                0, validSVTypeSet,
                fieldFilterList == null ? null : IntList.wrap(new int[fieldFilterList.size()]), fieldFilterList,
                extraFilterList == null ? null : IntList.wrap(new int[extraFilterList.size()]), extraFilterList
        );
    }

    public void clear() {
        validContigCounter = 0;
        locationFilterCounter = 0;
        validTypeFilterCounter = 0;

        if (fieldFilterCounter != null && !fieldFilterCounter.isEmpty()) {
            int size = fieldFilterCounter.size();
            for (int i = 0; i < size; i++) {
                fieldFilterCounter.fastSet(i, 0);
            }
        }

        if (extraFilterCounter != null && !extraFilterList.isEmpty()) {
            int size = extraFilterCounter.size();
            for (int i = 0; i < size; i++) {
                extraFilterCounter.fastSet(i, 0);
            }
        }
    }
}
