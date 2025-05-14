package edu.sysu.pmglab.sdfa.annotation.preprocess;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.StringSplitter;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.executor.Pipeline;
import edu.sysu.pmglab.executor.Workflow;
import edu.sysu.pmglab.io.FileUtils;
import edu.sysu.pmglab.io.file.LiveFile;
import edu.sysu.pmglab.io.reader.ReaderStream;
import edu.sysu.pmglab.progressbar.ProgressBar;
import edu.sysu.pmglab.sdfa.annotation.output.frame.IntervalSourceOutputFrame;
import edu.sysu.pmglab.sdfa.annotation.source.GenomeSource;
import edu.sysu.pmglab.sdfa.annotation.source.Source;
import edu.sysu.pmglab.sdfa.annotation.source.SourceManager;

import java.io.File;
import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-22 21:08
 * @description
 */
public class ConfigInput {
    LiveFile configPath;
    final File outputDir;
    boolean silent = false;
    private static final Bytes ANNOTATION_RESOURCE_BLOCK_HEADER = new Bytes("[[annotation]]");

    public ConfigInput(File outputDir, LiveFile configPath) {
        this.outputDir = outputDir;
        this.configPath = configPath;
        AnnotationConfig.outputDir = this.outputDir;
    }

    public ConfigInput(String outputDir, LiveFile configPath) {
        this.outputDir = new File(outputDir);
        this.outputDir.mkdirs();
        this.configPath = configPath;
        AnnotationConfig.outputDir = this.outputDir;
    }

    /**
     * annotation resource prepare: load annotation resources and initialize to CCF files
     *
     * @return
     * @throws IOException
     */
    public List<Pipeline> annotationResourcePrepareTasks() throws IOException {
        List<Pipeline> tasks = new List<>();
        ByteStream cache = new ByteStream();
        Logger logger = LogBackOptions.getRootLogger();
        //region parse annotation config file
        ReaderStream readerStream = configPath.openAsText();
        List<List<Bytes>> configOfResources = new List<>();
        boolean hasNext;
        while (cache.toBytes().startsWith(ANNOTATION_RESOURCE_BLOCK_HEADER) || readerStream.readline(cache) != -1) {
            List<Bytes> annotation = new List<>();
            cache.clear();
            while ((hasNext = (readerStream.readline(cache) != -1))) {
                Bytes line = cache.toBytes();
                if (line.length() == 0||line.byteAt(0) == Constant.NUMBER_SIGN) {
                    cache.clear();
                    continue;
                }
                if (!cache.toBytes().startsWith(ANNOTATION_RESOURCE_BLOCK_HEADER)) {
                    Bytes item = cache.toBytes();
                    if (item.length() != 0) {
                        annotation.add(cache.toBytes().detach());
                        cache.clear();
                        continue;
                    }
                }
                break;
            }
            if (!annotation.isEmpty()) {
                configOfResources.add(annotation);
            }
            if (!hasNext) {
                break;
            }
        }
        cache.close();
        readerStream.close();
        int configSize = configOfResources.size();
        List<AnnotationConfig> resourceConfigs = new List<>(configSize);
        for (int i = 0; i < configSize; i++) {
            resourceConfigs.add(new AnnotationConfig(configOfResources.fastGet(i)));
        }
        //endregion
        if (!silent) {
            StringBuilder resourceOptions = new StringBuilder();
            resourceOptions.append("Collect ").append(configSize).append(" resources from config:\n");
            for (int i = 0; i < configSize; i++) {
                resourceOptions.append(resourceConfigs.fastGet(i).toString());
                if (i != configSize - 1) {
                    resourceOptions.append("\n");
                }
            }
            logger.info(resourceOptions.toString());
            logger.info("Start to prepare annotation resources.");
            tasks.add(new Pipeline((status, context) -> context.put(
                    ProgressBar.class,
                    new ProgressBar.Builder().setTextRenderer("Resource Parsing and Loading Speed", "file(s)")
                            .setInitialMax(configSize)
                            .build()
            )));
        }
        for (int i = 0; i < configSize; i++) {
            int finalI = i;
            tasks.add((new Pipeline(
                    (status, context) -> {
                        SourceManager.addSource(resourceConfigs.fastGet(finalI).toSource());
                        Object o = context.get(ProgressBar.class);
                        if (o != null) {
                            ((ProgressBar) o).step(1);
                        }
                    })));
        }
        tasks.add(new Pipeline(true, ((status, context) -> {
            Object o = context.get(ProgressBar.class);
            if (o != null) {
                ((ProgressBar) o).close();
            }
        })));
        return tasks;
    }

    static class AnnotationConfig {
        String type;
        // output dir has the same name with raw file
        boolean warn;
        LiveFile file;
        File loadFile;
        static int index = 1;
        static File outputDir;
        List<Bytes> config;

        public AnnotationConfig(List<Bytes> allConfigs) {
            // resource file
            Bytes fileConfig = allConfigs.fastGet(0);
            try {
                this.file = LiveFile.of(fileConfig.subBytes(5, fileConfig.length()).trim().toString());
            } catch (IOException e) {
                throw new UnsupportedOperationException("Please check the file path: " + fileConfig);
            }
            loadFile = FileUtils.getSubFile(outputDir, file.getName() + ".ccf");
            // resource type
            Bytes typeConfig = allConfigs.fastGet(1).subBytes(5, allConfigs.fastGet(1).length());
            this.type = typeConfig.toString();
            // resource output settings
            config = new List<>();
            for (int i = 2; i < allConfigs.size(); i++) {
                config.add(allConfigs.fastGet(i));
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(index++);
            builder.append(".\t");
            builder.append(file.getName());
            builder.append("\n\tType:\t");
            builder.append(type);
            if (!config.isEmpty()) {
                builder.append("\n\tConfig:\t");
                int size = config.size();
                for (int i = 0; i < size; i++) {
                    builder.append(config.fastGet(i).toString());
                    if (i != size - 1) {
                        builder.append(';');
                    }
                }
            }
            return builder.toString();
        }

        public Source toSource() throws IOException {
            Source source = SourceConvertorFactory.createSourceConvertor(type, file, outputDir).convert();
            if (!(source instanceof GenomeSource)) {
                // configure the output format
                IntervalSourceOutputFrame frame = new IntervalSourceOutputFrame(source.getSourceMeta());
                for (int i = 0; i < config.size(); i++) {
                    frame.accept(config.fastGet(i));
                }
                source.setFrame(frame);
            }
            return source;
        }
    }

    public static void main(String[] args) throws IOException {
        LogBackOptions.init();
        Workflow workflow = new Workflow(4);
        ConfigInput configInput = new ConfigInput(
                "/Users/wenjiepeng/Desktop/SDFA3.0/annotation",
                LiveFile.of("/Users/wenjiepeng/Desktop/SDFA3.0/annotation/config.txt")
        );
        List<Pipeline> pipelines = configInput.annotationResourcePrepareTasks();
        for (Pipeline pipeline : pipelines) {
            workflow.addTask(pipeline);
        }

        workflow.execute();
    }
}
