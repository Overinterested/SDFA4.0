package edu.sysu.pmglab.sdfa.sv;

import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.interval.IntInterval;
import edu.sysu.pmglab.container.list.IntList;

import java.util.Comparator;

/**
 * @author Wenjie Peng
 * @create 2024-08-25 20:23
 * @description
 */
public class SVCoordinate implements Comparable<SVCoordinate> {
    int pos;
    int end;
    String chr;
    int indexOfChr;
    public static final Comparator<IRecord> encoderSDSVComparator = (o1, o2) -> {
        SVCoordinate var1 = SVCoordinate.decode(o1.get(0));
        SVCoordinate var2 = SVCoordinate.decode(o2.get(0));
        return var1.compareTo(var2);
    };

    public SVCoordinate(int pos, int end, int indexOfChr) {
        this.pos = pos;
        this.end = end;
        this.indexOfChr = indexOfChr;
    }

    public SVCoordinate(int pos, int end, String chr) {
        this.pos = pos;
        this.end = end;
        this.chr = chr;
    }

    @Override
    public int compareTo(SVCoordinate o) {
        int status = Integer.compare(indexOfChr, o.indexOfChr);
        if (status == 0) {
            status = Integer.compare(pos, o.pos);
            return status == 0 ? Integer.compare(end, o.end) : status;
        }
        return status;
    }

    public SVCoordinate setIndexOfChr(int indexOfChr) {
        this.indexOfChr = indexOfChr;
        return this;
    }

    public SVCoordinate setChr(String chr) {
        this.chr = chr;
        return this;
    }

    public SVCoordinate setPos(int pos) {
        this.pos = pos;
        return this;
    }

    public SVCoordinate setEnd(int end) {
        this.end = end;
        return this;
    }

    public String getChr() {
        return chr;
    }

    public int getIndexOfChr() {
        return indexOfChr;
    }

    public int getPos() {
        return pos;
    }

    public int[] encode() {
        return new int[]{indexOfChr, pos, end};
    }

    public static SVCoordinate decode(IntList coordinate) {
        return new SVCoordinate(coordinate.get(1), coordinate.get(2), coordinate.get(0));
    }

    public int getEnd() {
        return end;
    }

    public IntInterval getCoordinateInterval() {
        return new IntInterval(pos, end == -1 ? pos + 1 : end);
    }

    @Override
    public String toString() {
        if (end == -1) {
            return chr + ":" + pos;
        }
        return chr + ":" + pos + "~" + end;
    }
}
