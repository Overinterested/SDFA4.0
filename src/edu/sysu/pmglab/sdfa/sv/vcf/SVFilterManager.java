package edu.sysu.pmglab.sdfa.sv.vcf;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Container;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
import edu.sysu.pmglab.commandParser.validator.range.Int_0_RangeValidator;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.gtb.genome.genotype.Genotype;
import edu.sysu.pmglab.gtb.genome.genotype.IGenotypes;
import edu.sysu.pmglab.gtb.genome.genotype.counter.ICounter;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.runtimecompiler.JavaCompiler;
import edu.sysu.pmglab.sdfa.sv.SVGenotypes;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;
import edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.gty.GenotypeFilterManager;
import edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.gty.HardyWeinbergCalculator;
import edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.sv.SVLevelFilterManager;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2024-12-27 01:57
 * @description
 */
public class SVFilterManager {
    private final boolean filterSV;
    private final boolean filterGty;

    SVLevelFilterManager svLevelFilterManager;
    GenotypeFilterManager genotypeFilterManager;

    public SVFilterManager(boolean filterSV, boolean filterGty) {
        this.filterSV = filterSV;
        this.filterGty = filterGty;
    }

    public SVFilterManager(boolean filterSV, boolean filterGty,
                           SVLevelFilterManager svLevelFilterManager,
                           GenotypeFilterManager genotypeFilterManager) {
        this.filterSV = filterSV;
        this.filterGty = filterGty;
        this.svLevelFilterManager = svLevelFilterManager;
        this.genotypeFilterManager = genotypeFilterManager;
    }

    public static class SVFilterManagerBuilder extends ICommandProgram {
        @Option(names = {"--store-contig-file"}, type = FieldType.file, container = Container.NONE)
        File validContigFile;

        @Option(names = {"--filter-size"}, type = FieldType.string)
        String filterSize;

        @Option(names = {"--store-sv-types"}, type = FieldType.string, container = Container.LIST)
        List<String> validSVTypes;

        @Option(names = {"--filter-gty"}, type = FieldType.string, container = Container.LIST, repeated = true)
        @OptionUsage(description = "--filter-gty <field_name> <function>")
        List<List<String>> filterGtyFunctions;

        @Option(names = {"--filter-info"}, type = FieldType.string, repeated = true)
        List<String> filterInfos;

        // regular built-in genotype filter
        @Option(names = {"--gty-dp"}, type = FieldType.varInt32, validator = Int_0_RangeValidator.class)
        int minDp = 0;

        @Option(names = {"--gty-qual"}, type = FieldType.varInt32, validator = Int_0_RangeValidator.class)
        int minGQ = 0;

        @Option(names = {"--gty-sec-pl"}, type = FieldType.varInt32, validator = Int_0_RangeValidator.class)
        int minPhredscaledLikelihood = 0;

        // regular built-in SV filter
        @Option(names = {"--hwe"}, type = FieldType.float16)
        float hweThreshold;

        @Option(names = {"--min-obs-rate"}, type = FieldType.float16)
        float minObsRate;

        @Option(names = {"--filter-gty-null"}, type = FieldType.NULL)
        boolean dropFilterNullGty = false;

        // parse item
        boolean filterSV;
        boolean filterGty;

        int validContigCounter = 0;
        HashSet<String> validContigNameSet;

        int locationFilterCounter = 0;
        IntInterval locationFilterInterval;

        int validTypeFilterCounter = 0;
        HashSet<SVTypeSign> validSVTypeSet;

        IntList fixedGtyFilterCounter;
        List<String> fixedGtyFilterNameSet;
        List<Function<String, Boolean>> fixedGtyFilterList;

        IntList fieldFilterCounter;
        List<Function<ISDSV, Boolean>> fieldFilterList;

        IntList extraFilterCounter;
        List<Function<ISDSV, Boolean>> extraFilterList;

        public SVFilterManagerBuilder() {
        }

        public SVFilterManager build() throws IOException {
            if (validContigFile != null) {
                ByteStream cache = new ByteStream();
                LiveFile contigFile = LiveFile.of(validContigFile);
                ReaderStream readerStream = contigFile.openAsText();
                HashSet<String> validContigNameSet = new HashSet<>();
                while (readerStream.readline(cache) != -1) {
                    validContigNameSet.add(cache.toString());
                }
                Logger logger = LogBackOptions.getRootLogger();
                if (validContigNameSet.isEmpty()) {
                    logger.warn("Contig file used for filter SVs is empty.");
                } else {
                    logger.info("Contig file used for filter SVs contains " + validContigNameSet.size() + " valid contigs.");
                }
                this.filterSV = true;
                this.validContigNameSet = validContigNameSet;
            }
            if (filterSize != null) {
                String[] lengthThreshold = filterSize.split(",");
                if (lengthThreshold.length != 0) {
                    int min = Integer.parseInt(lengthThreshold[0].trim());
                    int max = Integer.MAX_VALUE;
                    if (lengthThreshold.length == 2) {
                        max = Integer.parseInt(lengthThreshold[1].trim());
                    }
                    this.filterSV = true;
                    this.locationFilterInterval = new IntInterval(min, max);
                }
            }
            if (validSVTypes != null) {
                HashSet<SVTypeSign> validSVTypeSet = new HashSet<>();
                for (String validSVType : validSVTypes) {
                    validSVTypeSet.add(SVTypeSign.add(validSVType));
                }
                this.filterSV = true;
                this.validSVTypeSet = validSVTypeSet;
            }

            //region regular built-in gty filter
            List<String> filterGtyNames = new List<>();
            List<Function<String, Boolean>> filterGtyFunctions = new List<>();
            String compareIntFunction = "value.equals(\".\")?true:Integer.parseInt(value) >= min";
            if (minDp != 0) {
                filterGtyNames.add("DP");
                filterGtyFunctions.add(
                        JavaCompiler.eval(
                                compareIntFunction.replace("min", String.valueOf(minDp)),
                                JavaCompiler.Param.of("value", String.class),
                                JavaCompiler.Param.of(Boolean.class)
                        )
                );
                this.filterGty = true;
            }
            if (minGQ != 0) {
                filterGtyNames.add("GQ");
                filterGtyFunctions.add(
                        JavaCompiler.eval(
                                compareIntFunction.replace("min", String.valueOf(minGQ)),
                                JavaCompiler.Param.of("value", String.class),
                                JavaCompiler.Param.of(Boolean.class)
                        )
                );
                this.filterGty = true;
            }

            if (minPhredscaledLikelihood != 0) {
                filterGtyNames.add("PL");
                String plCompare = "value.equals(\".,.,.\")?true:(new IntList(List.wrap(value.split(\",\")).apply(Integer::parseInt)).sort().fastGet(1)) >= min";
                JavaCompiler.importClass(List.class);
                JavaCompiler.importClass(IntList.class);
                filterGtyFunctions.add(
                        JavaCompiler.eval(
                                plCompare.replace("min", String.valueOf(minPhredscaledLikelihood)),
                                JavaCompiler.Param.of("value", String.class),
                                JavaCompiler.Param.of(Boolean.class)
                        )
                );
                this.filterGty = true;
            }
            //endregion

            // filter genotypes
            if (this.filterGtyFunctions != null && !this.filterGtyFunctions.isEmpty()) {
                for (List<String> filterGtyFunction : this.filterGtyFunctions) {
                    if (filterGtyFunction.size() != 2) {
                        throw new UnsupportedOperationException("The param, '--filter-gty', only accepts two params as input: The first is name and the second is function.");
                    }
                    filterGtyNames.add(filterGtyFunction.fastGet(0));
                    filterGtyFunctions.add(JavaCompiler.eval(
                            filterGtyFunction.fastGet(1),
                            JavaCompiler.Param.of("value", Object.class),
                            JavaCompiler.Param.of(Boolean.class)
                    ));
                }
            }

            if (!filterGtyNames.isEmpty()) {
                this.filterGty = true;
                this.fixedGtyFilterNameSet = filterGtyNames;
                this.fixedGtyFilterList = filterGtyFunctions;
                this.fixedGtyFilterCounter = IntList.wrap(this.fixedGtyFilterNameSet.size());
            }

            // filter info
            if (filterInfos != null && !filterInfos.isEmpty()) {
                List<Function<ISDSV, Boolean>> functions = new List<>();
                for (String filterInfo : filterInfos) {
                    functions.add(
                            JavaCompiler.eval(
                                    filterInfo,
                                    JavaCompiler.Param.of("sv", ISDSV.class),
                                    JavaCompiler.Param.of(Boolean.class)
                            )
                    );
                }
                this.filterSV = true;
                this.fieldFilterList = functions;
                this.fieldFilterCounter = IntList.wrap(new int[functions.size()]);
            }

            // SV filter
            List<Function<ISDSV, Boolean>> extraFilterList = new List<>();
            if (dropFilterNullGty) {
                extraFilterList.add(
                        sdsv -> {
                            SVGenotypes svGenotypes = sdsv.getSVGenotypes();
                            if (svGenotypes == null) {
                                return false;
                            }
                            IGenotypes genotypes = svGenotypes.getGtyBox().get();
                            if (genotypes == null || genotypes.size() == 0) {
                                return false;
                            }
                            int size = genotypes.size();
                            for (int i = 0; i < size; i++) {
                                Genotype genotype = genotypes.get(i);
                                if (genotype.left() >= 1 || genotype.right() >= 1) {
                                    return true;
                                }
                            }
                            return false;
                        }
                );
            }
            if (hweThreshold != 0) {
                extraFilterList.add(
                        sdsv -> {
                            SVGenotypes svGenotypes = sdsv.getSVGenotypes();
                            if (svGenotypes == null) {
                                return false;
                            }
                            IGenotypes genotypes = svGenotypes.getGtyBox().get();
                            if (genotypes == null || genotypes.size() == 0) {
                                return false;
                            }
                            ICounter counter = genotypes.counter();
                            return HardyWeinbergCalculator.calculate(
                                    // 0/0
                                    counter.count(GenotypeConstant.Wild_TYPE_Homozygous),
                                    // 0/1
                                    counter.count(GenotypeConstant.Wild_TYPE_Homozygous),
                                    // 1/1
                                    counter.count(GenotypeConstant.Wild_TYPE_Homozygous),
                                    false
                                    ) > hweThreshold;
                        }
                );
            }
            if (minObsRate != 0) {
                extraFilterList.add(
                        sdsv -> {
                            SVGenotypes svGenotypes = sdsv.getSVGenotypes();
                            if (svGenotypes == null) {
                                return false;
                            }
                            IGenotypes genotypes = svGenotypes.getGtyBox().get();
                            int num = 0, size;
                            if (genotypes == null || (size = genotypes.size()) == 0) {
                                return false;
                            }
                            for (Genotype genotype : genotypes) {
                                if (genotype == GenotypeConstant.MISSING_GTY) {
                                    num++;
                                }
                            }
                            return num / (float) size < minObsRate;
                        }
                );
            }
            // extended SV filter
            if (this.extraFilterList != null && !this.extraFilterList.isEmpty()) {
                extraFilterList.addAll(this.extraFilterList);
            }
            if (!extraFilterList.isEmpty()) {
                int size = extraFilterList.size();
                this.extraFilterCounter = IntList.wrap(new int[size]);
                this.extraFilterList = extraFilterList;
                filterSV = true;
            }
            SVFilterManager instance = new SVFilterManager(
                    filterSV, filterGty,
                    new SVLevelFilterManager(
                            filterSV, validContigCounter, validContigNameSet,
                            locationFilterCounter, locationFilterInterval,
                            validTypeFilterCounter, validSVTypeSet,
                            fieldFilterCounter, fieldFilterList,
                            extraFilterCounter, this.extraFilterList
                    ),
                    new GenotypeFilterManager(filterGty, fixedGtyFilterCounter, filterGtyNames, fixedGtyFilterList)
            );
            return instance.filter() ? instance : null;
        }

        public static void main(String[] args) throws IOException {
            SVFilterManagerBuilder svFilterManagerBuilder = new SVFilterManagerBuilder();
            String[] command = ("--store-contig-file /Users/wenjiepeng/Desktop/tmp/storeContig.txt " +
                    "--filter-size 30 " +
                    "--store-sv-types ins del " +
                    "--gty-dp 10 " +
                    "--gty-qual 20 " +
                    "--filter-gty Integer.parseInt(gty.get(\"DP\"))>10 " +
                    "--filter-gty Integer.parseInt(gty.get(\"GQ\"))>10 " +
                    "--filter-info sv.get(\"QUAL\").toString().equals(\"1\") " +
                    "--filter-info sv.get(\"FILTER\").toString().equals(\"1\")").split(" ");
            CommandOptions options = svFilterManagerBuilder.parse(command);
            SVFilterManager build = svFilterManagerBuilder.build();
        }
    }


    public boolean filter() {
        return filterSV || filterGty;
    }

    public SVFilterManager newInstance() {
        return new SVFilterManager(filterSV, filterGty)
                .setGenotypeFilterManager(genotypeFilterManager.newInstance())
                .setSVLevelFilterManager(svLevelFilterManager.newInstance());

    }

    public boolean filterSV() {
        return svLevelFilterManager.filter();
    }

    public boolean filterGty() {
        return filterGty;
    }

    public SVLevelFilterManager getSVLevelFilterManager() {
        return svLevelFilterManager;
    }

    public GenotypeFilterManager getGenotypeFilterManager() {
        return genotypeFilterManager;
    }

    private SVFilterManager setSVLevelFilterManager(SVLevelFilterManager svLevelFilterManager) {
        this.svLevelFilterManager = svLevelFilterManager;
        return this;
    }

    private SVFilterManager setGenotypeFilterManager(GenotypeFilterManager genotypeFilterManager) {
        this.genotypeFilterManager = genotypeFilterManager;
        return this;
    }

    public void clear() {

    }
}
