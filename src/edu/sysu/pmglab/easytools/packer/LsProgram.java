//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.ccf.type.FieldType;
//import edu.sysu.pmglab.commandParser.CommandOptions;
//import edu.sysu.pmglab.commandParser.ICommandProgram;
//import edu.sysu.pmglab.commandParser.annotation.option.Available;
//import edu.sysu.pmglab.commandParser.annotation.option.DynamicOption;
//import edu.sysu.pmglab.commandParser.annotation.option.Option;
//import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
//import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
//import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
//import edu.sysu.pmglab.container.interval.IntInterval;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.progressbar.unit.DataLengthUnit;
//import edu.sysu.pmglab.utils.StringFormatter;
//import gnu.trove.procedure.TObjectProcedure;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.text.DecimalFormatSymbols;
//import java.util.Comparator;
//import java.util.Locale;
//import java.util.Map;
//
//@Parser(usage = "ls <file> [options]",
//        usage_item = {@UsageItem(key = "About", value = "Generate a detailed listing of subparts within the specified packed archive.")})
//class LsProgram extends ICommandProgram {
//    /**
//     * 日志系统
//     */
//    private final static Logger logger = LoggerFactory.getLogger(LsProgram.class);
//    /**
//     * 指定操作的文件
//     */
//    @Option(names = {"ls"}, type = FieldType.livefile, required = true)
//    LiveFile file;
//    /**
//     * 范围
//     */
//    @Option(names = {"--index-range", "-ir"}, type = FieldType.intInterval)
//    @OptionUsage(description  = {"Retrieve packed entries within a specified range of record indices from 'min' (inclusive) to 'max' (exclusive)."},
//            format = "--index-range <min>~<max>")
//    IntInterval indexRange = new IntInterval(0, Integer.MAX_VALUE);
//
//    /**
//     * 过滤方法
//     */
//    @DynamicOption(names = {"--filter"}, args = {"name=", "size="}, repeated = true)
//    @OptionUsage(description  = {"Apply filters to select or exclude files within the archive based on specified criteria. Use the following sub-parameters:",
//            "* name: Retain file whose file name matches the specified pattern.",
//            "* size: Retain file within a specific size range, using comparison operators (>, >=, <, <=, ==, !=) to define the range. Specify the size with a unit (B, KB, MB, GB, TB, PB), which is case-insensitive.",
//            "Note:",
//            "- For name, use '*' or '{}' as wildcards to match any sequence of characters.",
//            "- Prefix any sub-parameter with ! to exclude files that match the pattern."},
//            format = "--filter [name] [size]")
//    List<Map<String, String>> filter = null;
//
//    /**
//     * 排序
//     */
//    @Option(names = "--sort", available = @Available(value = {"RANGE", "SIZE", "NAME", "TIME", "R", "S", "N", "T",
//            "-RANGE", "-SIZE", "-NAME", "-TIME", "-R", "-S", "-N", "-T"}, upper = true))
//    @OptionUsage(description  = {"Sort the output based on the specified criteria. The available sorting options are: ",
//            "* RANGE (R): Sort the entries by the data range, based on the starting position of each packed entry (default).",
//            "* SIZE (S): Sort the entries by file size, from smallest to largest.",
//            "* NAME (N): Sort the entries alphabetically by file name (ASCII order).",
//            "* TIME (T): Sort the entries by the last modification time, from the oldest to the newest.",
//            "Note: You can reverse the sort order by prefixing the criteria with '-'."},
//            defaultTo = "RANGE",
//            format = "--sort [-]<RANGE/SIZE/NAME/TIME>")
//    String sort = "RANGE";
//
//    /**
//     * 显示规则
//     */
//    @OptionUsage(description  = {"Format output using a custom template. If no text is specified after '--format', a summary of the file information will be printed.",
//            "The following placeholders are supported for substitution:\n" +
//                    "* {NAME} - The name of the packed entry.\n" +
//                    "* {PATH} - The full path to the file.\n" +
//                    "* {SIZE} - The size of the packed entry.\n" +
//                    "* {SIZE.F} - The formatted size of the packed entry.\n" +
//                    "* {LAST_MODIFY_TIME} - The last modification time of the packed entry.\n" +
//                    "* {LAST_MODIFY_TIME.F} - The formatted last modification time of the packed entry.\n" +
//                    "* {RANGE} - The data range the packed entry.\n" +
//                    "* {MD5} - The md5 checksum of the packed entry."
//    },
//            defaultTo = "{NAME}\\t{SIZE.F}\\t{LAST_MODIFY_TIME.F}\\t{MD5}",
//            format = "--format [rule]")
//    List<String> rule = List.wrap("{NAME}\t{SIZE.F}\t{LAST_MODIFY_TIME.F}\t{MD5}");
//    /**
//     * 使用/禁用索引表
//     */
//    @Option(names = {"--disable-idx"}, type = FieldType.NULL)
//    @OptionUsage(description  = "Suppress loading packed file details from the external indexer.")
//    boolean loadIndex = true;
//
//    public static void main(String[] args) throws IOException {
//        LsProgram program = new LsProgram();
//        CommandOptions options = program.parse(args.length == 1 && args[0].equals("ls") ? new String[]{"--help"} : args);
//
//        if (options.isHelp()) {
//            logger.info("\n{}", options.usage());
//            return;
//        }
//
//        // 文件信息
//        PackedFile files = new PackedFile(program.file, program.loadIndex, program.indexRange).listFiles(new TObjectProcedure<PackedEntry>() {
//            final List<TObjectProcedure<LiveFile>> filters = PackerProgram.getFilter(program.filter);
//
//            @Override
//            public boolean execute(PackedEntry file) {
//                for (TObjectProcedure<LiveFile> filter : filters) {
//                    if (!filter.execute(file)) {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        });
//
//        // --sort [RANGE/SIZE/NAME/TIME]
//        switch (program.sort) {
//            case "RANGE":
//            case "R":
//                files = files.sort(Comparator.comparing(PackedEntry::range));
//                break;
//            case "-RANGE":
//            case "-R":
//                files = files.sort((f1, f2) -> -f1.range().compareTo(f2.range()));
//                break;
//            case "SIZE":
//            case "S":
//                files = files.sort(Comparator.comparingLong(PackedEntry::length));
//                break;
//            case "-SIZE":
//            case "-S":
//                files = files.sort(new Comparator<PackedEntry>() {
//                    final Comparator<PackedEntry> comparator = Comparator.comparingLong(PackedEntry::length);
//
//                    @Override
//                    public int compare(PackedEntry o1, PackedEntry o2) {
//                        return -comparator.compare(o1, o2);
//                    }
//                });
//                break;
//            case "NAME":
//            case "N":
//                files = files.sort(new Comparator<PackedEntry>() {
//                    final Comparator<String> naturalOrderComparator = Comparator.naturalOrder();
//
//                    @Override
//                    public int compare(PackedEntry o1, PackedEntry o2) {
//                        return naturalOrderComparator.compare(o1.tag, o2.tag);
//                    }
//                });
//                break;
//            case "-NAME":
//            case "-N":
//                files = files.sort(new Comparator<PackedEntry>() {
//                    final Comparator<String> naturalOrderComparator = Comparator.naturalOrder();
//
//                    @Override
//                    public int compare(PackedEntry o1, PackedEntry o2) {
//                        return -naturalOrderComparator.compare(o1.tag, o2.tag);
//                    }
//                });
//                break;
//            case "TIME":
//            case "T":
//                files = files.sort(Comparator.comparingLong(PackedEntry::lastModifyTime));
//                break;
//            case "-TIME":
//            case "-T":
//                files = files.sort(new Comparator<PackedEntry>() {
//                    final Comparator<PackedEntry> comparator = Comparator.comparingLong(PackedEntry::lastModifyTime);
//
//                    @Override
//                    public int compare(PackedEntry o1, PackedEntry o2) {
//                        return -comparator.compare(o1, o2);
//                    }
//                });
//                break;
//        }
//
//        if (program.rule.size() > 0) {
//            StringFormatter formatter = new StringFormatter(program.rule.fastGet(0));
//            // 打印标题
//            formatter.format("NAME", "NAME")
//                    .format("PATH", "PATH")
//                    .format("SIZE", "SIZE")
//                    .format("SIZE.F", "SIZE")
//                    .format("LAST_MODIFY_TIME", "LAST_MODIFY_TIME")
//                    .format("LAST_MODIFY_TIME.F", "LAST_MODIFY_TIME")
//                    .format("RANGE", "RANGE")
//                    .format("MD5", "MD5");
//            System.out.println(formatter.toString());
//
//            for (PackedEntry entry : files) {
//                formatter.format("NAME", entry.getName())
//                        .format("PATH", entry.getPath())
//                        .format("SIZE", entry.length())
//                        .format("SIZE.F", entry.formatLength(null))
//                        .format("LAST_MODIFY_TIME", entry.lastModifyTime())
//                        .format("LAST_MODIFY_TIME.F", entry.formatLastModifyTime(null))
//                        .format("RANGE", entry.range() == null ? "-" : entry.range())
//                        .format("MD5", entry.md5());
//
//                System.out.println(formatter.toString());
//            }
//        } else {
//            StringFormatter summary = new StringFormatter("{COUNT} files, {SIZE} total")
//                    .format("COUNT", files.numOfFiles())
//                    .format("SIZE", DataLengthUnit.B.convert(files.length(), new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US))));
//            System.out.println(summary.toString());
//        }
//    }
//}
