package edu.sysu.pmglab.sdfa.merge.output;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.ByteList;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.genotype.Genotype;
import edu.sysu.pmglab.gtb.genome.genotype.IGenotypes;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVGenotypes;
import edu.sysu.pmglab.sdfa.sv.sdsv.CompleteSDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wenjie Peng
 * @create 2024-10-07 03:09
 * @description
 */
public interface MultiSSVOutputter {

    ByteStream cache = new ByteStream();
    IntList supportRecords = new IntList();
    IntList encodeGenotypes = new IntList();
    ByteList genotypeChangeFlag = new ByteList();
    SVGenotypes mergedGenotypes = new SVGenotypes((GTBox) null);
    DecimalFormat df = MultiCSVOutputter.df;

    Bytes CI_POS = new Bytes("CI_POS");
    Bytes CI_END = new Bytes("CI_END");
    Bytes CI_LEN = new Bytes("CI_LEN");
    Bytes STD_POS = new Bytes("STD_POS");
    Bytes STD_END = new Bytes("STD_END");
    Bytes STD_LEN = new Bytes("STD_LEN");
    Bytes SUPP_VEC = new Bytes("SUPP_VEC");
    AtomicInteger count = new AtomicInteger(0);

    HashMap<String, MultiSSVOutputter> outputter = new HashMap<>();
    DynamicIndexableMap<Bytes, Bytes> fixedInfo = new DynamicIndexableMap<>(
            new LinkedSet<>(new Bytes[]{
                    VCFInfoManager.END,
                    VCFInfoManager.SVLEN,
                    VCFInfoManager.SVTYPE,
                    CI_POS, CI_END, CI_LEN,
                    STD_POS, STD_END, STD_LEN, SUPP_VEC
            })
    );


    // default outputter
    MultiSSVOutputter DEFAULT_SSV_MERGED_OUTPUT = new MultiSSVOutputter() {

        // TODO: no genotypes and SUPP_INDEX item
        @Override
        public void output(List<ISDSV> canBeMergedSSVs, ByteStream cache) {
            resetGTs();
            ISDSV first = canBeMergedSSVs.fastGet(0);
            CompleteSDSV completeSDSV = new CompleteSDSV();
            int pos, end, length;
            int maxPos = Integer.MIN_VALUE, maxEnd = Integer.MIN_VALUE, maxLen = Integer.MIN_VALUE;
            int minPos = Integer.MAX_VALUE, minEnd = Integer.MAX_VALUE, minLen = Integer.MAX_VALUE;
            float stdPos, stdEnd, stdLen;
            long sumPos = 0, sumEnd = 0, sumLen = 0, numOfSV = canBeMergedSSVs.size();
            float sumPosValueSquare = 0, sumLenValueSquare = 0, sumEndValueSquare = 0;
            IGenotypes mergedGenotypesCache = mergedGenotypes.getGtyBox().get();
            for (ISDSV canBeMergedSSV : canBeMergedSSVs) {
                int fileID = canBeMergedSSV.getFileID();
                Genotype genotype = canBeMergedSSV.getSVGenotypes().getGtyBox().get().get(0);
                // genotype
                int supports = supportRecords.fastGet(fileID);
                if (supports == 0) {
                    mergedGenotypesCache.set(fileID, genotype);
                } else {
                    Genotype lastGenotype = mergedGenotypesCache.get(fileID);
                    mergedGenotypesCache.set(
                            fileID,
                            genotype.intcode() > lastGenotype.intcode() ?
                                    genotype : lastGenotype
                    );
                }
                supportRecords.fastSet(fileID, supports + 1);
                pos = canBeMergedSSV.getPos();
                end = canBeMergedSSV.getEnd();
                length = canBeMergedSSV.length();
                minPos = Math.min(minPos, pos);
                minEnd = Math.min(minEnd, end);
                minLen = Math.min(minLen, length);

                maxPos = Math.max(pos, maxPos);
                maxEnd = Math.max(end, maxEnd);
                maxLen = Math.max(length, maxLen);

                sumPos += pos;
                sumLen += length;
                sumEnd += end;
                sumPosValueSquare += Math.pow(pos, 2);
                sumEndValueSquare += Math.pow(end, 2);
                sumLenValueSquare += Math.pow(length, 2);
            }
            pos = (int)(sumPos / numOfSV);
            end = (int)(sumEnd / numOfSV);
            length = (int)(sumLen / numOfSV);
            if (numOfSV == 1) {
                stdPos = 0;
                stdEnd = 0;
                stdLen = 0;
            } else {
                double varPos = sumPosValueSquare / numOfSV - Math.pow(pos, 2);
                stdPos = varPos <= 0 ? 0 : (float) Math.sqrt(varPos);
                double varEnd = sumEndValueSquare / numOfSV - Math.pow(end, 2);
                stdEnd = varEnd <= 0 ? 0 : (float) Math.sqrt(varEnd);
                double varLen = sumLenValueSquare / numOfSV - Math.pow(length, 2);
                stdLen = varLen <= 0 ? 0 : (float) Math.sqrt(varLen);
            }
            // end, len, type, CI_POS, CI_END, CI_LEN, STD_POS, STD_END, STD_LEN, SUPP_VEC
            fixedInfo.putByIndex(0, writeAndClear(cache, end));
            fixedInfo.putByIndex(1, writeAndClear(cache, length));
            fixedInfo.putByIndex(2, writeAndClear(cache, first.getType().getName()));
            fixedInfo.putByIndex(3, writeAndClear(cache, minPos - pos, maxPos - pos));
            fixedInfo.putByIndex(4, writeAndClear(cache, minEnd - end, maxEnd - end));
            fixedInfo.putByIndex(5, writeAndClear(cache, minLen - length, maxLen - length));
            fixedInfo.putByIndex(6, writeAndClear(cache, stdPos));
            fixedInfo.putByIndex(7, writeAndClear(cache, stdEnd));
            fixedInfo.putByIndex(8, writeAndClear(cache, stdLen));
            fixedInfo.putByIndex(9, writeAndClear(cache, supportRecords));
            completeSDSV.setCoordinate(new SVCoordinate(pos, end, first.nameOfContig()))
                    .setID(new Bytes("SDFA." + count.incrementAndGet()))
                    .setAlt(new Bytes("<" + first.getType().getName() + ">"))
                    .setInfo(fixedInfo)
                    .setGenotypes(mergedGenotypes);
            cache.write(completeSDSV.toVCFRecord(cache));
        }
    };


    void output(List<ISDSV> canBeMergedSSVs, ByteStream cache);

    static MultiSSVOutputter getByDefault(String nameOfSSVType) {
        return outputter.getOrDefault(nameOfSSVType, DEFAULT_SSV_MERGED_OUTPUT);
    }

    static void initSupportRecords(int numOfSamples) {
        for (int i = 0; i < numOfSamples; i++) {
            supportRecords.add(0);
            genotypeChangeFlag.add((byte) 0);
            mergedGenotypes.setGenotypes(new GTBox(numOfSamples, GenotypeConstant.Wild_TYPE_Homozygous));
            encodeGenotypes.add(GenotypeConstant.Wild_TYPE_Homozygous.hashCode());
        }
    }

    static Bytes writeAndClear(ByteStream cache, int value) {
        cache.write(ASCIIUtility.toASCII(value));
        Bytes transferValue = cache.toBytes().detach();
        cache.clear();
        return transferValue;
    }

    static Bytes writeAndClear(ByteStream cache, float value) {
        cache.write(ASCIIUtility.toASCII(df.format(value), Constant.CHAR_SET));
        Bytes transferValue = cache.toBytes().detach();
        cache.clear();
        return transferValue;
    }

    static Bytes writeAndClear(ByteStream cache, String value) {
        cache.write(ASCIIUtility.toASCII(value, Constant.CHAR_SET));
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

    static SVGenotypes resetGTs() {
        int size = encodeGenotypes.size();
        IGenotypes genotypes = mergedGenotypes.getGtyBox().get();
        for (int i = 0; i < size; i++) {
            genotypes.set(i, GenotypeConstant.Wild_TYPE_Homozygous);
        }
        return mergedGenotypes;
    }
}
