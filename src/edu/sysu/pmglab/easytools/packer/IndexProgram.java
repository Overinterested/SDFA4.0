//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.RuntimeProperty;
//import edu.sysu.pmglab.bytecode.ByteStream;
//import edu.sysu.pmglab.ccf.CCFWriter;
//import edu.sysu.pmglab.ccf.record.IRecord;
//import edu.sysu.pmglab.ccf.type.FieldType;
//import edu.sysu.pmglab.commandParser.CommandOptions;
//import edu.sysu.pmglab.commandParser.ICommandProgram;
//import edu.sysu.pmglab.commandParser.annotation.option.Container;
//import edu.sysu.pmglab.commandParser.annotation.option.Option;
//import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
//import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
//import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
//import edu.sysu.pmglab.container.interval.LongInterval;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.reader.ISeekableReaderStream;
//import edu.sysu.pmglab.io.reader.ReaderStream;
//import edu.sysu.pmglab.progressbar.MultiProgressBar;
//import edu.sysu.pmglab.progressbar.ProgressConsumer;
//import edu.sysu.pmglab.progressbar.TextProgressRenderers;
//import edu.sysu.pmglab.progressbar.console.ConsoleConsumer;
//import edu.sysu.pmglab.utils.MD5;
//import edu.sysu.pmglab.utils.MapUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//@Parser(usage = "index <input> <input> ... [options]",
//        usage_item = {@UsageItem(key = "About", value = "Builds an external index to accelerate access to data.")})
//class IndexProgram extends ICommandProgram {
//    /**
//     * 日志系统
//     */
//    private final static Logger logger = LoggerFactory.getLogger(IndexProgram.class);
//    /**
//     * 指定读取文件
//     */
//    @Option(names = {"index"}, type = FieldType.livefile, required = true, container = Container.LIST)
//    List<LiveFile> inputs;
//    /**
//     * 输出文件夹
//     */
//    @Option(names = {"--output", "-o"}, type = FieldType.file)
//    @OptionUsage(description  = "Specify the output directory.", format = "--output <dir>", defaultTo = ".")
//    File outputDir = RuntimeProperty.WORKSPACE_PATH;
//    /**
//     * 静默执行
//     */
//    @Option(names = {"--silent", "-s"}, type = FieldType.NULL)
//    @OptionUsage(description  = "Suppress terminal output logs.")
//    boolean silent = false;
//
//    public static void main(String[] args) throws IOException {
//        IndexProgram program = new IndexProgram();
//        CommandOptions options = program.parse(args.length == 1 && args[0].equals("index") ? new String[]{"--help"} : args);
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
//        MultiProgressBar bar = new MultiProgressBar.Builder()
//                .setConsumer(program.silent ? ProgressConsumer.SILENT : new ConsoleConsumer())
//                .setRenderers(new TextProgressRenderers()
//                        .add("Indexed File")
//                        .add("Indexed Entry"))
//                .build();
//
//        byte[] lengthOfName = new byte[2];
//        byte[] bytesOfName = new byte[65536];
//        byte[] lengthOfLong = new byte[8];
//        byte[] md5bytes = new byte[16];
//        ByteStream bytes = new ByteStream();
//
//        for (LiveFile file : program.inputs) {
//            CCFWriter.Builder writer = new CCFWriter.Builder(new File(program.outputDir, file.getName() + ".pidx"))
//                    .addMeta(new CCFMetaItem<>("SOURCE", FieldType.stringMap, MapUtils.of(
//                            "NAME", file.getName(),
//                            "SIZE", String.valueOf(file.length()),
//                            "LAST_MODIFY_TIME", String.valueOf(file.lastModifyTime())
//                    )))
//                    .addField("NAME", FieldType.string)
//                    .addField("SIZE", FieldType.varInt64)
//                    .addField("LAST_MODIFY_TIME", FieldType.varInt64)
//                    .addField("RANGE", FieldType.longInterval)
//                    .addField("MD5", FieldType.string);
//
//            IRecord record = writer.getRecord();
//
//            // 使用 reader stream 包装缓冲区对象
//            try (ISeekableReaderStream reader = new ReaderStream(file.openAsBinary())) {
//                long pointer;
//                int index = 0;
//                while (true) {
//                    int validBytes = reader.read(lengthOfName);
//                    if (validBytes == -1) {
//                        // 文件已经结束
//                        break;
//                    }
//
//                    if (validBytes != 2) {
//                        // 文件损坏
//                        break;
//                    }
//
//                    int nameLength = (bytes.wrap(lengthOfName, 0, 2).binary2Short() & 0xFF) + 1;
//                    if (reader.read(bytesOfName, 0, nameLength) != nameLength) {
//                        break;
//                    }
//
//                    // 文件名
//                    String name = new String(bytesOfName, 0, nameLength, StandardCharsets.UTF_8);
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        break;
//                    }
//                    long length = bytes.wrap(lengthOfLong, 0, 8).binary2Long();
//
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        break;
//                    }
//                    long lastModifiedTime = bytes.wrap(lengthOfLong, 0, 8).binary2Long();
//
//                    // 记录当前指针, 为数据起点
//                    pointer = reader.tell();
//                    reader.skip(length);
//                    if (reader.tell() != pointer + length) {
//                        break;
//                    }
//
//                    if (reader.read(md5bytes, 0, 16) != 16) {
//                        break;
//                    }
//
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        break;
//                    }
//
//                    if (bytes.wrap(lengthOfLong, 0, 8).valueEquals(PackedFile.MAGIC)) {
//                        record.set(0, name);
//                        record.set(1, length);
//                        record.set(2, lastModifiedTime);
//                        record.set(3, new LongInterval(pointer, pointer + length));
//                        record.set(4, MD5.toString(md5bytes));
//                        writer.write(record);
//                        bar.step(0, 1);
//                        index++;
//                    } else {
//                        break;
//                    }
//                }
//
//            }
//
//            writer.close();
//            bar.step(1, 0);
//        }
//        bar.close();
//    }
//}
