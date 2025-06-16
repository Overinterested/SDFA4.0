//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.RuntimeProperty;
//import edu.sysu.pmglab.ccf.type.FieldType;
//import edu.sysu.pmglab.commandParser.CommandOptions;
//import edu.sysu.pmglab.commandParser.ICommandProgram;
//import edu.sysu.pmglab.commandParser.annotation.option.CustomOption;
//import edu.sysu.pmglab.commandParser.annotation.option.DynamicOption;
//import edu.sysu.pmglab.commandParser.annotation.option.Option;
//import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
//import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
//import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
//import edu.sysu.pmglab.commandParser.converter.FileSizeConverter;
//import edu.sysu.pmglab.commandParser.converter.IConverter;
//import edu.sysu.pmglab.commandParser.exception.ParameterException;
//import edu.sysu.pmglab.commandParser.validator.range.Int_1_RangeValidator;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.executor.ThreadQueue;
//import edu.sysu.pmglab.io.FileUtils;
//import edu.sysu.pmglab.io.file.FileType;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.file.LocalFile;
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
//import java.util.AbstractMap;
//import java.util.Map;
//import java.util.regex.Pattern;
//
//@Parser(usage = "build <input> <input> ... [options]",
//        usage_item = {@UsageItem(key = "About", value = "Package multiple files into a single output file.")})
//class BuildProgram extends ICommandProgram {
//    private static final String REGEX = "^\\s*(\\d+(?:\\.\\d+)?)\\s*([BKMGTP]B?)$";
//    private static final Pattern pattern = Pattern.compile(REGEX);
//
//    /**
//     * 日志系统
//     */
//    private final static Logger logger = LoggerFactory.getLogger(BuildProgram.class);
//
//    /**
//     * 指定读取文件
//     */
//    @CustomOption(names = {"build"}, converter = InputConverter.class, arity = {1, -1}, required = true)
//    @OptionUsage(
//            description = {"Specify files to package.",
//                    "If the path is a local directory, all sub-files within the directory will be included in the archive.",
//                    "If no tag is specified, the tag defaults to the file name."},
//            format = "build <path> <tag>=<path> ...")
//    List<Map.Entry<String, LiveFile>> inputs;
//
//    /**
//     * 输出文件
//     */
//    @Option(names = {"--output", "-o"}, type = FieldType.file)
//    @OptionUsage(description = "Specify the output file path (without extension).", format = "--output <file>", defaultTo = "./archive")
//    File output = new File(RuntimeProperty.WORKSPACE_PATH, "archive");
//
//    /**
//     * 追加数据
//     */
//    @Option(names = {"--append", "-a"}, type = FieldType.NULL)
//    @OptionUsage(description = "Append packed entries to the output file. If not specified, the output file will be overwritten.")
//    boolean append = false;
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
//    @DynamicOption(names = {"--filter", "-f"}, args = {"path=", "name=", "size="}, repeated = true)
//    @OptionUsage(description = {"Apply filters to select or exclude files within the archive based on specified criteria. Use the following sub-parameters:",
//            "* path: Retain file whose full path matches the specified pattern.",
//            "* name: Retain file whose file name matches the specified pattern.",
//            "* size: Retain file within a specific size range, using comparison operators (>, >=, <, <=, ==, !=) to define the range. Specify the size with a unit (B, KB, MB, GB, TB, PB), which is case-insensitive.",
//            "Note:",
//            "- For path and name, use '*' or '{}' as wildcards to match any sequence of characters.",
//            "- Prefix any sub-parameter with ! to exclude files that match the pattern."},
//            format = "--filter [path] [name] [size]")
//    List<Map<String, String>> filter = null;
//    /**
//     * 使用/禁用索引表
//     */
//    @Option(names = {"--disable-idx"}, type = FieldType.NULL)
//    @OptionUsage(description = "Suppress the construction of the external indexer after the task is completed.")
//    boolean buildIndex = true;
//    /**
//     * 静默执行
//     */
//    @Option(names = {"--silent", "-s"}, type = FieldType.NULL)
//    @OptionUsage(description = "Suppress terminal output logs.")
//    boolean silent = false;
//
//    public static void main(String[] args) throws IOException {
//        BuildProgram program = new BuildProgram();
//        CommandOptions options = program.parse(args.length == 1 && args[0].equals("build") ? new String[]{"--help"} : args);
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
//        List<List<Map.Entry<String, LiveFile>>> partTasks = part(program);
//        if (partTasks.size() == 0) {
//            // 结束
//            Packer packer = new Packer(program.blocker == null ? program.output + ".pack" : program.output + "_1.pack", program.append);
//            packer.close(program.buildIndex);
//        } else if (partTasks.size() == 1) {
//            Packer packer = new Packer(program.blocker == null ? program.output + ".pack" : program.output + "_1.pack", program.append);
//            for (List<Map.Entry<String, LiveFile>> tasks : partTasks) {
//                for (Map.Entry<String, LiveFile> task : tasks) {
//                    ProgressBar bar = new ProgressBar.Builder()
//                            .setInitialMax(task.getValue().length())
//                            .setConsumer(program.silent ? ProgressConsumer.SILENT : new ConsoleConsumer())
//                            .setRenderer(new BarProgressRenderer("Archiving", DataLengthUnit.B))
//                            .build();
//                    packer.setListening(bar);
//                    String md5 = packer.append(task.getKey(), task.getValue());
//                    bar.close();
//                    bar.print("MD5 (packed://" + packer.getFile() + ":" + task.getKey() + ") = " + md5 + "\n\n");
//                }
//            }
//            packer.close(program.buildIndex);
//        } else {
//            // 并行打包, 此时计算总大小
//            long totalSize = 0;
//            for (List<Map.Entry<String, LiveFile>> tasks : partTasks) {
//                for (Map.Entry<String, LiveFile> task : tasks) {
//                    totalSize += task.getValue().length();
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
//                        Packer packer = new Packer(program.output + "_" + (index + 1) + ".pack", program.append).setListening(bar);
//                        List<Map.Entry<String, LiveFile>> tasks = partTasks.fastGet(index);
//                        for (Map.Entry<String, LiveFile> task : tasks) {
//                            packer.append(task.getKey(), task.getValue());
//                        }
//                        packer.close(program.buildIndex);
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
//    private static List<List<Map.Entry<String, LiveFile>>> part(BuildProgram program) {
//        List<TObjectProcedure<LiveFile>> filters = PackerProgram.getFilter(program.filter);
//
//        TLongIntProcedure blocker;
//        if (program.blocker == null) {
//            blocker = (length, count) -> false;
//        } else {
//            long maxLength = program.blocker.get("size").length() > 0 ? FileSizeConverter.INSTANCE.convert("--block", program.blocker.get("size")) : Long.MAX_VALUE;
//            int maxCount = program.blocker.get("count").length() > 0 ? Math.max(Integer.parseInt(program.blocker.get("count")), 1) : Integer.MAX_VALUE;
//            blocker = (l, i) -> l >= maxLength || i >= maxCount;
//        }
//
//        List<List<Map.Entry<String, LiveFile>>> tasks = new List<>();
//
//        int count = 0;
//        long length = 0;
//        List<Map.Entry<String, LiveFile>> task = new List<>();
//
//        next:
//        for (int i = 0; i < program.inputs.size(); i++) {
//            Map.Entry<String, LiveFile> input = program.inputs.fastGet(i);
//            for (TObjectProcedure<LiveFile> filter : filters) {
//                if (!filter.execute(input.getValue())) {
//                    continue next;
//                }
//            }
//
//            count += 1;
//            length += input.getValue().length();
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
//
//    /**
//     * 输入数据转换器
//     */
//    enum InputConverter implements IConverter<List<Map.Entry<String, LiveFile>>> {
//        INSTANCE;
//
//        @Override
//        public List<Map.Entry<String, LiveFile>> convert(String name, String... values) {
//            List<Map.Entry<String, LiveFile>> container = new List<>();
//            for (String value : values) {
//                int index = value.indexOf("=");
//                try {
//                    if (index == -1 || index == 0) {
//                        if (index == 0) {
//                            value = value.substring(1);
//                        }
//                        if (LiveFile.getProtocol(value) == FileType.LOCAL) {
//                            // 本地文件要小心文件夹
//                            File file = new File(value);
//                            if (file.exists() && file.isDirectory()) {
//                                // 文件夹, 遍历子文件
//                                String prefix = file.getCanonicalPath();
//                                if (!prefix.endsWith("/")) {
//                                    prefix += "/";
//                                }
//                                for (File subFile : FileUtils.listFiles(file)) {
//                                    container.add(new AbstractMap.SimpleImmutableEntry<>(subFile.getCanonicalPath().substring(prefix.length()), new LocalFile(subFile)));
//                                }
//                            } else {
//                                container.add(new AbstractMap.SimpleImmutableEntry<>(file.getName(), new LocalFile(file)));
//                            }
//                        } else {
//                            LiveFile file = LiveFile.of(value);
//                            container.add(new AbstractMap.SimpleImmutableEntry<>(file.getName(), file));
//                        }
//                    } else {
//                        String tag = value.substring(0, index);
//                        if (LiveFile.getProtocol(value) == FileType.LOCAL) {
//                            // 本地文件要小心文件夹
//                            File file = new File(value.substring(index + 1));
//                            if (file.exists() && file.isDirectory()) {
//                                // 文件夹, 遍历子文件
//                                String prefix = file.getCanonicalPath();
//                                if (!prefix.endsWith("/")) {
//                                    prefix += "/";
//                                }
//                                for (File subFile : FileUtils.listFiles(file)) {
//                                    container.add(new AbstractMap.SimpleImmutableEntry<>(tag + "/" + subFile.getCanonicalPath().substring(prefix.length()), new LocalFile(subFile)));
//                                }
//                            } else {
//                                container.add(new AbstractMap.SimpleImmutableEntry<>(tag, new LocalFile(file)));
//                            }
//                        } else {
//                            LiveFile file = LiveFile.of(value.substring(index + 1));
//                            container.add(new AbstractMap.SimpleImmutableEntry<>(tag, file));
//                        }
//                    }
//                } catch (IOException e) {
//                    throw new ParameterException(e);
//                }
//            }
//            return container;
//        }
//    }
//}
