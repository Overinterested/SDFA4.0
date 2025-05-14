package edu.sysu.pmglab.sdfa.sv.csv;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.sdsv.ISDSV;

/**
 * @author Wenjie Peng
 * @create 2024-08-26 01:22
 * @description store one complex SV
 */
public class ComplexSV implements Comparable<ComplexSV> {
    int fileID;
    int indexOfFile;
    SVTypeSign type;
    List<ISDSV> items = new List<>();

    public static ComplexSV of(List<? extends ISDSV> svs) {
        ComplexSV complexSV = new ComplexSV();
        for (ISDSV sv : svs) {
            complexSV.items.add(sv);
        }
        complexSV.fileID = svs.fastGet(0).getFileID();
        return complexSV;

    }

    public ComplexSV bind(ISDSV sv) {
        items.add(sv);
        return this;
    }

    public ComplexSV setItems(List<ISDSV> svs) {
        this.items = svs;
        return this;
    }

    public int numOfSubSVs() {
        return items.size();
    }

    public int indexOfFile() {
        if (indexOfFile == -1) {
            indexOfFile = items.get(0).getCsvLocation().indexInFile();
        }
        return indexOfFile;
    }

    public List<ISDSV> getSVs() {
        return items;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Complex SV contains ").append(items.size()).append(" subSVs, ");
        for (int i = 0; i < items.size(); i++) {
            sb.append(i).append(" SV is in ");
            sb.append(items.get(i).getCoordinate().getChr());
            if (i != items.size() - 1) {
                sb.append(", ");
            } else {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    public SVTypeSign getType() {
        return items.get(0).getType();
    }

    public ComplexSV setType(SVTypeSign type) {
        this.type = type;
        return this;
    }

    @Override
    public int compareTo(ComplexSV o) {
        int min = Math.min(items.size(), o.items.size());
        for (int i = 0; i < min; i++) {
            int status = items.get(i).getCoordinate().compareTo(o.items.get(i).getCoordinate());
            if (status != 0) {
                return status;
            }
        }
        return Integer.compare(items.size(), o.items.size());
    }

    public String getNameOfType(){
        return items.fastGet(0).getNameOfType();
    }


    public String getContigName(int index){
        return items.fastGet(index).nameOfContig();
    }
}
