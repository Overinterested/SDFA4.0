package edu.sysu.pmglab.easytools.transfer;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Wenjie Peng
 * @create 2024-11-28 20:03
 * @description
 */
public class FileTransfer {
    File mapFile;
    final File inputDir;
    File defaultOutputDir;
    HashMap<String, File> dividedNameOutputDirMap = new HashMap<>();

    public FileTransfer(String inputDir) {
        this.inputDir = new File(inputDir);
    }

    public void submit() throws IOException, InterruptedException {
        ByteStream cache = new ByteStream();
        HashMap<String, String> nameToDividedNameMap = new HashMap<>();

        ReaderStream readerStream = LiveFile.of(mapFile).openAsText();
        while (readerStream.readline(cache) != -1) {
            List<Bytes> split = new List<>();
            Iterator<Bytes> iterator = cache.toBytes().split(Constant.TAB);
            while (iterator.hasNext()) split.add(iterator.next().detach());

            nameToDividedNameMap.put(split.fastGet(0).toString(), split.fastGet(1).toString());
            cache.clear();
        }
        readerStream.close();


        List<String> dividedNames = new List<>(nameToDividedNameMap.keySet());

        int size = dividedNames.size();
        File[] files = inputDir.listFiles();
        for (File file : files) {
            String name = file.getName();
            for (int i = 0; i < size; i++) {
                String tmp = dividedNames.fastGet(i);
                if (name.contains(tmp)) {
                    String tag = nameToDividedNameMap.get(tmp);
                    File tagOutputFie = dividedNameOutputDirMap.get(tag);
                    file.renameTo(FileUtils.getSubFile(tagOutputFie, name));
                    break;
                }
            }
        }
    }

    public FileTransfer setDefaultOutputDir(String defaultOutputDir) {
        this.defaultOutputDir = new File(defaultOutputDir);
        return this;
    }

    public FileTransfer add(String name, String outputDir) {
        dividedNameOutputDirMap.put(name, new File(outputDir));
        return this;
    }

    public FileTransfer setMapFile(String mapFile) {
        this.mapFile = new File(mapFile);
        return this;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new FileTransfer("/Users/wenjiepeng/Desktop/SV/data/private/disease/eye_hospital/sniffles2")
                .add("Glaucoma", "/Users/wenjiepeng/Desktop/SDFA3.0/nagf/data/Glaucoma/sniffles2/vcf")
                .add("Exophthalmos", "/Users/wenjiepeng/Desktop/SDFA3.0/nagf/data/Exophthalmos/sniffles2/vcf")
                .add("35data", "/Users/wenjiepeng/Desktop/SDFA3.0/nagf/data/35data/sniffles2/vcf")
                .setMapFile("/Users/wenjiepeng/Desktop/SV/data/private/disease/eye_hospital/cuteSV_disease.txt")
                .submit();

    }
}
