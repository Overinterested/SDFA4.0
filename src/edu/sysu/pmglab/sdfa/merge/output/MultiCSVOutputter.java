package edu.sysu.pmglab.sdfa.merge.output;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.csv.ComplexSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.CompleteSDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wenjie Peng
 * @create 2024-10-07 03:09
 * @description
 */
public interface MultiCSVOutputter {
    ByteStream cache = new ByteStream();
    IntList supportRecords = new IntList();
    AtomicInteger count = new AtomicInteger(0);
    HashMap<String, MultiCSVOutputter> outputter = new HashMap<>();
    DecimalFormat df = new DecimalFormat("0.##");

    DynamicIndexableMap<Bytes, Bytes> fixedInfo = new DynamicIndexableMap<>(
            new LinkedSet<>(new Bytes[]{

            })
    );

    // default outputter
    MultiCSVOutputter DEFAULT_CSV_MERGED_OUTPUT = new MultiCSVOutputter() {
        @Override
        public void outputTo(List<ComplexSV> canBeMergedCSVs, ByteStream cache) {
            ComplexSV complexSV = canBeMergedCSVs.fastGet(0);
            int size = canBeMergedCSVs.size();
            int numOfSubSVs = complexSV.numOfSubSVs();
            IntList posList = new IntList(size);
            IntList endList = new IntList(size);
            IntList lenList = new IntList(size);
            for (int i = 0; i < size; i++) {
                posList.add(0);
                endList.add(0);
                lenList.add(0);
            }
            for (ComplexSV canBeMergedCSV : canBeMergedCSVs) {
                List<ISDSV> svs = canBeMergedCSV.getSVs();
                for (int i = 0; i < numOfSubSVs; i++) {
                    posList.set(i, posList.fastGet(0) + svs.fastGet(i).getPos());
                    endList.set(i, endList.fastGet(0) + svs.fastGet(i).getEnd());
                    lenList.set(i, lenList.fastGet(0) + svs.fastGet(i).length());
                }
            }
            // mean
            for (int i = 0; i < numOfSubSVs; i++) {
                posList.fastSet(i, posList.fastGet(i) / size);
                endList.fastSet(i, endList.fastGet(i) / size);
                lenList.fastSet(i, lenList.fastGet(i) / size);
            }
            for (int i = 0; i < numOfSubSVs; i++) {
                fixedInfo.put(new Bytes("POS" + (i + 1)), writeAndClear(cache, posList.fastGet(i)));
                fixedInfo.put(new Bytes("END" + (i + 1)), writeAndClear(cache, endList.fastGet(i)));
                fixedInfo.put(new Bytes("SVLEN" + (i + 1)), writeAndClear(cache, lenList.fastGet(i)));
            }
            fixedInfo.put(VCFInfoManager.SVTYPE, writeAndClear(cache, complexSV.getNameOfType()));
            CompleteSDSV completeSDSV = new CompleteSDSV();
            completeSDSV.setCoordinate(new SVCoordinate(posList.fastGet(0), endList.get(0), complexSV.getContigName(0)))
                    .setID(new Bytes("SDFA." + count.incrementAndGet()))
                    .setAlt(new Bytes("<" + complexSV.getType().getName() + ">"))
                    .setInfo(fixedInfo);
            cache.write(completeSDSV.toVCFRecord(cache));
        }
    };


    void outputTo(List<ComplexSV> canBeMergedCSVs, ByteStream cache);

    static MultiCSVOutputter getByDefault(String nameOfCSVType) {
        return outputter.getOrDefault(nameOfCSVType, DEFAULT_CSV_MERGED_OUTPUT);
    }

    static void initSupportRecords(int numOfSamples) {
        for (int i = 0; i < numOfSamples; i++) {
            supportRecords.add(0);
        }
    }

    static Bytes writeAndClear(ByteStream cache, int value) {
        cache.write(ASCIIUtility.toASCII(value));
        Bytes transferValue = cache.toBytes().detach();
        cache.clear();
        return transferValue;
    }

    static Bytes writeAndClear(ByteStream cache, float value) {
        cache.write((ASCIIUtility.toASCII(df.format(value),Constant.CHAR_SET)));
        Bytes transferValue = cache.toBytes().detach();
        cache.clear();
        return transferValue;
    }

    static Bytes writeAndClear(ByteStream cache, String value) {
        cache.write(ASCIIUtility.toASCII(value,Constant.CHAR_SET));
        Bytes transferValue = cache.toBytes().detach();
        cache.clear();
        return transferValue;
    }

    static Bytes writeAndClear(ByteStream cache, int ci_min, int ci_max) {
        cache.write(ASCIIUtility.toASCII(ci_min));
        cache.write(Constant.COMMA);
        cache.write(ASCIIUtility.toASCII(ci_max));
        Bytes transferValue = cache.toBytes().detach();
        cache.clear();
        return transferValue;
    }

    static Bytes writeAndClear(ByteStream cache, IntList values) {
        for (int i = 0; i < values.size(); i++) {
            cache.write(ASCIIUtility.toASCII(values.fastGet(i)));
            values.fastSet(i, 0);
        }
        Bytes transferValue = cache.toBytes().detach();
        cache.clear();
        return transferValue;
    }
}

