package edu.sysu.pmglab.sdfa.toolkit;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.ccf.CCFWriter;
import edu.sysu.pmglab.ccf.meta.CCFMeta;
import edu.sysu.pmglab.ccf.meta.CCFMetaItem;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.genotype.IGenotypes;
import edu.sysu.pmglab.gtb.genome.genotype.counter.ICounter;
import edu.sysu.pmglab.gtb.genome.genotype.encoder.Encoder;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.SDFTable;
import edu.sysu.pmglab.sdfa.base.SDFFormatManager;
import edu.sysu.pmglab.sdfa.gwas.PEDFile;
import edu.sysu.pmglab.sdfa.sv.vcf.format.GTBox;

import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;

/**
 * @author Wenjie Peng
 * @create 2025-03-08 16:02
 * @description
 */
public class SDFExtract {
    PEDFile pedFile;
    String inputSDFFile;
    String outputSDFFile;
    float maxMAF = -1;
    float minMAF = -1;
    boolean silent = false;
    Encoder encoder = new Encoder();
    BiFunction<IRecord, SDFReader, IRecord> convert;
    private LinkedSet<String> extractIndividual = new LinkedSet<>();
    List<BiFunction<IRecord, SDFReader, IRecord>> filterFunctionList;

    private SDFExtract() {
        filterFunctionList = new List<>();
    }

    public static SDFExtract of(String inputSDFFile, String pedFile, String outputSDFFile) throws IOException {
        SDFExtract res = new SDFExtract();
        if (inputSDFFile == null) {
            throw new UnsupportedOperationException("Input SDF file is not assigned.");
        }
        if (outputSDFFile == null) {
            throw new UnsupportedOperationException("Output SDF file is not assigned.");
        }
        if (pedFile != null) {
            res.pedFile = PEDFile.load(pedFile);
        }
        res.inputSDFFile = inputSDFFile;
        res.outputSDFFile = outputSDFFile;
        return res;
    }

    /**
     * @param filter
     * @return
     */
    public SDFExtract addFilter(BiFunction<IRecord, SDFReader, IRecord> filter) {
        this.filterFunctionList.add(filter);
        return this;
    }

    public SDFExtract setConvert(BiFunction<IRecord, SDFReader, IRecord> convert) {
        this.convert = convert;
        return this;
    }


    public void submit() throws IOException {
        IRecord record;
        SDFReader reader = new SDFReader(inputSDFFile);
        CCFWriter writer = CCFWriter.setOutput(new File(outputSDFFile)).addFields(reader.getRawFields()).instance();
        IntList indexOfExtractSubjects = indexesOfIndividuals(reader.getIndividuals());

        GTBox rawGTs = GTBox.instance.newInstance();
        GTBox newGTs = new GTBox(indexOfExtractSubjects.size());
        SDFFormatManager formatManager = reader.getReaderOption().getSDFTable().getFormatManager();
        while ((record = reader.readRecord()) != null) {
            rawGTs = rawGTs.decodeSelf(record.get(3));
            for (int i = 0; i < indexOfExtractSubjects.size(); i++) {
                int indexOfCurrSubject = indexOfExtractSubjects.fastGet(i);
                newGTs.loadOne(i, rawGTs.getGenotype(indexOfCurrSubject));
            }
            if (maxMAF != -1 || minMAF != -1) {
                IGenotypes genotypes = newGTs.get();
                ICounter counter = genotypes.counter();
                // ./.
                int filteredGT = counter.count(GenotypeConstant.MISSING_GTY);
                // 1/1
                int mutantGT = counter.count(GenotypeConstant.Mutant_Homozygous);
                // 0/0
                int wildGT = counter.count(GenotypeConstant.Wild_TYPE_Homozygous);
                // 0/1 or 1/0
                int heterozygousType = counter.count(GenotypeConstant.Heterozygous_0_1) + counter.count(GenotypeConstant.Heterozygous_1_0);

                int validGT = genotypes.size() - filteredGT;
                float maf = (mutantGT * 2 + heterozygousType) / (float) validGT * 2;
                if (maxMAF != -1 && maxMAF <= maf){
                    continue;
                }
                if (minMAF != -1 && minMAF >= maf){
                    continue;
                }
            }
            formatManager.replaceWithExtractedSubjects(indexOfExtractSubjects);
            record.set(3, encoder.encode(newGTs.get()));
            record.set(4, formatManager.encode());
            writer.write(record);
        }
        CCFMeta meta = reader.getReaderOption().getSDFTable().getMeta();
        meta.add(new CCFMetaItem(SDFTable.SDF_INDIVIDUAL_TAG, FieldType.stringIndexableSet, extractIndividual));
        writer.addMeta(meta);
        reader.close();
        writer.close();
    }

    private IntList indexesOfIndividuals(LinkedSet<String> individuals) {
        IntList indexesOfRaw = new IntList();
        int size = pedFile.size();
        int match = 0;
        for (int i = 0; i < size; i++) {
            String subjectName = pedFile.getUIDByIndex(i).toString();
            int currIndex = individuals.indexOf(subjectName);
            if (currIndex != -1) {
                match++;
                indexesOfRaw.add(currIndex);
                extractIndividual.add(subjectName);
            }
        }
        if (!silent) {
            Logger logger = LogBackOptions.getRootLogger();
            if (match == 0) {
                throw new UnsupportedOperationException("No subject can be map from PED file to SDF file");
            }
            logger.info("Totally collect " + size + " subjects from PED file and map " + match + " subjects in SDF.");
        }
        return indexesOfRaw;
    }

    public SDFExtract setMaxMAF(float maxMAF) {
        this.maxMAF = maxMAF;
        return this;
    }

    public SDFExtract setMinMAF(float minMAF) {
        this.minMAF = minMAF;
        return this;
    }
}
