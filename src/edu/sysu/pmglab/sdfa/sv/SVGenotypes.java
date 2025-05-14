package edu.sysu.pmglab.sdfa.sv;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.easytools.wrapper.MemoryBytesSplitter;
import edu.sysu.pmglab.gtb.genome.genotype.Genotype;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFFormatManager;
import edu.sysu.pmglab.sdfa.sv.vcf.format.FormatAttrBox;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;
import edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.gty.GenotypeFilterManager;

/**
 * @author Wenjie Peng
 * @create 2024-12-21 01:42
 * @description
 */
public class SVGenotypes {
    private GTBox genotypes;
    private List<Bytes> encodedAttrs;

    private static boolean dropMetric = false;

    public SVGenotypes(GTBox genotypes, List<Bytes> encodedAttrs) {
        this.genotypes = genotypes;
        this.encodedAttrs = encodedAttrs;
    }

    public SVGenotypes(GTBox genotypes) {
        this.genotypes = genotypes;
    }


    public SVGenotypes(Bytes encodedGtys) {
        genotypes = GTBox.instance.decode(encodedGtys);
    }

    /**
     * TODO: modify as the next function: newInstance()
     *
     * @param memorySplitter
     * @param vcfFormatManager
     * @param indexOfFormat
     * @return
     */
    public SVGenotypes refresh(MemoryBytesSplitter memorySplitter, VCFFormatManager vcfFormatManager, int indexOfFormat, GenotypeFilterManager genotypeFilterManager) {
        genotypes.clear();
        int indexOfCurrFormatAttr;
        BytesSplitter splitter = new BytesSplitter(Constant.COLON);
        List<FormatAttrBox> attrsBoxListInCache = vcfFormatManager.getAttrsBoxListInCache(indexOfFormat);

        // init parameters
        IntList indexesOfFilter = vcfFormatManager.getIndexesOfFilter(indexOfFormat);
        boolean filter = indexesOfFilter != null && genotypeFilterManager != null && genotypeFilterManager.filter();

        int sampleIndex = 0;
        boolean filterCurrGty;
        while (memorySplitter.hasNext()) {
            filterCurrGty = false;
            Bytes currSubject = memorySplitter.next();
            splitter.init(currSubject);
            Genotype genotype = splitter.next().toGenotype();
            // first must be GT
            indexOfCurrFormatAttr = 1;
            while (splitter.hasNext()) {
                Bytes fieldValue = splitter.next();
                if (!dropMetric) {
                    // store metrics
                    attrsBoxListInCache.fastGet(indexOfCurrFormatAttr).loadOne(fieldValue);
                }
                // filter
                if (!filterCurrGty && filter) {
                    int indexOfFilterFunction = indexesOfFilter.fastGet(indexOfCurrFormatAttr);
                    if (indexOfFilterFunction == -1) {
                        indexOfCurrFormatAttr++;
                        continue;
                    }
                    filterCurrGty = !genotypeFilterManager.filter(indexOfFilterFunction, fieldValue);
                    if (filterCurrGty) {
                        genotype = GenotypeConstant.MISSING_GTY;
                    }
                }
                // break if drop all metrics
                if (filterCurrGty && dropMetric) {
                    break;
                }
                indexOfCurrFormatAttr++;
            }
            genotypes.loadOne(sampleIndex++, genotype);
        }
        encodedAttrs = dropMetric ? GenotypeConstant.EMPTY_GTY_METRIC : vcfFormatManager.encode();
        return this;
    }


    /**
     * parse the genotype string and obtain a SVGenotypes instance
     *
     * @param formatManager         the global format instance in VCF format
     * @param indexOfFormat         the corresponding format index
     * @param memorySplitter        the genotype strings in Bytes
     * @param genotypeFilterManager the genotype filter manager
     */
    public static SVGenotypes newInstance(MemoryBytesSplitter memorySplitter, int sampleSize, VCFFormatManager formatManager, int indexOfFormat, GenotypeFilterManager genotypeFilterManager) {
        int indexOfCurrFormatAttr;
        BytesSplitter splitter = new BytesSplitter(Constant.COLON);
        List<FormatAttrBox> attrsBoxListInCache = formatManager.getAttrsBoxListInCache(indexOfFormat);
        formatManager.checkEncodeAttrs(attrsBoxListInCache.size());
        GTBox genotypes = (GTBox) attrsBoxListInCache.fastGet(0);
        genotypes.init(sampleSize);
        // init parameters
        IntList indexesOfFilter = formatManager.getIndexesOfFilter(indexOfFormat);
        boolean filter = indexesOfFilter != null && genotypeFilterManager != null && genotypeFilterManager.filter();

        int sampleIndex = 0;
        boolean filterCurrGty;
        while (memorySplitter.hasNext()) {
            filterCurrGty = false;
            Bytes currSubject = memorySplitter.next();
            splitter.init(currSubject);
            Genotype genotype = splitter.next().toGenotype();
            // first attr must be gty
            indexOfCurrFormatAttr = 1;
            // loop for format attribute
            while (splitter.hasNext()) {
                Bytes fieldValue = splitter.next();
                if (!dropMetric) {
                    // store metrics
                    attrsBoxListInCache.fastGet(indexOfCurrFormatAttr).loadOne(fieldValue);
                }
                // filter
                if (!filterCurrGty && filter) {
                    int indexOfFilterFunction = indexesOfFilter.fastGet(indexOfCurrFormatAttr);
                    if (indexOfFilterFunction == -1) {
                        indexOfCurrFormatAttr++;
                        continue;
                    }

                    filterCurrGty = !genotypeFilterManager.filter(indexOfFilterFunction, fieldValue);
                    if (filterCurrGty) {
                        genotype = GenotypeConstant.MISSING_GTY;
                    }
                }
                // skip loop if drop all metrics
                if (filterCurrGty && dropMetric) {
                    break;
                }
                indexOfCurrFormatAttr++;
            }
            genotypes.loadOne(sampleIndex, genotype.intcode());
            sampleIndex++;
            if (sampleIndex == sampleSize) {
                break;
            }
        }
        if (!dropMetric) {
            List<Bytes> encodeAttrs = formatManager.encode();
            return new SVGenotypes(genotypes, encodeAttrs);
        }
        return new SVGenotypes(genotypes, GenotypeConstant.EMPTY_GTY_METRIC);
    }

    public GTBox getGtyBox() {
        return genotypes;
    }

    public void clear() {
        genotypes.clear();
    }

    public int numOfSamples() {
        return genotypes.size();
    }

    public Genotype getGty(int index) {
        return genotypes.getGenotype(index);
    }

    public static void dropMetric(boolean dropMetric) {
        SVGenotypes.dropMetric = dropMetric;
    }

    public List<Bytes> getEncodedAttrs() {
        if (encodedAttrs.isEmpty()){
            return GenotypeConstant.EMPTY_GTY_METRIC;
        }
        return encodedAttrs;
    }

    public SVGenotypes setEncodedAttrs(List<Bytes> encodedAttrs) {
        this.encodedAttrs = encodedAttrs;
        return this;
    }

    public SVGenotypes setGenotypes(GTBox genotypes) {
        this.genotypes = genotypes;
        return this;
    }
}
