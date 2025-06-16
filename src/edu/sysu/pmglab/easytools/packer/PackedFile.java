//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.bytecode.ByteStream;
//import edu.sysu.pmglab.bytecode.Bytes;
//import edu.sysu.pmglab.ccf.CCFReader;
//import edu.sysu.pmglab.ccf.CCFTable;
//import edu.sysu.pmglab.ccf.CCFWriter;
//import edu.sysu.pmglab.ccf.record.IRecord;
//import edu.sysu.pmglab.ccf.type.FieldType;
//import edu.sysu.pmglab.container.interval.IntInterval;
//import edu.sysu.pmglab.container.interval.LongInterval;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.file.LocalFile;
//import edu.sysu.pmglab.io.reader.ISeekableReaderStream;
//import edu.sysu.pmglab.io.reader.ReaderStream;
//import edu.sysu.pmglab.io.text.writer.CustomJoiner;
//import edu.sysu.pmglab.io.writer.ChannelWriterStream;
//import edu.sysu.pmglab.utils.MD5;
//import edu.sysu.pmglab.utils.MapUtils;
//import gnu.trove.procedure.TObjectProcedure;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicBoolean;
//
///**
// * [2 byte 文件名长度][文件名][8 bytes 文件长度][8 bytes 数据修改时间][数据][16 bytes MD5 码][结束位, PE]
// */
//public class PackedFile implements Iterable<PackedEntry> {
//    /**
//     * 魔术编码
//     */
//    public static final Bytes MAGIC = new Bytes(new byte[]{'P' - 'A', 'A' - 'A', 'C' - 'A', 'K' - 'A', '_' - 'A', 'E' - 'A', 'N' - 'A', 'D' - 'A'});
//    /**
//     * 文件对象
//     */
//    final LiveFile file;
//    /**
//     * 是否完整
//     */
//    final AtomicBoolean integrity = new AtomicBoolean(true);
//    /**
//     * 文件子部分
//     */
//    final Map<String, PackedEntry> entries;
//
//    /**
//     * 构造器方法
//     */
//    PackedFile(LiveFile file, Map<String, PackedEntry> entries) {
//        this.file = file;
//        this.entries = entries;
//    }
//
//    /**
//     * 构造器方法
//     */
//    public PackedFile(String filename) throws IOException {
//        this(LiveFile.of(filename), false);
//    }
//
//    /**
//     * 构造器方法
//     */
//    public PackedFile(File filename) throws IOException {
//        this(LiveFile.of(filename), false);
//    }
//
//    /**
//     * 构造器方法
//     */
//    public PackedFile(LiveFile file) throws IOException {
//        this(file, false);
//    }
//
//    /**
//     * 构造器方法
//     *
//     * @param filename  文件名
//     * @param loadIndex 是否加载外部索引
//     */
//    public PackedFile(String filename, boolean loadIndex) throws IOException {
//        this(LiveFile.of(filename), loadIndex);
//    }
//
//    /**
//     * 构造器方法
//     *
//     * @param file      文件对象
//     * @param loadIndex 是否加载外部索引
//     */
//    public PackedFile(File file, boolean loadIndex) throws IOException {
//        this(LiveFile.of(file), loadIndex);
//    }
//
//    /**
//     * 构造器方法
//     *
//     * @param file      文件对象
//     * @param loadIndex 是否加载外部索引
//     */
//    public PackedFile(LiveFile file, boolean loadIndex) throws IOException {
//        this(file, loadIndex, new IntInterval(0, Integer.MAX_VALUE));
//    }
//
//    /**
//     * 构造器方法
//     *
//     * @param file      文件对象
//     * @param loadIndex 是否加载外部索引
//     * @param range     左闭右开, 筛选子文件范围
//     */
//    public PackedFile(LiveFile file, boolean loadIndex, IntInterval range) throws IOException {
//        this.file = file;
//        this.entries = new LinkedHashMap<>();
//        if (range == null || range.end() <= 0) {
//            return;
//        }
//
//        int index = 0;
//        if (loadIndex) {
//            // 加载索引表
//            try {
//                CCFTable indexer = new CCFTable(file.getPath() + ".pidx");
//                Map<String, String> sources = indexer.getMeta().getUniqueValue("SOURCE");
//                if (Long.parseLong(sources.get("SIZE")) == file.length() && Long.parseLong(sources.get("LAST_MODIFY_TIME")) == file.lastModifyTime()) {
//                    // 状态一致
//                    CCFReader reader = new CCFReader(indexer);
//                    for (IRecord record : reader) {
//                        if (range.contains(index++, false)) {
//                            entries.put(record.get("NAME"), new PackedEntry(file, record.get("NAME"), record.get("SIZE"), record.get("LAST_MODIFY_TIME"),
//                                    record.get("RANGE"), record.get("MD5")));
//                        }
//
//                        if (index >= range.end()) {
//                            break;
//                        }
//                    }
//
//                    reader.close();
//                    return;
//                }
//            } catch (Exception | Error e) {
//                // 忽略异常, 硬加载
//                this.entries.clear();
//            }
//        }
//
//        if (file instanceof PackedEntry) {
//            // 此时只能提取到第一个文件
//            if (range.contains(index)) {
//                this.entries.put(file.getName(), (PackedEntry) file);
//            }
//            index++;
//        } else {
//            byte[] lengthOfName = new byte[2];
//            byte[] bytesOfName = new byte[65536];
//            byte[] lengthOfLong = new byte[8];
//            byte[] md5bytes = new byte[16];
//            ByteStream bytes = new ByteStream();
//
//            // 使用 reader stream 包装缓冲区对象
//            try (ISeekableReaderStream reader = new ReaderStream(file.openAsBinary())) {
//                long pointer;
//                while (index < range.end()) {
//                    int validBytes = reader.read(lengthOfName);
//                    if (validBytes == -1) {
//                        // 文件已经结束
//                        return;
//                    }
//
//                    if (validBytes != 2) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    int nameLength = (bytes.write(lengthOfName, 0, 2).binary2Short() & 0xFF) + 1;
//                    if (reader.read(bytesOfName, 0, nameLength) != nameLength) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    // 文件名
//                    String name = new String(bytesOfName, 0, nameLength, StandardCharsets.UTF_8);
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//                    long length = bytes.wrap(lengthOfLong, 0, 8).binary2Long();
//
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//                    long lastModifiedTime = bytes.wrap(lengthOfLong, 0, 8).binary2Long();
//
//                    // 记录当前指针, 为数据起点
//                    pointer = reader.tell();
//                    reader.skip(length);
//                    if (reader.tell() != pointer + length) {
//                        // 数据不完整, 损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    if (reader.read(md5bytes, 0, 16) != 16) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    if (bytes.wrap(lengthOfLong, 0, 8).valueEquals(MAGIC)) {
//                        if (range.contains(index, false)) {
//                            this.entries.put(name, new PackedEntry(this.file, name, length, lastModifiedTime, new LongInterval(pointer, pointer + length), MD5.toString(md5bytes)));
//                        }
//
//                        index++;
//                    } else {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * 只要特定子部分的构造器方法
//     */
//    PackedFile(LiveFile file, String entry) throws IOException {
//        this.file = file;
//        this.entries = new LinkedHashMap<>();
//        if (file instanceof PackedEntry) {
//            this.entries.put(file.getName(), (PackedEntry) file);
//        } else {
//            byte[] lengthOfName = new byte[2];
//            byte[] bytesOfName = new byte[65536];
//            byte[] lengthOfLong = new byte[8];
//            byte[] md5bytes = new byte[16];
//            ByteCodeWrapper bytes = new ByteCodeWrapper();
//
//            try (ReaderStream reader = new ReaderStream(file.openAsBinary())) {
//                long pointer;
//                while (true) {
//                    int validBytes = reader.read(lengthOfName);
//                    if (validBytes == -1) {
//                        // 文件已经结束
//                        return;
//                    }
//
//                    if (validBytes != 2) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    int nameLength = (bytes.wrap(lengthOfName, 0, 2).binary2Short() & 0xFF) + 1;
//                    if (reader.read(bytesOfName, 0, nameLength) != nameLength) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    // 文件名
//                    String name = new String(bytesOfName, 0, nameLength, StandardCharsets.UTF_8);
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//                    long length = bytes.wrap(lengthOfLong, 0, 8).binary2Long();
//
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//                    long lastModifiedTime = bytes.wrap(lengthOfLong, 0, 8).binary2Long();
//
//                    // 记录当前指针, 为数据起点
//                    pointer = reader.tell();
//                    reader.skip(length);
//                    if (reader.tell() != pointer + length) {
//                        // 数据不完整, 损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    if (reader.read(md5bytes, 0, 16) != 16) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    if (reader.read(lengthOfLong, 0, 8) != 8) {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//
//                    if (bytes.wrap(lengthOfLong, 0, 8).valueEquals(MAGIC)) {
//                        if (name.equals(entry)) {
//                            this.entries.put(name, new PackedEntry(this.file, name, length, lastModifiedTime, new LongInterval(pointer, pointer + length), MD5.toString(md5bytes)));
//                            return;
//                        }
//                    } else {
//                        // 文件损坏
//                        this.integrity.set(false);
//                        return;
//                    }
//                }
//            } finally {
//                bytes.clear();
//            }
//        }
//    }
//
//    /**
//     * 获取文件 IO 流
//     */
//    public PackedEntry getEntry(String name) {
//        return this.entries.get(name);
//    }
//
//    /**
//     * 获取文件对象
//     */
//    public PackedEntry getEntry(int index) {
//        if (index < 0 || index >= this.entries.size()) {
//            throw new ArrayIndexOutOfBoundsException();
//        }
//
//        int i = 0;
//        for (PackedEntry entry : this) {
//            if (i == index) {
//                return entry;
//            }
//            i++;
//        }
//
//        throw new ArrayIndexOutOfBoundsException();
//    }
//
//    /**
//     * 将此文件的子部分按照数据大小近似进行切块
//     *
//     * @param partNum 子部分数
//     * @return 切块结果
//     */
//    public List<PackedFile> divide(int partNum) {
//        List<PackedEntry> entries = new List<>(this);
//        List<List<PackedEntry>> parts = entries.divide(PackedEntry::length, partNum);
//        List<PackedFile> returns = new List<>();
//        for (List<PackedEntry> part : parts) {
//            Map<String, PackedEntry> map = new LinkedHashMap<>();
//            for (PackedEntry file : part) {
//                map.put(file.getName(), file);
//            }
//            if (map.size() > 0) {
//                returns.add(new PackedFile(this.file, map));
//            }
//        }
//
//        return returns;
//    }
//
//    /**
//     * 储存索引表, 用于加速下一次的访问
//     *
//     * @param target 目标路径
//     */
//    public LocalFile saveIndexer(String target) throws IOException {
//        CCFWriter writer = new CCFWriter(new File(target))
//                .addMeta(new CCFMetaItem<>("SOURCE", FieldType.stringMap, MapUtils.of(
//                        "NAME", getFile().getName(),
//                        "SIZE", String.valueOf(getFile().length()),
//                        "LAST_MODIFY_TIME", String.valueOf(getFile().lastModifyTime())
//                )))
//                .addField("NAME", FieldType.string)
//                .addField("SIZE", FieldType.varInt64)
//                .addField("LAST_MODIFY_TIME", FieldType.varInt64)
//                .addField("RANGE", FieldType.longInterval)
//                .addField("MD5", FieldType.string);
//
//        IRecord record = writer.getRecord();
//        for (PackedEntry file : this) {
//            record.set(0, file.getName());
//            record.set(1, file.length());
//            record.set(2, file.lastModifyTime());
//            record.set(3, file.range());
//            record.set(4, file.md5());
//            writer.write(record);
//        }
//        writer.close();
//
//        return new LocalFile(writer.getFile());
//    }
//
//    /**
//     * 获取文件 IO 流
//     */
//    public ISeekableReaderStream getReader(String name) throws IOException {
//        PackedEntry entry = getEntry(name);
//        if (entry != null) {
//            return entry.openAsBinary();
//        }
//        return null;
//    }
//
//    /**
//     * 提取文件子数据
//     */
//    public LocalFile extract(String name, String target) throws IOException {
//        return extract(name, target == null ? new File(name) : new File(target));
//    }
//
//    /**
//     * 提取文件子数据
//     */
//    public LocalFile extract(String name, File target) throws IOException {
//        PackedEntry entry = getEntry(name);
//        if (entry == null) {
//            // 不存在此部分数据
//            throw new IOException("Unpacked entry: " + name);
//        } else {
//            if (target == null) {
//                target = new File(name);
//            }
//
//            ChannelWriterStream writer = new ChannelWriterStream(target);
//            try (ISeekableReaderStream reader = getReader(name)) {
//                reader.writeTo(0, reader.length(), writer);
//            }
//            writer.close();
//            writer.getFile().setLastModified(entry.lastModifyTime());
//
//            return new LocalFile(writer.getFile());
//        }
//    }
//
//    /**
//     * 文件是否完整
//     */
//    public boolean isIntegrity() {
//        return this.integrity.get();
//    }
//
//    @Override
//    public Iterator<PackedEntry> iterator() {
//        return this.entries.values().iterator();
//    }
//
//    /**
//     * 获取文件对象
//     */
//    public LiveFile getFile() {
//        return file;
//    }
//
//    /**
//     * 列出子文件
//     */
//    public PackedFile listFiles(IntInterval range) {
//        if (range == null) {
//            return this;
//        }
//        int pointer = 0;
//        Map<String, PackedEntry> files = new LinkedHashMap<>();
//        for (PackedEntry entry : this.entries.values()) {
//            if (range.contains(pointer, false)) {
//                files.put(entry.getName(), entry);
//            }
//            pointer++;
//        }
//
//        if (files.size() == this.entries.size()) {
//            return this;
//        }
//        return new PackedFile(this.file, files);
//    }
//
//    /**
//     * 列出子文件
//     */
//    public PackedFile listFiles(List<TObjectProcedure<PackedEntry>> filters) {
//        if (filters == null || filters.size() == 0) {
//            return this;
//        }
//
//        Map<String, PackedEntry> files = new LinkedHashMap<>();
//        next:
//        for (PackedEntry entry : this.entries.values()) {
//            for (TObjectProcedure<PackedEntry> filter : filters) {
//                if (!filter.execute(entry)) {
//                    continue next;
//                }
//            }
//            files.put(entry.getName(), entry);
//        }
//
//        if (files.size() == this.entries.size()) {
//            return this;
//        }
//
//        return new PackedFile(this.file, files);
//    }
//
//    /**
//     * 列出子文件
//     */
//    public PackedFile listFiles(TObjectProcedure<PackedEntry> filter) {
//        if (filter == null) {
//            return this;
//        }
//        Map<String, PackedEntry> files = new LinkedHashMap<>();
//        for (PackedEntry entry : this.entries.values()) {
//            if (filter.execute(entry)) {
//                files.put(entry.getName(), entry);
//            }
//        }
//
//        if (files.size() == this.entries.size()) {
//            return this;
//        }
//        return new PackedFile(this.file, files);
//    }
//
//    /**
//     * 文件排序
//     */
//    public PackedFile sort(Comparator<PackedEntry> comparator) {
//        if (comparator != null) {
//            List<Map.Entry<String, PackedEntry>> files = new List<>();
//            for (Map.Entry<String, PackedEntry> file : this.entries.entrySet()) {
//                files.add(file);
//            }
//
//            files.sort((o1, o2) -> comparator.compare(o1.getValue(), o2.getValue()));
//
//            Map<String, PackedEntry> returns = new LinkedHashMap<>();
//            for (Map.Entry<String, PackedEntry> file : files) {
//                returns.put(file.getKey(), file.getValue());
//            }
//
//            return new PackedFile(this.file, returns);
//        } else {
//            return new PackedFile(this.file, this.entries);
//        }
//    }
//
//    /**
//     * 数据长度
//     */
//    public long length() {
//        long length = 0;
//        for (PackedEntry file : this.entries.values()) {
//            length += file.length();
//        }
//        return length;
//    }
//
//    /**
//     * 子文件数
//     */
//    public int numOfFiles() {
//        return this.entries.size();
//    }
//
//    @Override
//    public String toString() {
//        CustomJoiner formatter = new CustomJoiner("{}\t{}\t{}\t{}");
//        StringBuilder builder = new StringBuilder("NAME\tSIZE\tLAST_MODIFIED_TIME\tRANGE\tMD5");
//        for (PackedEntry entry : this.entries.values()) {
//            builder.append("\n");
//            builder.append(entry.getName());
//            builder.append("\t");
//            builder.append(entry.formatLength(null));
//            builder.append("\t");
//            builder.append(entry.formatLastModifyTime(null));
//            builder.append("\t");
//            if (entry.length() == 0) {
//                builder.append("-");
//            } else {
//                builder.append(entry.range().start());
//                builder.append("-");
//                builder.append(entry.range().end() - 1);
//            }
//            builder.append("\t");
//            builder.append(entry.md5());
//        }
//        return builder.toString();
//    }
//}
