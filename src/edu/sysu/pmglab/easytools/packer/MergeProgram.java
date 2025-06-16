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
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.progressbar.BarProgressRenderer;
//import edu.sysu.pmglab.progressbar.ProgressBar;
//import edu.sysu.pmglab.progressbar.ProgressConsumer;
//import edu.sysu.pmglab.progressbar.console.ConsoleConsumer;
//import edu.sysu.pmglab.progressbar.unit.DataLengthUnit;
//import gnu.trove.procedure.TObjectProcedure;
//import gnu.trove.set.hash.THashSet;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.*;
//
//@Parser(usage = "merge <input> <input> ... [options]",
//        usage_item = {@UsageItem(key = "About", value = "Merge multiple packed archives into a single output file.")})
//class MergeProgram extends ICommandProgram {
//    /**
//     * 日志系统
//     */
//    private final static Logger logger = LoggerFactory.getLogger(MergeProgram.class);
//
//    /**
//     * 指定读取文件
//     */
//    @Option(names = {"merge"}, type = FieldType.livefile, required = true, container = Container.LIST)
//    List<LiveFile> inputs;
//
//    /**
//     * 输出文件
//     */
//    @Option(names = {"--output", "-o"}, type = FieldType.file)
//    @OptionUsage(description  = "Specify the output file path (without extension).", format = "--output <file>", defaultTo = "./archive")
//    File output = new File(RuntimeProperty.WORKSPACE_PATH, "archive");
//    /**
//     * 如果文件已经存在, 如何处理
//     */
//    @Option(names = {"--exists", "-e"}, available = @Available(value = {"SKIP", "REPLACE", "KEEP_NEWEST", "STOP"}, upper = true))
//    @OptionUsage(
//            defaultTo = "SKIP",
//            format = "--exists [SKIP/REPLACE/KEEP_NEWEST/STOP]",
//            description = "Specify the action to take when encountering files with the same name during merging.")
//    String exists = "SKIP";
//    /**
//     * 追加数据
//     */
//    @Option(names = {"--append", "-a"}, type = FieldType.NULL)
//    @OptionUsage(description  = "Append packed entries to the output file. If not specified, the output file will be overwritten.")
//    boolean append = false;
//    /**
//     * 过滤方法
//     */
//    @DynamicOption(names = {"--filter", "-f"}, args = {"name=", "path=", "size="}, repeated = true)
//    @OptionUsage(description  = {"Apply filters to select or exclude files within the archive based on specified criteria. Use the following sub-parameters:",
//            "* name: Retain file whose file name matches the specified pattern.",
//            "* path: Retain file whose full path matches the specified pattern.",
//            "* size: Retain file within a specific size range, using comparison operators (>, >=, <, <=, ==, !=) to define the range. Specify the size with a unit (B, KB, MB, GB, TB, PB), which is case-insensitive.",
//            "Note:",
//            "- For name and path, use '*' or '{}' as wildcards to match any sequence of characters.",
//            "- Prefix any sub-parameter with ! to exclude files that match the pattern."},
//            format = "--filter [path] [name] [size]")
//    List<Map<String, String>> filter = null;
//    /**
//     * 使用/禁用索引表
//     */
//    @Option(names = {"--disable-idx"}, type = FieldType.NULL)
//    @OptionUsage(description  = "Suppress the loading and construction of the external indexer during task execution.")
//    boolean indexable = true;
//    /**
//     * 静默执行
//     */
//    @Option(names = {"--silent", "-s"}, type = FieldType.NULL)
//    @OptionUsage(description  = "Suppress terminal output logs.")
//    boolean silent = false;
//
//    public static void main(String[] args) throws IOException {
//        MergeProgram program = new MergeProgram();
//        CommandOptions options = program.parse(args.length == 1 && args[0].equals("merge") ? new String[]{"--help"} : args);
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
//        Iterable<PackedEntry> tasks = filterInputs(program);
//        Packer packer = new Packer(program.output + ".pack", program.append);
//        for (PackedEntry task : tasks) {
//            ProgressBar bar = new ProgressBar.Builder()
//                    .setInitialMax(task.length())
//                    .setConsumer(program.silent ? ProgressConsumer.SILENT : new ConsoleConsumer())
//                    .setRenderer(new BarProgressRenderer("Archiving", DataLengthUnit.B))
//                    .build();
//            packer.setListening(bar);
//            String md5 = packer.append(task.getName(), task);
//            bar.close();
//            bar.print("MD5 (packed://" + packer.getFile() + ":" + task.getName() + ") = " + md5 + "\n\n");
//        }
//        packer.close(program.indexable);
//    }
//
//    /**
//     * 任务分块
//     */
//    private static Iterable<PackedEntry> filterInputs(MergeProgram program) throws IOException {
//        List<TObjectProcedure<LiveFile>> filters = PackerProgram.getFilter(program.filter);
//
//        if (program.exists.equals("SKIP") || program.exists.equals("STOP")) {
//            return new Iterable<PackedEntry>() {
//                @Override
//                public Iterator<PackedEntry> iterator() {
//                    return new Iterator<PackedEntry>() {
//                        final Set<String> names = new THashSet<>();
//                        final Iterator<LiveFile> outerIterator = program.inputs.iterator();
//                        Iterator<PackedEntry> innerIterator = null;
//                        PackedEntry nextFile = null;
//
//                        {
//                            advanceToNextValidFile();
//                        }
//
//                        @Override
//                        public boolean hasNext() {
//                            return nextFile != null;
//                        }
//
//                        @Override
//                        public PackedEntry next() {
//                            if (!hasNext()) {
//                                throw new NoSuchElementException();
//                            }
//
//                            PackedEntry currentFile = nextFile;
//                            advanceToNextValidFile();
//                            return currentFile;
//                        }
//
//                        private void advanceToNextValidFile() {
//                            // 执行初始化
//                            try {
//                                this.nextFile = null;
//                                while (true) {
//                                    if (innerIterator == null || !innerIterator.hasNext()) {
//                                        if (!outerIterator.hasNext()) {
//                                            return; // 没有更多的元素
//                                        }
//                                        PackedFile file = new PackedFile(outerIterator.next(), program.indexable);
//                                        if (file.numOfFiles() == 0 && !program.silent) {
//                                            logger.warn("There are no packed entries in the {}, it might be a raw data file rather than a packed file.", file.getFile());
//                                        }
//                                        innerIterator = file.iterator();
//                                    }
//
//                                    next:
//                                    while (innerIterator.hasNext()) {
//                                        PackedEntry file = innerIterator.next();
//                                        for (TObjectProcedure<LiveFile> filter : filters) {
//                                            if (!filter.execute(file)) {
//                                                continue next;
//                                            }
//                                        }
//                                        this.nextFile = file;
//                                        return;
//                                    }
//
//                                    innerIterator = null; // 当前 innerIterator 用完了，继续处理下一个 outerIterator
//                                }
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    };
//                }
//            };
//        } else {
//            Map<String, PackedEntry> tasks = new LinkedHashMap<>();
//            for (int i = 0; i < program.inputs.size(); i++) {
//                PackedFile input = new PackedFile(program.inputs.fastGet(i), program.indexable);
//                next:
//                for (PackedEntry task : input) {
//                    for (TObjectProcedure<LiveFile> filter : filters) {
//                        if (!filter.execute(task)) {
//                            continue next;
//                        }
//                    }
//
//                    if (tasks.containsKey(task.getName())) {
//                        switch (program.exists) {
//                            // SKIP/REPLACE/KEEP_NEWEST/STOP
//                            case "SKIP":
//                                break;
//                            case "KEEP_NEWEST":
//                                if (task.lastModifyTime() > tasks.get(task.getName()).lastModifyTime()) {
//                                    tasks.put(task.getName(), task);
//                                }
//                                break;
//                            case "REPLACE":
//                                tasks.put(task.getName(), task);
//                                break;
//                            case "STOP":
//                                logger.error("Termination during processing of file {} -> {}: Duplicate entry", task.getPath(), task.getName());
//                                break next;
//                        }
//                    } else {
//                        tasks.put(task.getName(), task);
//                    }
//                }
//            }
//            return new List<>(tasks.values());
//        }
//    }
//}
