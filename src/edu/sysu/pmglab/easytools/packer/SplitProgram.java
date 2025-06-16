//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.RuntimeProperty;
//import edu.sysu.pmglab.ccf.type.FieldType;
//import edu.sysu.pmglab.commandParser.CommandOptions;
//import edu.sysu.pmglab.commandParser.ICommandProgram;
//import edu.sysu.pmglab.commandParser.annotation.option.DynamicOption;
//import edu.sysu.pmglab.commandParser.annotation.option.Option;
//import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
//import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
//import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
//import edu.sysu.pmglab.commandParser.converter.FileSizeConverter;
//import edu.sysu.pmglab.commandParser.validator.range.Int_1_RangeValidator;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.executor.ThreadQueue;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.progressbar.BarProgressRenderer;
//import edu.sysu.pmglab.progressbar.ProgressBar;
//import edu.sysu.pmglab.progressbar.ProgressConsumer;
//import edu.sysu.pmglab.progressbar.console.ConsoleConsumer;
//import edu.sysu.pmglab.progressbar.unit.DataLengthUnit;
//import gnu.trove.procedure.TLongIntProcedure;
//import gnu.trove.procedure.TObjectProcedure;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Map;
//
//@Parser(usage = "split <input> [options]",
//        usage_item = {@UsageItem(key = "About", value = "Split a packed archive into multiple archives based on specified criteria.")})
//class SplitProgram extends ICommandProgram {
//    /**
//     * 日志系统
//     */
//    private final static Logger logger = LoggerFactory.getLogger(SplitProgram.class);
//
//    /**
//     * 指定读取文件
//     */
//    @Option(names = {"split"}, type = FieldType.livefile, required = true)
//    LiveFile input;
//    /**
//     * 输出文件
//     */
//    @Option(names = {"--output", "-o"}, type = FieldType.file)
//    @OptionUsage(description = "Specify the output file path (without extension).", format = "--output <file>", defaultTo = "./archive")
//    File output = new File(RuntimeProperty.WORKSPACE_PATH, "archive");
//    /**
//     * 并行线程数
//     */
//    @Option(names = {"--threads", "-t"}, type = FieldType.int32, validator = Int_1_RangeValidator.class, defaultTo = "4")
//    @OptionUsage(description = "Configure the number of concurrent threads.", format = "--threads <int, >=1>", defaultTo = "4")
//    int threads = 4;
//    /**
//     * 分块
//     */
//    @DynamicOption(names = {"--block", "-b"}, args = {"size=", "count="})
//    @OptionUsage(
//            format = "--block [size] [count]",
//            description = {"Specify the maximum archive size and the maximum number of entries per archive. When either limit is exceeded, a new archive will be created.",
//                    "* size: Maximum archive size (>= 1), with a case-insensitive unit (B, KB, MB, GB, TB, PB).",
//                    "* count: Maximum number of entries per archive (>= 1)."})
//    Map<String, String> blocker = null;
//
//    /**
//     * 过滤方法
//     */
//    @DynamicOption(names = {"--filter", "-f"}, args = {"name=", "size="}, repeated = true)
//    @OptionUsage(description = {"Apply filters to select or exclude files within the archive based on specified criteria. Use the following sub-parameters:",
//            "* name: Retain file whose file name matches the specified pattern.",
//            "* size: Retain file within a specific size range, using comparison operators (>, >=, <, <=, ==, !=) to define the range. Specify the size with a unit (B, KB, MB, GB, TB, PB), which is case-insensitive.",
//            "Note:",
//            "- For name, use '*' or '{}' as wildcards to match any sequence of characters.",
//            "- Prefix any sub-parameter with ! to exclude files that match the pattern."},
//            format = "--filter [path] [name] [size]")
//    List<Map<String, String>> filter = null;
//    /**
//     * 使用/禁用索引表
//     */
//    @Option(names = {"--disable-idx"}, type = FieldType.NULL)
//    @OptionUsage(description = "Suppress the loading and construction of the external indexer during task execution.")
//    boolean indexable = true;
//    /**
//     * 静默执行
//     */
//    @Option(names = {"--silent", "-s"}, type = FieldType.NULL)
//    @OptionUsage(description = "Suppress terminal output logs.")
//    boolean silent = false;
//
//    public static void main(String[] args) throws IOException {
//        SplitProgram program = new SplitProgram();
//        CommandOptions options = program.parse(args.length == 1 && args[0].equals("split") ? new String[]{"--help"} : args);
//
//        if (options.isHelp()) {
//            logger.info("\n{}", options.usage());
//            return;
//        }
//
//        if (!program.silent) {
//            logger.info("\n{}", options);
//        }
//
//        List<List<PackedEntry>> partTasks = part(program);
//        if (partTasks.size() == 0) {
//            // 结束
//            Packer packer = new Packer(program.output + "_1.pack", false);
//            packer.close(program.indexable);
//        } else if (partTasks.size() == 1) {
//            Packer packer = new Packer(program.output + "_1.pack", false);
//            for (List<PackedEntry> tasks : partTasks) {
//                for (PackedEntry task : tasks) {
//                    ProgressBar bar = new ProgressBar.Builder()
//                            .setInitialMax(task.length())
//                            .setConsumer(program.silent ? ProgressConsumer.SILENT : new ConsoleConsumer())
//                            .setRenderer(new BarProgressRenderer("Archiving", DataLengthUnit.B))
//                            .build();
//                    packer.setListening(bar);
//                    String md5 = packer.append(task);
//                    bar.close();
//                    bar.print("MD5 (packed://" + packer.getFile() + ":" + task.getName() + ") = " + md5 + "\n\n");
//                }
//            }
//            packer.close(program.indexable);
//        } else {
//            // 并行打包, 此时计算总大小
//            long totalSize = 0;
//            for (List<PackedEntry> tasks : partTasks) {
//                for (PackedEntry task : tasks) {
//                    totalSize += task.length();
//                }
//            }
//
//            try (ThreadQueue queue = new ThreadQueue(Math.min(program.threads, partTasks.size()));
//                 ProgressBar bar = new ProgressBar.Builder()
//                         .setInitialMax(totalSize)
//                         .setConsumer(program.silent ? ProgressConsumer.SILENT : new ConsoleConsumer())
//                         .setRenderer(new BarProgressRenderer("Archiving", DataLengthUnit.B))
//                         .build()) {
//
//                for (int i = 0; i < partTasks.size(); i++) {
//                    final int index = i;
//                    queue.addTask((status, context) -> {
//                        Packer packer = new Packer(program.output + "_" + (index + 1) + ".pack", false).setListening(bar);
//                        List<PackedEntry> tasks = partTasks.fastGet(index);
//                        for (PackedEntry task : tasks) {
//                            packer.append(task);
//                        }
//                        packer.close(program.indexable);
//                    });
//                }
//
//                queue.await();
//            }
//        }
//    }
//
//    /**
//     * 任务分块
//     */
//    private static List<List<PackedEntry>> part(SplitProgram program) throws IOException {
//        List<TObjectProcedure<LiveFile>> filters = PackerProgram.getFilter(program.filter);
//
//        TLongIntProcedure blocker;
//        if (program.blocker == null) {
//            blocker = (length, count) -> false;
//        } else {
//            long maxLength = program.blocker.get("size").length() > 0 ? FileSizeConverter.INSTANCE.convert("--block", program.blocker.get("size")) : Long.MAX_VALUE;
//            int maxCount = program.blocker.get("count").length() > 0 ? Math.max(Integer.parseInt(program.blocker.get("count")), 1) : Integer.MAX_VALUE;
//            blocker = (l, c) -> l >= maxLength || c >= maxCount;
//        }
//
//        List<List<PackedEntry>> tasks = new List<>();
//
//        int count = 0;
//        long length = 0;
//        List<PackedEntry> task = new List<>();
//
//        next:
//        for (PackedEntry input : new PackedFile(program.input, program.indexable)) {
//            for (TObjectProcedure<LiveFile> filter : filters) {
//                if (!filter.execute(input)) {
//                    continue next;
//                }
//            }
//
//            count += 1;
//            length += input.length();
//            task.add(input);
//
//            if (blocker.execute(length, count)) {
//                // 需要分块
//                tasks.add(task);
//                task = new List<>();
//                length = 0;
//                count = 0;
//            }
//        }
//
//        if (task.size() > 0) {
//            tasks.add(task);
//        }
//
//        return tasks;
//    }
//}
