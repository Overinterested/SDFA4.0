//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.ccf.CCFWriter;
//import edu.sysu.pmglab.ccf.record.IRecord;
//import edu.sysu.pmglab.ccf.type.FieldType;
//import edu.sysu.pmglab.container.interval.LongInterval;
//import edu.sysu.pmglab.container.list.List;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.file.LocalFile;
//import edu.sysu.pmglab.io.partreader.BoundReader;
//import edu.sysu.pmglab.io.reader.ISeekableReaderStream;
//import edu.sysu.pmglab.io.writer.ChannelAppendStream;
//import edu.sysu.pmglab.io.writer.ChannelOutputStream;
//import edu.sysu.pmglab.io.writer.ChannelWriterStream;
//import edu.sysu.pmglab.progressbar.ProgressBar;
//import edu.sysu.pmglab.utils.MD5;
//import edu.sysu.pmglab.utils.MapUtils;
//
//import java.io.Closeable;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.Comparator;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
///**
// * 打包数据
// */
//public class Packer implements AutoCloseable, Closeable {
//    /**
//     * 输出文件路径
//     */
//    final ChannelOutputStream writer;
//
//    /**
//     * 文件子部分
//     */
//    final Map<String, PackedEntry> entries = new LinkedHashMap<>();
//
//    /**
//     * MD5 计算器
//     */
//    final MessageDigest md5;
//
//    /**
//     * 交换缓存区
//     */
//    final byte[] cache = new byte[8192 * 10];
//
//    /**
//     * 监听器
//     */
//    ProgressBar bar;
//
//    /**
//     * 包装器
//     */
//    public Packer(String file) throws IOException {
//        this(new File(file), true);
//    }
//
//    /**
//     * 包装器
//     */
//    public Packer(String file, boolean append) throws IOException {
//        this(new File(file), append);
//    }
//
//    /**
//     * 包装器
//     */
//    public Packer(File file) throws IOException {
//        this(file, true);
//    }
//
//    /**
//     * 包装器
//     */
//    public Packer(File file, boolean append) throws IOException {
//        if (append && file.exists()) {
//            // 追加前要求保证前面的数据是完整的, 否则将来也无法读取此数据
//            PackedFile packFile = new PackedFile(new LocalFile(file));
//            if (!packFile.isIntegrity()) {
//                throw new IOException("Broken packed archive: " + file);
//            }
//
//            this.entries.putAll(packFile.entries);
//            this.writer = new ChannelAppendStream(file);
//        } else {
//            this.writer = new ChannelWriterStream(file);
//        }
//
//        try {
//            this.md5 = MessageDigest.getInstance("MD5");
//        } catch (NoSuchAlgorithmException e) {
//            throw new IOException(e);
//        }
//    }
//
//    /**
//     * 设置监听器
//     */
//    Packer setListening(ProgressBar bar) {
//        this.bar = bar;
//        return this;
//    }
//
//    /**
//     * 追加数据
//     */
//    public String append(LiveFile file) throws IOException {
//        return append(file.getName(), file);
//    }
//
//    /**
//     * 追加数据
//     */
//    public String append(String tag, LiveFile file) throws IOException {
//        if (tag == null) {
//            tag = file.getName();
//        }
//
//        if (tag.contains(":")) {
//            throw new IOException("Invalid entry name: the tag cannot contain the ':' character");
//        }
//
//        byte[] name = tag.getBytes(StandardCharsets.UTF_8);
//        if (name.length > 65536) {
//            throw new IllegalArgumentException("Invalid entry name: too long (> 65536)");
//        }
//
//        byte[] md5;
//        synchronized (writer) {
//            if (entries.containsKey(tag)) {
//                throw new IOException("Duplicate entry: " + tag);
//            }
//
//            long length = file.length();
//            long lastModifyTime = file.lastModifyTime();
//
//            this.writer.writeShort((short) (name.length - 1));
//            this.writer.write(name);
//            this.writer.writeLong(length);
//            this.writer.writeLong(lastModifyTime);
//
//            this.md5.reset();
//            this.writer.flush();
//            long pointer = this.writer.tell();
//            try (ISeekableReaderStream reader = new BoundReader(file.openAsBinary(), new LongInterval(0, file.length()))) {
//                int len;
//                while ((len = reader.read(cache)) != -1) {
//                    this.writer.write(cache, 0, len);
//                    this.md5.update(cache, 0, len);
//
//                    if (bar != null) {
//                        bar.step(len);
//                    }
//                }
//            }
//
//            // 16 bytes 数据
//            md5 = this.md5.digest();
//            this.writer.write(md5);
//            this.writer.write(PackedFile.MAGIC);
//            this.writer.flush();
//            this.entries.put(tag, new PackedEntry(new LazyLocalFile(this.writer.getFile()), tag, length, lastModifyTime, new LongInterval(pointer, pointer + length), MD5.toString(md5)));
//        }
//
//        return MD5.toString(md5);
//    }
//
//    /**
//     * 追加数据
//     */
//    public String append(String tag, byte[] data) throws IOException {
//        return append(tag, new ImmutableByteCode(data));
//    }
//
//    /**
//     * 追加数据
//     */
//    public String append(String tag, InputStream data) throws IOException {
//        if (tag != null) {
//            if (tag.contains(":")) {
//                throw new IOException("Invalid entry name: the tag cannot contain the ':' character");
//            }
//
//            if (tag.getBytes(StandardCharsets.UTF_8).length > 65536) {
//                throw new IllegalArgumentException("Invalid entry name: too long (> 65536)");
//            }
//        }
//
//        byte[] md5;
//
//        synchronized (writer) {
//            if (tag == null) {
//                // 使用时间戳作为文件名
//                tag = String.valueOf(System.nanoTime());
//            }
//
//            if (entries.containsKey(tag)) {
//                throw new IOException("Duplicate entry: " + tag);
//            }
//
//            long lastModifyTime = 0L;
//            long length = 0L;
//            byte[] name = tag.getBytes(StandardCharsets.UTF_8);
//            this.writer.writeShort((short) (name.length - 1));
//            this.writer.write(name);
//
//            // 标记文件长度
//            long lengthPointer = this.writer.tell();
//            this.writer.writeLong(length);
//            this.writer.writeLong(lastModifyTime);
//
//            this.md5.reset();
//            this.writer.flush();
//            long pointer = this.writer.tell();
//            int len;
//            while ((len = data.read(cache)) != -1) {
//                length += len;
//                this.writer.write(cache, 0, len);
//                this.md5.update(cache, 0, len);
//
//                if (bar != null) {
//                    bar.step(len);
//                }
//            }
//
//            // 16 bytes 数据
//            long markPointer = this.writer.tell();
//            this.writer.seek(lengthPointer);
//            this.writer.writeLong(length);
//            this.writer.seek(markPointer);
//
//            md5 = this.md5.digest();
//            this.writer.write(md5);
//            this.writer.write(PackedFile.MAGIC);
//            this.writer.flush();
//
//            this.entries.put(tag, new PackedEntry(new LazyLocalFile(this.writer.getFile()), tag, length, lastModifyTime, new LongInterval(pointer, pointer + length), MD5.toString(md5)));
//        }
//
//        return MD5.toString(md5);
//    }
//
//    /**
//     * 追加数据
//     */
//    public String append(String tag, IByteCode data) throws IOException {
//        if (tag != null) {
//            if (tag.contains(":")) {
//                throw new IOException("Invalid entry name: the tag cannot contain the ':' character");
//            }
//
//            if (tag.getBytes(StandardCharsets.UTF_8).length > 65536) {
//                throw new IllegalArgumentException("Invalid entry name: too long (> 65536)");
//            }
//        }
//
//        byte[] md5;
//        synchronized (writer) {
//            if (tag == null) {
//                // 使用时间戳作为文件名
//                tag = String.valueOf(System.nanoTime());
//            }
//
//            if (entries.containsKey(tag)) {
//                throw new IOException("Duplicate entry: " + tag);
//            }
//
//            long length = data.length();
//            long lastModifyTime = 0L;
//            byte[] name = tag.getBytes(StandardCharsets.UTF_8);
//            this.writer.writeShort((short) (name.length - 1));
//            this.writer.write(name);
//            this.writer.writeLong(length);
//            this.writer.writeLong(lastModifyTime);
//
//            this.md5.reset();
//            this.writer.flush();
//            long pointer = this.writer.tell();
//            this.writer.write(data.unsafeGetBytes(), data.unsafeGetStart(), data.length());
//            this.md5.update(data.unsafeGetBytes(), data.unsafeGetStart(), data.length());
//            if (bar != null) {
//                bar.step(data.length());
//            }
//
//            // 16 bytes 数据
//            md5 = this.md5.digest();
//            this.writer.write(md5);
//            this.writer.write(PackedFile.MAGIC);
//            this.writer.flush();
//
//            this.entries.put(tag, new PackedEntry(new LazyLocalFile(this.writer.getFile()), tag, length, lastModifyTime, new LongInterval(pointer, pointer + length), MD5.toString(md5)));
//        }
//
//        return MD5.toString(md5);
//    }
//
//    /**
//     * 合并数据
//     * 如果包含重复数据, 将会抛出异常
//     */
//    public Packer merge(PackedFile file) throws IOException {
//        if (file == null || file.numOfFiles() == 0) {
//            return this;
//        }
//
//        synchronized (writer) {
//            for (PackedEntry entry : file) {
//                if (entries.containsKey(entry.getName())) {
//                    throw new IOException("Duplicate entry: " + entry.getName());
//                }
//            }
//
//            for (PackedEntry entry : file) {
//                long length = entry.length();
//                long lastModifyTime = entry.lastModifyTime();
//
//                byte[] name = entry.getName().getBytes(StandardCharsets.UTF_8);
//                this.writer.writeShort((short) (name.length - 1));
//                this.writer.write(name);
//                this.writer.writeLong(length);
//                this.writer.writeLong(lastModifyTime);
//
//                this.md5.reset();
//                this.writer.flush();
//                long pointer = this.writer.tell();
//                try (ISeekableReaderStream reader = entry.openAsBinary()) {
//                    int len;
//                    while ((len = reader.read(cache)) != -1) {
//                        this.writer.write(cache, 0, len);
//                        this.md5.update(cache, 0, len);
//
//                        if (bar != null) {
//                            bar.step(len);
//                        }
//                    }
//                }
//
//                // 16 bytes 数据
//                this.writer.write(this.md5.digest());
//                this.writer.write(PackedFile.MAGIC);
//                this.writer.flush();
//                this.entries.put(entry.getName(), new PackedEntry(new LazyLocalFile(this.writer.getFile()), entry.getName(), length, lastModifyTime, new LongInterval(pointer, pointer + length), entry.md5()));
//            }
//        }
//
//        return this;
//    }
//
//    /**
//     * 是否包含指定 tag
//     */
//    public boolean contains(String tag) {
//        return this.entries.containsKey(tag);
//    }
//
//    /**
//     * 获取文件对象
//     */
//    public File getFile() {
//        return this.writer.getFile();
//    }
//
//    /**
//     * 关闭文件流
//     */
//    public PackedFile close(boolean saveIndex) throws IOException {
//        synchronized (this.writer) {
//            this.writer.close();
//        }
//
//        if (saveIndex) {
//            CCFWriter writer = new CCFWriter(new File(getFile().getCanonicalPath() + ".pidx"))
//                    .addMeta(new CCFMetaItem<>("SOURCE", FieldType.stringMap, MapUtils.of(
//                            "NAME", getFile().getName(),
//                            "SIZE", String.valueOf(getFile().length()),
//                            "LAST_MODIFY_TIME", String.valueOf(getFile().lastModified())
//                    )))
//                    .addField("NAME", FieldType.string)
//                    .addField("SIZE", FieldType.varInt64)
//                    .addField("LAST_MODIFY_TIME", FieldType.varInt64)
//                    .addField("RANGE", FieldType.longInterval)
//                    .addField("MD5", FieldType.string);
//
//            IRecord record = writer.getRecord();
//            List<PackedEntry> entries = List.wrap(this.entries.values());
//            entries.sort(Comparator.comparingLong(o -> o.range().start()));
//            for (PackedEntry file : entries) {
//                record.set(0, file.getName());
//                record.set(1, file.length());
//                record.set(2, file.lastModifyTime());
//                record.set(3, file.range());
//                record.set(4, file.md5());
//                writer.write(record);
//            }
//            writer.close();
//        }
//
//        return new PackedFile(new LocalFile(this.writer.getFile()), this.entries);
//    }
//
//    @Override
//    public void close() throws IOException {
//        close(false);
//    }
//}
