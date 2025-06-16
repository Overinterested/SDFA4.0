//package edu.sysu.pmglab.easytools.packer;
//
//import edu.sysu.pmglab.container.interval.LongInterval;
//import edu.sysu.pmglab.io.file.FileType;
//import edu.sysu.pmglab.io.file.LiveFile;
//import edu.sysu.pmglab.io.partreader.BoundReader;
//import edu.sysu.pmglab.io.reader.EmptyReaderStream;
//import edu.sysu.pmglab.io.reader.ISeekableReaderStream;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
///**
// * 映射到文件中
// */
//public class PackedEntry extends LiveFile {
//    /**
//     * 父文件对象
//     */
//    final LiveFile parent;
//
//    /**
//     * 部分文件名
//     */
//    final String tag;
//
//    /**
//     * 部分文件长度
//     */
//    final long length;
//
//    /**
//     * 最后修改时间
//     */
//    final long lastModifiedTime;
//
//    /**
//     * MD5 码
//     */
//    final String md5;
//
//    /**
//     * 数据范围 (左闭右开)
//     */
//    final LongInterval range;
//
//    /**
//     * 构造器方法
//     */
//    public PackedEntry(String path) throws IOException {
//        if (path.startsWith("packed://")) {
//            path = path.substring(9);
//        }
//
//        int index = path.lastIndexOf(":");
//        if (index == -1) {
//            throw new IOException("Incorrect PACKED path format: packed://<path>:<tag>");
//        } else {
//            String tag = path.substring(index + 1);
//            LiveFile parent = LiveFile.of(path.substring(0, index));
//
//            PackedEntry entry = new PackedFile(parent, tag).getEntry(tag);
//            this.parent = parent;
//            if (entry == null) {
//                throw new IOException("Unpacked entry '" + tag + "' in " + this.getParent());
//            }
//            this.tag = entry.tag;
//            this.length = entry.length;
//            this.lastModifiedTime = entry.lastModifiedTime;
//            this.range = entry.range;
//            this.md5 = entry.md5;
//        }
//    }
//
//    /**
//     * 构造器方法
//     */
//    public PackedEntry(String path, String tag) throws IOException {
//        this(LiveFile.of(path), tag);
//    }
//
//    /**
//     * 构造器方法
//     */
//    public PackedEntry(File path, String tag) throws IOException {
//        this(LiveFile.of(path), tag);
//    }
//
//    /**
//     * 构造器方法
//     */
//    public PackedEntry(LiveFile parent, String tag) throws IOException {
//        PackedEntry entry = new PackedFile(parent, tag).getEntry("tag");
//        if (entry == null) {
//            throw new IOException("Unpacked entry: " + tag);
//        }
//        this.parent = parent;
//        this.tag = entry.tag;
//        this.length = entry.length;
//        this.lastModifiedTime = entry.lastModifiedTime;
//        this.range = entry.range;
//        this.md5 = entry.md5;
//    }
//
//    /**
//     * 构造器方法
//     *
//     * @param name             文件名 (标签), 长度要求 1~65536
//     * @param length           文件数据长度
//     * @param lastModifiedTime 上次修改时间
//     * @param range            文件数据范围, 使用左闭右闭
//     */
//    PackedEntry(LiveFile parent, String name, long length, long lastModifiedTime, LongInterval range, String md5) {
//        this.parent = parent;
//        if (name == null || name.length() == 0) {
//            throw new IllegalArgumentException("Invalid packed entry name: null or empty");
//        }
//
//        if (lastModifiedTime < 0) {
//            lastModifiedTime = 0;
//        }
//
//        if (length < 0) {
//            throw new IllegalArgumentException("Length of data < 0");
//        }
//
//        if (length != range.end() - range.start()) {
//            throw new IllegalArgumentException("Invalid packed entry length");
//        }
//
//        if (name.getBytes(StandardCharsets.UTF_8).length > 65536) {
//            throw new IllegalArgumentException("Invalid packed entry name: too long (> 65536)");
//        }
//
//        if (md5 == null || md5.length() != 32) {
//            throw new IllegalArgumentException("Invalid packed entry: MD5 checksum should be 32 characters long");
//        }
//
//        this.tag = name;
//        this.length = length;
//        this.lastModifiedTime = lastModifiedTime;
//        this.range = range;
//        this.md5 = md5;
//    }
//
//    /**
//     * 转为 PackedFile 对象
//     */
//    public PackedFile toPackedFile() throws IOException {
//        return new PackedFile(this);
//    }
//
//    /**
//     * 获取父文件对象
//     */
//    public LiveFile getParent() {
//        return parent;
//    }
//
//    @Override
//    public FileType getFileType() {
//        return FileType.PACKED;
//    }
//
//    @Override
//    public long lastModifyTime() {
//        return lastModifiedTime;
//    }
//
//    /**
//     * 数据长度
//     */
//    public long length() {
//        return length;
//    }
//
//    @Override
//    public String getPath() {
//        // packed://<file>:<tag>
//        return "packed://" + this.parent + ":" + this.tag;
//    }
//
//    @Override
//    public String getName() {
//        return this.tag;
//    }
//
//    @Override
//    public void deleteOnExit() throws IOException {
//
//    }
//
//    @Override
//    public ISeekableReaderStream openAsBinary() throws IOException {
//        if (this.range.start() == this.range.end()) {
//            // 不可读
//            return new EmptyReaderStream();
//        } else {
//            return new BoundReader(this.parent.openAsBinary(), this.range);
//        }
//    }
//
//    /**
//     * 获取 MD5 码
//     */
//    public String md5() {
//        return this.md5;
//    }
//
//    /**
//     * 数据范围 (左闭右开)
//     */
//    public LongInterval range() {
//        return this.range;
//    }
//}
