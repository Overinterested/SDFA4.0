//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.RuntimeProperty;
//import edu.sysu.pmglab.ccf.type.FieldType;
//import edu.sysu.pmglab.commandParser.CommandOptions;
//import edu.sysu.pmglab.commandParser.ICommandProgram;
//import edu.sysu.pmglab.commandParser.annotation.option.Available;
//import edu.sysu.pmglab.commandParser.annotation.option.Container;
//import edu.sysu.pmglab.commandParser.annotation.option.DynamicOption;
//import edu.sysu.pmglab.commandParser.annotation.option.Option;
//import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
//import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
//import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
//import edu.sysu.pmglab.commandParser.validator.range.Int_1_RangeValidator;
//import edu.sysu.pmglab.container.interval.IntInterval;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.executor.ThreadQueue;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.reader.ISeekableReaderStream;
//import edu.sysu.pmglab.io.writer.ChannelWriterStream;
//import edu.sysu.pmglab.io.writer.IWriterStream;
//import edu.sysu.pmglab.progressbar.BarProgressRenderer;
//import edu.sysu.pmglab.progressbar.ProgressBar;
//import edu.sysu.pmglab.progressbar.ProgressConsumer;
//import edu.sysu.pmglab.progressbar.console.ConsoleConsumer;
//import edu.sysu.pmglab.progressbar.unit.DataLengthUnit;
//import gnu.trove.procedure.TObjectProcedure;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.AbstractMap;
//import java.util.Map;
//
//@Parser(usage = "export <input> <input> ... [options]",
//        usage_item = {@UsageItem(key = "About", value = "Extract files from packed archives.")})
//class ExportProgram extends ICommandProgram {
//    /**
//     * 日志系统
//     */
//    private final static Logger logger = LoggerFactory.getLogger(ExportProgram.class);
//
//    /**
//     * 指定读取文件
//     */
//    @Option(names = {"export"}, type = FieldType.livefile, required = true, container = Container.LIST)
//    List<LiveFile> inputs;
//
//    /**
//     * 输出文件夹
//     */
//    @Option(names = {"--output", "-o"}, type = FieldType.file)
//    @OptionUsage(description  = "Specify the output directory.", format = "--output <dir>", defaultTo = ".")
//    File outputDir = RuntimeProperty.WORKSPACE_PATH;
//
//    /**
//     * 输出文件映射
//     */
//    @DynamicOption(names = "--map-output", arbitrary = true)
//    @OptionUsage(description  = "Reassign new paths for extracted files. The paths are relative to the '--output' directory.",
//            format = "--map-output <tag>=<path> <tag>=<path> ...")
//    Map<String, String> mapper = null;
//
//    /**
//     * 并行线程数
//     */
//    @Option(names = {"--threads", "-t"}, type = FieldType.int32, validator = Int_1_RangeValidator.class, defaultTo = "4")
//    @OptionUsage(description  = "Configure the number of concurrent threads.", format = "--threads <int, >=1>", defaultTo = "1")
//    int threads = 1;
//
//    /**
//     * 如果文件已经存在, 如何处理
//     */
//    @Option(names = {"--exists", "-e"}, available = @Available(value = {"SKIP", "REPLACE", "KEEP_NEWEST", "STOP"}, upper = true), defaultTo = "SKIP")
//    @OptionUsage(
//            defaultTo = "SKIP",
//            format = "--exists [SKIP/REPLACE/KEEP_NEWEST/STOP]",
//            description = "Specify the action to take when encountering files with the same name during extraction.")
//    String exists = "SKIP";
//
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
//    @DynamicOption(names = {"--filter", "-f"}, args = {"name=", "size="}, repeated = true)
//    @OptionUsage(description  = {"Apply filters to select or exclude files within the archive based on specified criteria. Use the following sub-parameters:",
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
//    @OptionUsage(description  = "Suppress loading packed file details from the external indexer.")
//    boolean loadIndex = true;
//    /**
//     * 静默执行
//     */
//    @Option(names = {"--silent", "-s"}, type = FieldType.NULL)
//    @OptionUsage(description  = "Suppress terminal output logs.")
//    boolean silent = false;
//
//    public static void main(String[] args) throws IOException {
//        ExportProgram program = new ExportProgram();
//        CommandOptions options = program.parse(args.length == 1 && args[0].equals("export") ? new String[]{"--help"} : args);
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
//        List<Map.Entry<PackedEntry, File>> tasks = part(program);
//        if (tasks.size() == 0) {
//            // 结束
//            program.outputDir.mkdirs();
//        } else if (tasks.size() == 1 || program.threads == 1) {
//            byte[] cache = new byte[81920];
//            out:
//            for (Map.Entry<PackedEntry, File> task : tasks) {
//                if (task.getValue().exists()) {
//                    // 如果输出文件已经存在
//                    switch (program.exists) {
//                        case "SKIP":
//                            continue out;
//                        case "REPLACE":
//                            break;
//                        case "KEEP_NEWEST":
//                            if (task.getKey().lastModifyTime() <= task.getValue().lastModified()) {
//                                continue out;
//                            }
//                            break;
//                        case "STOP":
//                            break out;
//                    }
//                }
//
//                // 创建父文件夹
//                task.getValue().getParentFile().mkdirs();
//                ProgressBar bar = new ProgressBar.Builder()
//                        .setInitialMax(task.getKey().length())
//                        .setConsumer(program.silent ? ProgressConsumer.SILENT : new ConsoleConsumer())
//                        .setRenderer(new BarProgressRenderer("Retrieving", DataLengthUnit.B))
//                        .build();
//                ISeekableReaderStream reader = task.getKey().openAsBinary();
//                IWriterStream writer = new ChannelWriterStream(task.getValue());
//                int len;
//                while ((len = reader.read(cache)) != -1) {
//                    writer.write(cache, 0, len);
//                    bar.step(len);
//                }
//                bar.close();
//                reader.close();
//                writer.close();
//                bar.print("MD5 (" + task.getValue() + ") = " + task.getKey().md5() + "\n\n");
//            }
//        } else {
//            // 并行解包, 此时计算总大小
//            long totalSize = 0;
//            for (Map.Entry<PackedEntry, File> task : tasks) {
//                totalSize += task.getKey().length();
//            }
//
//            ProgressBar bar = new ProgressBar.Builder()
//                    .setInitialMax(totalSize)
//                    .setConsumer(program.silent ? ProgressConsumer.SILENT : new ConsoleConsumer())
//                    .setRenderer(new BarProgressRenderer("Retrieving", DataLengthUnit.B))
//                    .build();
//            try (ThreadQueue queue = new ThreadQueue(Math.min(program.threads, tasks.size()))) {
//                out:
//                for (int i = 0; i < tasks.size(); i++) {
//                    final int index = i;
//
//                    Map.Entry<PackedEntry, File> task = tasks.fastGet(index);
//                    if (task.getValue().exists()) {
//                        // 如果输出文件已经存在
//                        switch (program.exists) {
//                            case "SKIP":
//                                continue out;
//                            case "KEEP_NEWEST":
//                                if (task.getKey().lastModifyTime() <= task.getValue().lastModified()) {
//                                    continue out;
//                                }
//                                break;
//                            case "STOP":
//                                break out;
//                        }
//                    }
//
//                    task.getValue().getParentFile().mkdirs();
//                    queue.addTask((status, context) -> {
//                        Map.Entry<PackedEntry, File> task1 = tasks.fastGet(index);
//                        byte[] cache = new byte[81920];
//
//                        ISeekableReaderStream reader = task1.getKey().openAsBinary();
//                        IWriterStream writer = new ChannelWriterStream(task1.getValue());
//                        int len;
//                        while ((len = reader.read(cache)) != -1) {
//                            writer.write(cache, 0, len);
//                            bar.step(len);
//                        }
//                        reader.close();
//                        writer.close();
//                    });
//                }
//
//                queue.await();
//            }
//            bar.close();
//        }
//    }
//
//    /**
//     * 任务分块
//     */
//    private static List<Map.Entry<PackedEntry, File>> part(ExportProgram program) throws IOException {
//        List<TObjectProcedure<LiveFile>> filters = PackerProgram.getFilter(program.filter);
//
//        List<Map.Entry<PackedEntry, File>> tasks = new List<>();
//        int pointer = 0;
//        for (int i = 0; i < program.inputs.size(); i++) {
//            PackedFile input = new PackedFile(program.inputs.fastGet(i), program.loadIndex, new IntInterval(program.indexRange.start() - pointer, program.indexRange.end() - pointer));
//            next:
//            for (PackedEntry entry : input) {
//                if (program.indexRange.contains(pointer++, false)) {
//                    for (TObjectProcedure<LiveFile> filter : filters) {
//                        if (!filter.execute(entry)) {
//                            continue next;
//                        }
//                    }
//
//                    if (program.mapper == null || !program.mapper.containsKey(entry.getName())) {
//                        tasks.add(new AbstractMap.SimpleImmutableEntry<>(entry, new File(program.outputDir, entry.getName())));
//                    } else {
//                        tasks.add(new AbstractMap.SimpleImmutableEntry<>(entry, new File(program.outputDir, program.mapper.get(entry.getName()))));
//                    }
//                }
//            }
//        }
//
//        return tasks;
//    }
//}
