package edu.sysu.pmglab.sdfa.toolkit;

import com.formdev.flatlaf.FlatLightLaf;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.CCFTable;
import edu.sysu.pmglab.ccf.field.FieldGroupMeta;
import edu.sysu.pmglab.ccf.field.FieldMeta;
import edu.sysu.pmglab.ccf.viewer.ReaderAdaptor;
import edu.sysu.pmglab.container.indexable.IndexableSet;
import edu.sysu.pmglab.container.indexable.LinkedSet;
import edu.sysu.pmglab.container.interval.LongInterval;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.SDFReaderOption;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.sv.sdsv.CompleteSDSV;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

import javax.swing.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Wenjie Peng
 * @create 2024-10-29 23:30
 * @description
 */
public class SDFViewerReader implements ReaderAdaptor {
    static {
        try {
            // improve UI
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {

        }
    }

    final SDFReader sdfReader;
    /**
     * 读取器实例
     */
    private CCFReader reader;

    private SDFReadType readerMode;


    /**
     * 构造器方法
     */
    public SDFViewerReader(String path) throws IOException {
        readerMode = SDFReadType.FULL;
        sdfReader = new SDFReader(path, readerMode);
        reader = sdfReader.getReader();
    }

    /**
     * 构造器方法
     */
    public SDFViewerReader(LiveFile path) throws IOException {
        readerMode = SDFReadType.FULL;
        sdfReader = new SDFReader(path, readerMode);
        reader = sdfReader.getReader();
    }


    /**
     * 构造器方法
     *
     * @param option 读取器参数
     */
    public SDFViewerReader(SDFReaderOption option) throws IOException {
        sdfReader = new SDFReader(option);
        this.reader = sdfReader.getReader();
    }

    @Override
    public Map<String, IndexableSet<String>> getHeader() {
        return new LinkedHashMap<String, IndexableSet<String>>() {
            {
                // 加入索引
                put("INDEX", new LinkedSet<>(new String[]{"INDEX"}));

                // 加入所有的字段信息
                for (FieldGroupMeta fieldGroup : reader.getAllFieldGroups()) {
                    String groupName = fieldGroup.groupName();
                    if (containsKey(groupName)) {
                        int dupIndex = 1;
                        while (true) {
                            String newGroupName = groupName + (groupName.endsWith("_") ? "" : "_") + dupIndex;
                            if (containsKey(newGroupName)) {
                                dupIndex++;
                            } else {
                                groupName = newGroupName;
                                break;
                            }
                        }
                    }

                    put(groupName, new LinkedSet<String>() {
                        {
                            for (FieldMeta field : fieldGroup) {
                                String simpleName = field.simpleName();
                                if (simpleName.equals("metrics") || simpleName.equals("line")){
                                    continue;
                                }
                                add(simpleName);
                            }
                        }
                    });
                }
            }
        };
    }

    @Override
    public Object[] read() throws IOException {
        ISDSV sdsv;
        sdsv = sdfReader.read();
        if (sdsv != null) {
            long index = reader.tell();
            if (sdsv instanceof CompleteSDSV) {
                return ((CompleteSDSV) sdsv).toGuiObject(index);
            }
        }
        return null;
    }

    @Override
    public long numOfRecords() {
        return this.reader.numOfRecords();
    }

    @Override
    public void seek(long pointer) throws IOException {
        this.reader.seek(pointer);
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    public LongInterval range() {
        return this.reader.available();
    }

    @Override
    public long tell() {
        return this.reader.tell();
    }

    @Override
    public CCFTable getTable() {
        return this.reader.getTable();
    }

    @Override
    public String toString() {
        return this.reader.getTable().toString();
    }

    @Override
    public LongInterval available() {
        return reader.available();
    }

}
