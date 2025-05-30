package edu.sysu.pmglab.easytools.identify;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.ccf.CCFReader;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.executor.*;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.writer.WriterStream;
import edu.sysu.pmglab.sdfa.SDFReader;
import edu.sysu.pmglab.sdfa.annotation.source.GenomeSource;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import edu.sysu.pmglab.sdfa.annotation.source.record.SourceRNARecord;
import edu.sysu.pmglab.sdfa.mode.IReaderMode;
import edu.sysu.pmglab.sdfa.mode.SDFReadType;
import edu.sysu.pmglab.sdfa.nagf.reference.RefRNAElement;
import edu.sysu.pmglab.sdfa.sv.SVContig;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wenjie Peng
 * @create 2025-05-28 12:48
 * @description
 */
public class SDFRelatedGeneIdentify {
    File outputFile;
    File inputSDFDir;
    String contigName;
    final File geneFile;
    final String geneName;
    IntList indexListInGeneFile = new IntList();
    AtomicInteger SVCount = new AtomicInteger(0);


    public SDFRelatedGeneIdentify(File geneFile, String geneName) {
        this.geneFile = geneFile;
        this.geneName = geneName;
    }

    public SDFRelatedGeneIdentify(File geneFile) {
        this.geneFile = geneFile;
        this.geneName = null;
    }

    public SDFRelatedGeneIdentify setContigName(String contigName) {
        this.contigName = contigName;
        return this;
    }

    public SDFRelatedGeneIdentify setInputSDFDir(File inputSDFDir) {
        this.inputSDFDir = inputSDFDir;
        return this;
    }

    public SDFRelatedGeneIdentify setOutputFile(File outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    public void submitTo(Workflow workflow) {
        List<File> files = FileUtils.listFiles(inputSDFDir, file -> file.getName().endsWith(".sdf"));
        if (files == null || files.isEmpty()) {
            throw new UnsupportedOperationException("There are no SDF files in input directory.");
        }
        workflow.addTask(new Pipeline((status, context) -> {
            CCFReader reader = new CCFReader(geneFile);
            SourceMeta meta = SourceMeta.load(reader.getReaderOption().getTable().getMeta());
            if (contigName != null) {
                SVContig init = SVContig.init();
                String contig = init.getContigNameByIndex(init.getContigIndexByName(contigName));
                IntInterval rangeByName = meta.getRangeByName(contig);
//                if (rangeByName == null || rangeByName.end() == rangeByName.start()) {
//                    context.put("valid contig", false);
//                    return;
//                }
                reader.limit(rangeByName.start(), rangeByName.end());
            }
            IRecord record = reader.getRecord();
            while (reader.read(record)) {
                SourceRNARecord sourceRNARecord = SourceRNARecord.load(record);
                if (sourceRNARecord.getNameOfGene().equals(geneName)) {
                    indexListInGeneFile.add((int) (reader.tell() - 1));
                }
            }
            if (!indexListInGeneFile.isEmpty()) indexListInGeneFile.sort();
            reader.close();
        }));
        workflow.execute();
        workflow.clearTasks();
        for (File file : files) {
            workflow.addTask(seek(file));
        }
        workflow.execute();
        workflow.clearTasks();
    }

    private synchronized static void write(WriterStream writerStream, ByteStream cache) throws IOException {
        writerStream.write(cache.toBytes());
        cache.clear();
    }

    private Pipeline seek(File sdfFile) {
        return new Pipeline((status, context) -> {
            IRecord record;
            WriterStream writerStream = (WriterStream) context.get(WriterStream.class);
            SDFReader reader = new SDFReader(sdfFile, SDFReadType.ANNOTATION_SEEK);
            if (contigName != null) {
                SDFReader limit = reader.limit(contigName);
                if (limit == null) {
                    reader.closeAll();
                    return;
                }
            }
            SDFReader full = new SDFReader(sdfFile);
            ByteStream cache = new ByteStream();
            while ((record = reader.readRecord()) != null) {
                IntList range = record.get(0);
                if (range.isEmpty()){
                    continue;
                }
                int flag = isIntervalContainingAny(range.toArray(), indexListInGeneFile);
                if (flag == -1) {
                    break;
                } else if (flag == 0) {
                    full.seek(reader.getReader().tell());
                    full.read().toVCFRecord(cache);
                    write(writerStream, cache);
                    SVCount.incrementAndGet();
                } else {
                    int a = 1;
                }
            }
            full.closeAll();
            reader.closeAll();
        });
    }

    private static int isIntervalContainingAny(int[] range, IntList values) {
        // 参数校验
        if (range == null || range.length != 2 || values == null || values.size() == 0) {
            return -1; // 可以考虑用其他值表示参数错误
        }

        int left = range[0];
        int right = range[1];

        // 区间有效性检查
        if (left > right) {
            return -1;
        }

        // 检查第一个数是否超过range的左端
        if (values.fastGet(0) > right) {
            return -1;
        }

        // 使用二分查找找到第一个 >= left 的元素
        int low = 0;
        int high = values.size();

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (values.fastGet(mid) < left) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        // 检查是否至少有一个数在区间内
        if (low < values.size() && values.fastGet(low) <= right) {
            return 0; // 至少有一个数在区间内
        }

        return 1; // 没有数在区间内
    }

}
