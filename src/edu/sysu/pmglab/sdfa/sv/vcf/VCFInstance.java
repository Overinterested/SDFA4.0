package edu.sysu.pmglab.sdfa.sv.vcf;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.wrapper.MemoryBytesSplitter;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.sdfa.sv.*;
import edu.sysu.pmglab.sdfa.sv.bnd.DetailedBND;
import edu.sysu.pmglab.sdfa.sv.csv.SharedFieldComplexSV;
import edu.sysu.pmglab.sdfa.sv.vcf.calling.AbstractCallingParser;
import edu.sysu.pmglab.sdfa.sv.vcf.calling.ICallingType;
import edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.gty.GenotypeFilterManager;
import edu.sysu.pmglab.sdfa.sv.vcf.quantitycontrol.sv.SVLevelFilterManager;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2025-02-02 05:36
 * @description
 */
public class VCFInstance {
    private int fileID;
    private SVContig contig;
    private final LiveFile vcfFile;
    private boolean parsed = false;
    private boolean emptyVCF = false;
    private VCFInfoManager infoManager;
    private final String vcfCallingName;
    private final LinkedSet<String> individuals;
    private VCFFormatManager formatManager;
    private SVFilterManager svFilterManager;
    private final VCFHeaderManager headerManager;
    private SDSVConversionManager conversionFromSV2Record;
    private final SharedFieldComplexSV sharedFieldComplexSV = new SharedFieldComplexSV();
    private static final SVTypeSign BND_TYPE = SVTypeSign.getByName(new Bytes("BND"));

    public static final Bytes HEADER_COLUMN = new Bytes(new byte[]{
            // #CHR
            Constant.NUMBER_SIGN, Constant.C, Constant.H, Constant.R, Constant.TAB,
            // POS
            Constant.P, Constant.O, Constant.S, Constant.TAB,
            // ID
            Constant.I, Constant.D, Constant.TAB,
            // REF
            Constant.R, Constant.E, Constant.F, Constant.TAB,
            // ALT
            Constant.A, Constant.L, Constant.T, Constant.TAB,
            // QUAL
            Constant.Q, Constant.U, Constant.A, Constant.L, Constant.TAB,
            // FILTER
            Constant.F, Constant.I, Constant.L, Constant.T, Constant.E, Constant.R, Constant.TAB,
            // INFO
            Constant.I, Constant.N, Constant.F, Constant.O, Constant.TAB,
            // FORMAT
            Constant.F, Constant.O, Constant.R, Constant.M, Constant.A, Constant.T, Constant.TAB
    });


    public VCFInstance(LiveFile vcfFile, String vcfCallingName) {
        this.vcfFile = vcfFile;
        this.individuals = new LinkedSet<>();
        this.headerManager = new VCFHeaderManager();
        this.vcfCallingName = vcfCallingName == null ? "vcf4.3" : vcfCallingName;
    }

    public VCFInstance parse() throws IOException {
        if (parsed) {
            return this;
        }
        int indexOfFile = -1;
        ByteStream cache = new ByteStream();
        ReaderStream reader = vcfFile.openAsText();
        AbstractCallingParser parser = ICallingType.get(vcfCallingName).getParser();
        while (reader.readline(cache) != -1) {
            Bytes line = cache.toBytes();
            if (line.byteAt(0) == Constant.NUMBER_SIGN) {
                headerManager.parse(cache.toBytes());
                cache.clear();
                continue;
            }
            break;
        }
        if (cache.length() == 0) {
            emptyVCF = true;
            return this;
        }

        initFields();
        int subjectSize = individuals.size();
        SVGenotypes svGenotypes = null;
        // region init vcf variables
        Bytes id, ref, alt, qual, filter;
        List<SVCoordinate> coordinateList = new List<>();
        SVCoordinate initCoordinate = new SVCoordinate(-1, -1, -1);
        coordinateList.add(initCoordinate);
        // filter
        boolean globalFilter, gtyFilter, svLevelFilter;
        globalFilter = svFilterManager != null && svFilterManager.filter();
        gtyFilter = globalFilter && svFilterManager.filterGty();
        svLevelFilter = globalFilter && svFilterManager.filterSV();
        GenotypeFilterManager genotypeFilterManager = gtyFilter ? svFilterManager.getGenotypeFilterManager() : null;
        SVLevelFilterManager svLevelFilterManager = svLevelFilter ? svFilterManager.getSVLevelFilterManager() : null;
        // endregion
        boolean load;
        int indexOfColInLine;
        MemoryBytesSplitter splitter = new MemoryBytesSplitter(Constant.TAB);
        boolean skipLine = false;
        do {
            indexOfFile++;
            indexOfColInLine = 0;
            Bytes line = cache.toBytes();
            splitter.initVCFSplitter(line);
            splitter.clearMemory();
            while (splitter.hasNext()) {
                Bytes value = splitter.next();
                if (indexOfColInLine == 1) {
                    // pos
                    initCoordinate.setIndexOfChr(contig.getContigIndexByName(splitter.fastGetInVCF(0).toString()))
                            .setPos(value.toInt());
                    indexOfColInLine++;
                    continue;
                }
                if (indexOfColInLine == 7) {
                    // info attribute
                    infoManager.parse(value);
                    load = parser.parseInfo(infoManager, coordinateList, contig);
                    if (infoManager.getType() == BND_TYPE) {
                        DetailedBND parse = DetailedBND.parse(splitter.fastGetInVCF(4));
                        int otherBNDPosition = parse.getOtherBNDPosition();
                        if (coordinateList.size() == 2) {
                            if (coordinateList.fastGet(1).getPos() == -1) {
                                if (otherBNDPosition != -1) {
                                    coordinateList.fastGet(1)
                                            .setIndexOfChr(contig.getContigIndexByName(parse.getOtherBNDContig()))
                                            .setPos(otherBNDPosition);
                                } else {
                                    coordinateList.popLast();
                                }
                            }
                        }
                    }
                    coordinateList.sort(SVCoordinate::compareTo);
                    if (!load) {
                        cache.clear();
                        skipLine = true;
                        continue;
                    }
                }
                if (indexOfColInLine == 8) {
                    // format
                    int indexOfFormat = formatManager.getFormatIndex(value);
                    indexOfFormat = subjectSize == 0 ? -1 : indexOfFormat;
                    svGenotypes = SVGenotypes.newInstance(splitter, subjectSize, formatManager, indexOfFormat, genotypeFilterManager);
                    break;
                }
                indexOfColInLine++;
            }

            id = splitter.fastGetInVCF(2);
            ref = splitter.fastGetInVCF(3);
            alt = splitter.fastGetInVCF(4);
            qual = splitter.fastGetInVCF(5);
            filter = splitter.fastGetInVCF(6);
            // filter sv
            if (svLevelFilter) {
                sharedFieldComplexSV.setFileID(fileID)
                        .setGenotypes(svGenotypes)
                        .setID(id)
                        .setRef(ref)
                        .setAlt(alt)
                        .setQuality(qual)
                        .setFilter(filter)
                        .setLength(infoManager.getLen())
                        .setCoordinates(coordinateList)
                        .setInfo(infoManager.getIndexedInfoFields());
                boolean filterSV = svLevelFilterManager.filter(sharedFieldComplexSV);
                if (!filterSV) {
                    cache.clear();
                    coordinateList.clear();
                    coordinateList.add(initCoordinate);
                    continue;
                }
            }
            // convert and store svs
            conversionFromSV2Record.parseToRecordsAndStore(
                    id, ref, alt, qual, filter, infoManager, svGenotypes,
                    coordinateList, infoManager.getLen(), indexOfFile
            );
            // count contig for sorting and indexing
            for (SVCoordinate coordinate : coordinateList) {
                contig.countContigByIndex(coordinate.getIndexOfChr());
            }
            cache.clear();
            coordinateList.clear();
            coordinateList.add(initCoordinate);
        } while (reader.readline(cache) != -1);
        reader.close();
        conversionFromSV2Record.close(this);
        parsed = true;
        return this;
    }

    // preload the fields in annotation lines of VCF
    private void initFields() {
        if (contig == null) {
            contig = SVContig.init();
        }
        List<String> contigNameSet = headerManager.getContigNameSet();
        for (String contigName : contigNameSet) {
            contig.addContigName(contigName);
        }
        if (!headerManager.getSubjectNameSet().isEmpty()) {
            List<String> subjectNameSet = headerManager.getSubjectNameSet();
            for (int i = 0; i < subjectNameSet.size(); i++) {
                if (subjectNameSet.fastGet(i).equals("NULL")) {
                    subjectNameSet.fastSet(i, vcfFile.getPath() + "_subject_" + (i + 1));
                }
            }
            individuals.addAll(headerManager.getSubjectNameSet());
        }
        this.infoManager = VCFInfoManager.init(headerManager.getInfoNameSet());
        if (svFilterManager != null && svFilterManager.getGenotypeFilterManager() != null && svFilterManager.getGenotypeFilterManager().filterBuiltinGty()) {
            this.formatManager = VCFFormatManager.loadFilterFields(new LinkedSet<>(svFilterManager.getGenotypeFilterManager().getFixedGtyFilterNameSet().apply(Bytes::new)));
        } else {
            formatManager = VCFFormatManager.loadFilterFields(null);
        }
        this.formatManager.addFormatFields(headerManager.getFormatNameSet());
    }

    public VCFInstance storeHeader(boolean storeHeader) {
        this.headerManager.store = storeHeader;
        return this;
    }

    public VCFInstance setFileID(int fileID) {
        this.fileID = fileID;
        return this;
    }

    public int getFileID() {
        return fileID;
    }

    public SVContig getContig() {
        return contig;
    }

    public IndexableSet<String> getIndividuals() {
        return individuals;
    }

    public VCFInfoManager getInfoManager() {
        return infoManager;
    }

    public VCFFormatManager getFormatManager() {
        return formatManager;
    }

    public VCFHeaderManager getHeaderManager() {
        return headerManager;
    }

    public VCFInstance setConversionFromSV2Record(SDSVConversionManager conversionFromSV2Record) {
        this.conversionFromSV2Record = conversionFromSV2Record;
        return this;
    }

    public VCFInstance setSVFilterManager(SVFilterManager svFilterManager) {
        this.svFilterManager = svFilterManager;
        return this;
    }

    public static void main(String[] args) throws IOException {
        long l = System.currentTimeMillis();
        VCFInstance vcfInstance = new VCFInstance(
                LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/data/normal/vcf/M669-2_filt_centromere.vcf.gz"),
                "vcf4.3"
        );
        SVFilterManager.SVFilterManagerBuilder svFilterManagerBuilder = new SVFilterManager.SVFilterManagerBuilder();
        svFilterManagerBuilder.parse("--filter-size 50 --filter-info sv.get(\"PRECISE\")!=null".split(" "));
        vcfInstance.svFilterManager = null;
        vcfInstance.conversionFromSV2Record = new SDSVConversionManager();
        vcfInstance.conversionFromSV2Record.initWriter(new File("/Users/wenjiepeng/Desktop/tmp/a.sdf"));
        vcfInstance.parse();
        System.out.println(System.currentTimeMillis() - l);
    }

    public boolean isEmptyVCF() {
        return emptyVCF;
    }

    public void clear() {
        if (infoManager != null) {
            infoManager.clear();
        }
        if (formatManager != null) {
            formatManager.clear();
        }
        if (headerManager != null) {
            headerManager.clear();
        }
    }
}
