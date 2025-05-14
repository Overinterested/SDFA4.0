package edu.sysu.pmglab.sdfa.sv.vcf.calling;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.SVTypeSign;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 21:13
 * @description
 */
public abstract class AbstractCallingParser {
    /**
     * extract coordinates from info manager and return false when parsing wrong
     *
     * @param infoManager    information manager which stores the key-value entries of INFO
     * @param coordinateList store the coordinates
     * @param contig         the reference contig
     * @return whether it is successful
     */
    public boolean parseInfo(VCFInfoManager infoManager, List<SVCoordinate> coordinateList, SVContig contig) {
        SVCoordinate initCoordinate = coordinateList.get(0);
        int end = infoManager.getEnd();
        SVTypeSign type = infoManager.getType();
        if (type.isComplex()) {
            int indexOfContig2;
            Bytes contig2 = infoManager.getContig2();
            String contigName;
            if (contig2 != null) {
                contigName = contig2.toString();
                indexOfContig2 = contig.getContigIndexByName(contigName);
            } else {
                indexOfContig2 = initCoordinate.getIndexOfChr();
            }
            initCoordinate.setEnd(-1);
            coordinateList.add(new SVCoordinate(end, -1, indexOfContig2));
        } else {
            initCoordinate.setEnd(end);
        }
        return true;
    }

    /**
     * help replace the detection attribution of VCF info, like END, SVLEN and so on.
     * @param infoManager
     */
    public void replaceDetectAttr(VCFInfoManager infoManager){
        return;
    }
}
