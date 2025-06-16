//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.commandParser.CommandOptions;
//import edu.sysu.pmglab.commandParser.ICommandProgram;
//import edu.sysu.pmglab.commandParser.annotation.option.EntryOption;
//import edu.sysu.pmglab.commandParser.annotation.usage.OptionUsage;
//import edu.sysu.pmglab.commandParser.annotation.usage.Parser;
//import edu.sysu.pmglab.commandParser.annotation.usage.UsageItem;
//import edu.sysu.pmglab.commandParser.converter.FileSizeFilterConverter;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.text.reader.CustomSeparator;
//import edu.sysu.pmglab.io.text.reader.ISeparator;
//import gnu.trove.procedure.TLongProcedure;
//import gnu.trove.procedure.TObjectProcedure;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.util.Map;
//
///**
// * 打包工具
// */
//@Parser(usage = "packer [options]",
//        usage_item = {
//                @UsageItem(key = "API", value = {"edu.sysu.pmglab.packer.PackedFile", "edu.sysu.pmglab.packer.PackedEntry", "edu.sysu.pmglab.packer.Packer",}),
//                @UsageItem(key = "About", value = "The Packer tool allows you to package multiple files, merge archives, extract contents, and list archive details."),
//                @UsageItem(key = "Format", value = {"Each packed archive consists of subparts with the following structure:",
//                        "* [2 bytes (filename length - 1), short value]",
//                        "* [filename, UTF-8 coding]",
//                        "* [8 bytes file size, long value]",
//                        "* [8 bytes modification time, long value]",
//                        "* [data]",
//                        "* [16 bytes MD5 checksum]",
//                        "* [8 bytes end marker, (15, 0, 2, 10, 30, 4, 13, 3)]"})
//        }
//)
//public class PackerProgram extends ICommandProgram {
//    /**
//     * 日志系统
//     */
//    private final static Logger logger = LoggerFactory.getLogger(PackerProgram.class);
//
//    /**
//     * 构建 PACKER 文件
//     */
//    @EntryOption(value = "build")
//    @OptionUsage(description  = "Package multiple files into a single output file.",
//            format = "build <input> <input> ... [options]")
//    String[] build;
//
//    /**
//     * 从 PACKER 中提取数据
//     */
//    @EntryOption(value = "export")
//    @OptionUsage(description  = "Extract files from packed archives.",
//            format = "export <input> <input> ... [options]")
//    String[] export;
//
//    /**
//     * 列出文件信息
//     */
//    @EntryOption(value = "ls")
//    @OptionUsage(description  = "Generate a detailed listing of subparts within the specified packed archive.",
//            format = "ls <input> [options]")
//    String[] ls;
//
//    /**
//     * 合并 PACKER 文件
//     */
//    @EntryOption(value = "merge")
//    @OptionUsage(description  = "Merge multiple packed archives into a single output file.",
//            format = "merge <input> <input> ... [options]")
//    String[] merge;
//
//    /**
//     * 拆分 PACKER 文件
//     */
//    @EntryOption(value = "split")
//    @OptionUsage(description  = "Split a packed archive into multiple archives based on specified criteria.",
//            format = "split <input> [options]")
//    String[] split;
//
//    /**
//     * 构建索引
//     */
//    @EntryOption(value = "index")
//    @OptionUsage(description  = "Builds an external index to accelerate access to data.",
//            format = "index <input> <input> ... [options]")
//    String[] index;
//
//    /**
//     * 解析器入口函数
//     *
//     * @param args 传入参数
//     */
//    public static void main(String[] args) throws IOException {
//        PackerProgram program = new PackerProgram();
//        CommandOptions options = program.parse(args);
//
//        if (options.isHelp()) {
//            // 打印帮助文档
//            logger.info("\n{}", options.usage());
//            return;
//        }
//
//        if (options.passed("build")) {
//            BuildProgram.main(options.value("build"));
//            return;
//        }
//
//        if (options.passed("merge")) {
//            MergeProgram.main(options.value("merge"));
//            return;
//        }
//
//        if (options.passed("split")) {
//            SplitProgram.main(options.value("split"));
//            return;
//        }
//
//        if (options.passed("export")) {
//            ExportProgram.main(options.value("export"));
//            return;
//        }
//
//        if (options.passed("ls")) {
//            LsProgram.main(options.value("ls"));
//            return;
//        }
//
//        if (options.passed("index")) {
//            IndexProgram.main(options.value("index"));
//            return;
//        }
//    }
//
//
//    /**
//     * 获取过滤器
//     * --filter [path=] [name=] [tag=] [size=]
//     */
//    static List<TObjectProcedure<LiveFile>> getFilter(List<Map<String, String>> options) {
//        if (options == null || options.size() == 0) {
//            return new List<>(0);
//        }
//
//        List<TObjectProcedure<LiveFile>> filters = new List<>();
//        for (Map<String, String> option : options) {
//            switch (List.wrap(option.values()).count(string -> string.length() > 0)) {
//                case 0:
//                    continue;
//                case 1:
//                    if (option.containsKey("path") && option.get("path").length() > 0) {
//                        String path = option.get("path").replace("*", "{}");
//                        boolean NOT = path.startsWith("!");
//                        if (NOT) {
//                            path = path.substring(1);
//                        }
//
//                        ISeparator separator = new CustomSeparator(path);
//                        filters.add(file -> {
//                            if (NOT) {
//                                try {
//                                    // 取反
//                                    separator.accept(file.getPath());
//                                    return false;
//                                } catch (Exception e) {
//                                    return true;
//                                }
//                            } else {
//                                try {
//                                    separator.accept(file.getPath());
//                                    return true;
//                                } catch (Exception e) {
//                                    return false;
//                                }
//                            }
//                        });
//                    }
//
//                    if (option.containsKey("name") && option.get("name").length() > 0) {
//                        String name = option.get("name").replace("*", "{}");
//                        boolean NOT = name.startsWith("!");
//                        if (NOT) {
//                            name = name.substring(1);
//                        }
//
//                        ISeparator separator = new CustomSeparator(name);
//                        filters.add(file -> {
//                            if (NOT) {
//                                try {
//                                    // 取反
//                                    separator.accept(file.getName());
//                                    return false;
//                                } catch (Exception e) {
//                                    return true;
//                                }
//                            } else {
//                                try {
//                                    separator.accept(file.getName());
//                                    return true;
//                                } catch (Exception e) {
//                                    return false;
//                                }
//                            }
//                        });
//                    }
//
//                    if (option.containsKey("size") && option.get("size").length() > 0) {
//                        TLongProcedure filter = FileSizeFilterConverter.INSTANCE.convert("--filter", option.get("size"));
//                        filters.add(file -> filter.execute(file.length()));
//                    }
//                    break;
//                default:
//                    throw new IllegalArgumentException("Each '--filter' command applies only one rule at a time; use multiple '--filter' commands to apply different criteria");
//            }
//        }
//
//        return filters;
//    }
//}