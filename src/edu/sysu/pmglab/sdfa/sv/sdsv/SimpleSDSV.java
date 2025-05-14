package edu.sysu.pmglab.sdfa.sv.sdsv;

import edu.sysu.pmglab.bytecode.ASCIIUtility;
import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.easytools.constant.GenotypeConstant;
import edu.sysu.pmglab.sdfa.sv.CSVLocation;
import edu.sysu.pmglab.sdfa.sv.SDSVConversionManager;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;

/**
 * @author Wenjie Peng
 * @create 2024-09-04 02:22
 * @description load partial attributes of SV in some specific conditions, like loading only coordinate in annotation
 */
public class SimpleSDSV implements ISDSV {
    protected int length;
    protected SVTypeSign svTypeSign;
    protected SVCoordinate coordinate;
    protected CSVLocation csvLocation;

    private static int[] EMPTY_INT_LIST = new int[0];
    private static Bytes EMPTY_BYTECODE = new Bytes(new byte[]{Constant.PERIOD});
    private static Bytes[] EMPTY_QUALITY_MERTICS = new Bytes[0];

    public SimpleSDSV() {
    }

    public SimpleSDSV(SVCoordinate coordinate, SVTypeSign type) {
        this.svTypeSign = type;
        this.coordinate = coordinate;
    }

    public SimpleSDSV setLength(int length) {
        this.length = length;
        return this;
    }


    public int getLength() {
        return length;
    }

    public SVTypeSign getSvTypeSign() {
        return svTypeSign;
    }

    public SVCoordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public void parseRecord(IRecord record, SDSVConversionManager conversionManager) {
        this.coordinate = SVCoordinate.decode(record.get(0));
        this.length = record.get(1);
        this.svTypeSign = SVTypeSign.getByIndex(record.get(2));
        this.csvLocation = new CSVLocation(record.get(3), (IntList) record.get(4));
    }

    @Override
    public int numOfSubSVs() {
        return csvLocation.numOfItem();
    }

    @Override
    public int getContigIndex() {
        return coordinate.getIndexOfChr();
    }

    @Override
    public IntInterval getCoordinateInterval() {
        return coordinate.getCoordinateInterval();
    }

    @Override
    public int getEnd() {
        return coordinate.getEnd();
    }

    @Override
    public SVTypeSign getType() {
        return svTypeSign;
    }

    @Override
    public int getPos() {
        return coordinate.getPos();
    }

    @Override
    public boolean spanContig() {
        return svTypeSign.spanContig();
    }

    @Override
    public String getNameOfType() {
        return svTypeSign.getName();
    }

    @Override
    public CSVLocation getCsvLocation() {
        return csvLocation;
    }

    @Override
    public void writeTo(ByteStream cache) {
        cache.write(ASCIIUtility.toASCII(coordinate.getChr(),Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(ASCIIUtility.toASCII(coordinate.getPos()));
        cache.write(Constant.TAB);
        cache.write(ASCIIUtility.toASCII(coordinate.getEnd()));
        cache.write(Constant.TAB);
        cache.write(ASCIIUtility.toASCII(svTypeSign.getName(),Constant.CHAR_SET));
        cache.write(Constant.TAB);
        cache.write(ASCIIUtility.toASCII(length));
    }

    @Override
    public ISDSV setChrName(String contigName) {
        coordinate.setChr(contigName);
        return this;
    }

    public int length() {
        return length;
    }

    @Override
    public String nameOfContig() {
        return coordinate.getChr();
    }

    @Override
    public int indexInFile() {
        return csvLocation.indexInFile();
    }

    @Override
    public IRecord toRecord(IRecord record, int indexInFile) {
        return record.set(0, IntList.wrap(coordinate.getPos(), coordinate.getEnd()))
                .set(1, length)
                .set(2, svTypeSign.getIndex())
                .set(3, GenotypeConstant.EMPTY_GTY_ENCODE)
                .set(4, EMPTY_QUALITY_MERTICS)
                // id
                .set(5, EMPTY_BYTECODE)
                // ref
                .set(6, EMPTY_BYTECODE)
                // alt
                .set(7, EMPTY_BYTECODE)
                // qual
                .set(8, EMPTY_BYTECODE)
                // filter
                .set(9, EMPTY_BYTECODE)
                // infoField
                .set(10, EMPTY_QUALITY_MERTICS)
                .set(11, indexInFile)
                .set(12, EMPTY_INT_LIST)
                .set(13, EMPTY_INT_LIST);

    }

    @Override
    public SimpleSDSV setCoordinate(SVCoordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }

    @Override
    public SimpleSDSV setType(SVTypeSign type) {
        this.svTypeSign = type;
        return this;
    }
}
