package edu.sysu.pmglab.sdfa.annotation.output.frame;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.annotation.output.convertor.ConcatOutputConvertor;
import edu.sysu.pmglab.sdfa.annotation.output.convertor.OutputConvertor;
import edu.sysu.pmglab.sdfa.annotation.output.convertor.OutputConvertorFactory;
import edu.sysu.pmglab.sdfa.annotation.source.SourceMeta;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Wenjie Peng
 * @create 2024-09-18 02:09
 * @description
 */
public class IntervalSourceOutputFrame implements SourceOutputFrame<IRecord> {
    IntList pureLoadIndexes;
    final SourceMeta sourceMeta;
    List<IntList> indexesOfColumns;
    List<Bytes> outputColumnNames;
    TIntSet filterColumns = new TIntHashSet();
    List<OutputConvertor> outputColumnFunctions;

    // flag for specifying the user-defined output functions
    private static final byte[] OPT_FLAG = "opts".getBytes();
    // flag for specifying the names of output columns
    private static final byte[] NAMES_FLAG = "names".getBytes();
    // flag for specifying the names of related columns for output functions from input annotation resources
    private static final byte[] FIELDS_Flag = "fields".getBytes();
    // flag for specifying the indexes of loading columns from input annotation resources
    private static final byte[] COLUMNS_FLAG = "columns".getBytes();
    // flag for specifying the names of loading columns from input annotation resources
    private static final byte[] LOADER_FLAG = "loadCols".getBytes();

    // match any non-empty string enclosed by double quotes and capture the content inside the double quotes into a capture group.
    private static final Pattern pattern = Pattern.compile("\"([^\"]+)\"");

    public IntervalSourceOutputFrame(SourceMeta sourceMeta) {
        this.sourceMeta = sourceMeta;
        indexesOfColumns = new List<>();
        outputColumnNames = new List<>();
        outputColumnFunctions = new List<>();
    }

    public IntervalSourceOutputFrame accept(Bytes configLine) {
        if (configLine.startsWith(COLUMNS_FLAG)) {
            acceptRawColIndexes(configLine);
            return this;
        } else if (configLine.startsWith(FIELDS_Flag)) {
            acceptRawColNames(configLine);
            return this;
        } else if (configLine.startsWith(NAMES_FLAG)) {
            acceptOutputColNames(configLine);
            return this;
        } else if (configLine.startsWith(OPT_FLAG)) {
            acceptOutputFunctions(configLine);
            return this;
        } else if (configLine.startsWith(LOADER_FLAG)) {
            acceptRawLoadColNames(configLine);
            return this;
        }
        throw new UnsupportedOperationException(configLine + " does not start with the valid config flag");
    }

    private IntervalSourceOutputFrame acceptRawLoadColNames(Bytes rawColList) throws UnsupportedOperationException {
        Matcher matcher = pattern.matcher(rawColList.toString());
        while (matcher.find()) {
            String match = matcher.group(1);
            String[] rawNames = match.split(",");
            for (String rawName : rawNames) {
                int indexOfRawCol = sourceMeta.indexOfCol(rawName.trim());
                if (indexOfRawCol == -1) {
                    throw new UnsupportedOperationException("The annotation resource doesn't include " + rawName);
                }
                this.filterColumns.add(indexOfRawCol);
            }
        }
        return this;
    }

    private IntervalSourceOutputFrame acceptRawColNames(Bytes rawNameList) throws UnsupportedOperationException {
        Matcher matcher = pattern.matcher(rawNameList.toString());
        while (matcher.find()) {
            String match = matcher.group(1);
            String[] rawNames = match.split(",");
            IntList indexes = new IntList();
            for (String rawName : rawNames) {
                int indexOfRawCol = sourceMeta.indexOfCol(rawName.trim());
                if (indexOfRawCol == -1) {
                    throw new UnsupportedOperationException("The annotation resource doesn't include " + rawName);
                }
                indexes.add(indexOfRawCol);
            }
            this.indexesOfColumns.add(indexes);
        }
        return this;
    }

    private IntervalSourceOutputFrame acceptRawColIndexes(Bytes rawIndexList) throws UnsupportedOperationException {
        int numOfRawCols = sourceMeta.numOfCols();
        Matcher matcher = pattern.matcher(rawIndexList.toString());
        while (matcher.find()) {
            String match = matcher.group(1);
            String[] rawNames = match.split(",");
            IntList indexes = new IntList();
            for (String rawName : rawNames) {
                int indexOfRawCol;
                try {
                    indexOfRawCol = Integer.parseInt(rawName.trim());
                    if (indexOfRawCol > numOfRawCols) {
                        throw new UnsupportedOperationException("The raw column index in \"" + rawIndexList + "\" is beyond the numuber of annotation columns(" + numOfRawCols + ")");
                    }
                } catch (NumberFormatException e) {
                    throw new UnsupportedOperationException("The " + rawName + " in \"" + rawIndexList + "\" cannot be parsed to integer value");
                }
                indexes.add(indexOfRawCol);
            }
            this.indexesOfColumns.add(indexes);
        }
        return this;
    }

    private IntervalSourceOutputFrame acceptOutputColNames(Bytes outputFields) {
        Matcher matcher = pattern.matcher(outputFields.toString());
        while (matcher.find()) {
            String colName = matcher.group(1);
            this.outputColumnNames.add(new Bytes(colName.trim()));
        }
        return this;
    }

    private IntervalSourceOutputFrame acceptOutputFunctions(Bytes outputFunctions) {
        if (outputFunctions == null) {
            for (int i = 0; i < outputColumnNames.size(); i++) {
                this.outputColumnFunctions.add(new ConcatOutputConvertor(Constant.COLON));
            }
            return this;
        }
        Matcher matcher = pattern.matcher(outputFunctions.toString());
        while (matcher.find()) {
            String functionName = matcher.group(1);
            this.outputColumnFunctions.add(OutputConvertorFactory.getOutputConvertor(functionName));
        }
        return this;
    }

    /**
     * only load which output needs
     *
     * @return prune frame
     */
    public IntervalSourceOutputFrame optimize() {
        this.pureLoadIndexes = new IntList();
        TIntHashSet columnIndexes = new TIntHashSet();
        columnIndexes.addAll(filterColumns);
        for (IntList indexesOfColumn : indexesOfColumns) {
            for (int i = 0; i < indexesOfColumn.size(); i++) {
                columnIndexes.add(indexesOfColumn.fastGet(i));
            }
        }
        for (int columnIndex : columnIndexes.toArray()) {
            pureLoadIndexes.add(columnIndex);
        }
        pureLoadIndexes.sort();
        TIntIntMap map = new TIntIntHashMap();
        for (int i = 0; i < pureLoadIndexes.size(); i++) {
            map.put(pureLoadIndexes.fastGet(i), i);
        }
        for (IntList indexesOfColumn : indexesOfColumns) {
            for (int i = 0; i < indexesOfColumn.size(); i++) {
                indexesOfColumn.set(i, map.get(indexesOfColumn.get(i)));
            }
        }
        return this;
    }

    public Bytes buildEmptyAnnotation() {
        ByteStream cache = new ByteStream();
        int size = outputColumnNames.size();
        for (int i = 0; i < size; i++) {
            cache.write(Constant.PERIOD);
            if (i != size - 1) {
                cache.write(Constant.TAB);
            }
        }
        Bytes emptyAnnotation = cache.toBytes().detach();
        cache.close();
        return emptyAnnotation;
    }

    public void write(List<IRecord> records, ByteStream cache) {
        int size = outputColumnFunctions.size();
        for (int i = 0; i < size; i++) {
            cache.write(outputColumnFunctions.fastGet(i).output(records, indexesOfColumns.get(i)));
            if (i != size - 1) {
                cache.write(Constant.TAB);
            }
        }
    }

    public List<Bytes> getOutputColumnNames() {
        return outputColumnNames;
    }

    public IntList getPureLoadIndexes() {
        return pureLoadIndexes;
    }

    public Bytes getHeader() {
        int size = outputColumnNames.size();
        ByteStream cache = new ByteStream();
        for (int i = 0; i < size; i++) {
            cache.write(outputColumnNames.fastGet(i));
            if (i != size - 1) {
                cache.write(Constant.TAB);
            }
        }
        return cache.toBytes();
    }

}
