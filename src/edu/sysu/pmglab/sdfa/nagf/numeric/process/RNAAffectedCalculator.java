package edu.sysu.pmglab.sdfa.nagf.numeric.process;

import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-26 01:55
 * @description
 */
public interface RNAAffectedCalculator {
    HashMap<String, RNAAffectedCalculator> cache = new HashMap<>();

    RNAAffectedCalculator DEFAULT_CALCULATOR = (sdsv, rnaRecord) -> rnaRecord.getCalc().locateProportion(sdsv);
    RNAAffectedCalculator INV_DEFAULT_CALC = ((sdsv, rnaRecord) -> {
       if (sdsv.getType() == SVTypeSign.getByName("INV")){
           IntInterval coordinateInterval = sdsv.getCoordinateInterval();
           if (rnaRecord.getWholeRange().contains(coordinateInterval.start(), coordinateInterval.end())){
               return new float[7];
           }
       }
       return rnaRecord.getCalc().locateProportion(sdsv);
    });
    float[] affect(ISDSV sdsv, SourceRNARecord rnaRecord);

    static RNAAffectedCalculator getByDefault(String name) {
        if (name == null){
            return DEFAULT_CALCULATOR;
        }
        return cache.getOrDefault(name, DEFAULT_CALCULATOR);
    }
}
