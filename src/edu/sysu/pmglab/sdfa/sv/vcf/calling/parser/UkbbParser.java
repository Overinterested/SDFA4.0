package edu.sysu.pmglab.sdfa.sv.vcf.calling.parser;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;
import edu.sysu.pmglab.sdfa.sv.vcf.calling.AbstractCallingParser;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 22:23
 * @description
 */
public class UkbbParser extends AbstractCallingParser {

    @Override
    public boolean parseInfo(VCFInfoManager infoManager, List<SVCoordinate> coordinateList, SVContig contig) {
        SVCoordinate initCoordinate = coordinateList.get(0);
        int end = infoManager.getEnd();
        initCoordinate.setEnd(end);
        return true;
    }

    @Override
    public void replaceDetectAttr(VCFInfoManager infoManager) {
        VCFInfoManager.SVLEN = VCFInfoManager.SVSIZE;
    }
}
