package edu.sysu.pmglab.sdfa;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.type.FieldType;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.ICommandProgram;
import edu.sysu.pmglab.commandParser.annotation.option.Container;
import edu.sysu.pmglab.commandParser.annotation.option.Option;
import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
import edu.sysu.pmglab.container.indexable.DynamicIndexableMap;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.gtb.genome.genotype.encoder.Encoder;
import edu.sysu.pmglab.runtimecompiler.JavaCompiler;
import edu.sysu.pmglab.sdfa.toolkit.SDFRecordWrapper;
import gnu.trove.procedure.TObjectProcedure;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author Wenjie Peng
 * @create 2025-03-11 06:54
 * @description
 */
public class SDFFilter {
    final SDFReader reader;
    final SDFFilterWrapper wrapper;

    static {
        JavaCompiler.importClass(Bytes.class);
        JavaCompiler.importClass(DynamicIndexableMap.class);
    }

    IntList filterFormatIndexes = new IntList();
    List<TObjectProcedure<Object>> filterGTFunctions = new List<>();

    List<String> filterSVAttrs = new List<>();
    List<TObjectProcedure<Object>> filterSVFunctions = new List<>();


    public SDFFilter(SDFReader reader, SDFFilterBuilder builder) {
        this.reader = reader;
        this.wrapper = new SDFFilterWrapper(reader);
        // parse builder
        Logger logger = LogBackOptions.getRootLogger();
        if (builder != null) {
            if (!builder.build.get()){
                builder.build();
            }
            // register GT filter
            for (int i = 0; i < builder.filterGTFunctions.size(); i++) {
                Bytes filterGTAttr = builder.filterGTAttrs.fastGet(i);
                int index = reader.getFormatManager().indexOf(filterGTAttr);
                if (index == -1) {
                    logger.warn("The format filter item (" + filterGTAttr.toString() + ") can't be found in this file.");
                    continue;
                }
                this.filterFormatIndexes.add(index);
                this.filterGTFunctions.add(builder.filterGTFunctions.fastGet(i));
            }
            // register SV filter
            for (int i = 0; i < builder.filterSVFunctions.size(); i++) {
                String filterGTAttr = builder.filterSVAttrs.fastGet(i);
                this.filterSVAttrs.add(filterGTAttr);
                this.filterSVFunctions.add(builder.filterSVFunctions.fastGet(i));
            }
        }

    }

    public void accept(IRecord record) {
        this.wrapper.init(record);
    }

    public IRecord filter(IRecord record) {
        accept(record);
        return filter();
    }


    private IRecord filter() {
        for (int i = 0; i < filterSVAttrs.size(); i++) {
            String filterAttr = filterSVAttrs.fastGet(i);
            TObjectProcedure<Object> filterFunction = filterSVFunctions.fastGet(i);
            if (!filterSV(filterAttr, filterFunction)) {
                return null;
            }
        }
        for (int i = 0; i < filterFormatIndexes.size(); i++) {
            int formatIndex = filterFormatIndexes.fastGet(i);
            TObjectProcedure objectBooleanFunction = filterGTFunctions.fastGet(i);
            filterGT(formatIndex, objectBooleanFunction);
        }
        wrapper.overwrite();
        return wrapper.getRecord();
    }

    public boolean filterGT(int indexOfFormatAttr, TObjectProcedure filter) {
        return wrapper.filterGT(indexOfFormatAttr, filter);
    }

    public boolean filterSV(String attr, TObjectProcedure filter) {
        return wrapper.filterSV(attr, filter);
    }

    static class SDFFilterWrapper extends SDFRecordWrapper {
        boolean dropInfo = false;
        boolean dropMetric = false;
        private List<Bytes> emptyInfoList;
        private List<Bytes> emptyFormatList;

        private Encoder encoder = new Encoder();

        public SDFFilterWrapper(SDFReader reader) {
            super(reader);
            this.emptyInfoList = List.wrap(new Bytes[reader.getReaderOption().getSDFTable().getInfoManager().getInfoKeys().size()]);
            this.emptyFormatList = List.wrap(new Bytes[reader.getReaderOption().getSDFTable().getFormatManager().getFormatAttrNameList().size()]);
        }

        public void overwrite() {
            if (modifyGT) {
                record.set(3, encoder.encode(modifiedGT.get()));
            }
            if (dropMetric) {
                record.set(4, emptyFormatList);
            }
            if (dropInfo) {
                record.set(10, emptyInfoList);
            }
        }

    }

    @Parser(
            usage = "<filter_type> [options]",
            usage_item = {
                    @UsageItem(key = "API", value = "edu.sysu.pmglab.sdfa.SDFFilterBuilder"),
                    @UsageItem(key = "About", value = "Build a comprehensive filter for SDF file.")
            }
    )
    public static class SDFFilterBuilder extends ICommandProgram {
        @Option(names = {"--filter-gty"}, type = FieldType.string, container = Container.LIST, repeated = true)
        @OptionUsage(description = "--filter-gty <format_attr> <function>")
        List<List<String>> filterGtyParams;

        @Option(names = {"--filter-sv"}, type = FieldType.string, container = Container.LIST, repeated = true)
        @OptionUsage(description = "--filter-sv <sv_attr> <function>")
        List<List<String>> filterSVParams;

        List<Bytes> filterGTAttrs = new List<>();
        List<TObjectProcedure<Object>> filterGTFunctions = new List<>();

        List<String> filterSVAttrs = new List<>();
        List<TObjectProcedure<Object>> filterSVFunctions = new List<>();

        AtomicBoolean build = new AtomicBoolean(false);

        public SDFFilterBuilder() {

        }

        public SDFFilterBuilder build() {
            if (build.get()){
                return this;
            }
            if (filterGtyParams != null && !filterGtyParams.isEmpty()) {
                for (List<String> filterGtyParam : filterGtyParams) {
                    filterGTAttrs.add(new Bytes(filterGtyParam.fastGet(0)));
                    filterGTFunctions.add(
                            JavaCompiler.procedure(
                                    filterGtyParam.fastGet(1),
                                    JavaCompiler.Param.of("value", Object.class)
                            )
                    );
                }
            }
            if (filterSVParams != null && !filterSVParams.isEmpty()) {
                for (List<String> filterSVParam : filterSVParams) {
                    filterSVAttrs.add(filterSVParam.fastGet(0));
                    JavaCompiler.importClass(DynamicIndexableMap.class);
                    filterSVFunctions.add(
                            JavaCompiler.procedure(
                                    filterSVParam.fastGet(1),
                                    JavaCompiler.Param.of("value", Object.class)
                            )
                    );
                }
            }
            build.set(true);
            return this;
        }
    }

    public static void main(String[] args) throws IOException {
        String filterParams = "--filter-sv QUAL ((Bytes)value).toInt()==47 " +
                "--filter-gty DR (int)value>10";
        SDFFilterBuilder sdfFilterBuilder = new SDFFilterBuilder();
        CommandOptions parse = sdfFilterBuilder.parse(filterParams.split(" "));
        SDFReader sdfReader = new SDFReader("/Users/wenjiepeng/Desktop/SDFA3.0/nagf/analyze/Exophthalmos_Glaucoma/sniffles/sdf_4.0_analyze/annotation/DM18A2265.vcf.gz.sdf");
        SDFFilter sdfFilter = new SDFFilter(sdfReader, sdfFilterBuilder.build());
        IRecord record = sdfReader.readRecord();
        IRecord filter = sdfFilter.filter(record);
        int a = 1;
    }

}
