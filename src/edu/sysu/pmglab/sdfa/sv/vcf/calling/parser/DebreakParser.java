package edu.sysu.pmglab.sdfa.sv.vcf.calling.parser;

import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;
import edu.sysu.pmglab.sdfa.sv.vcf.calling.AbstractCallingParser;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 22:21
 * @description
 */
public class DebreakParser extends AbstractCallingParser {

    @Override
    public boolean parseInfo(VCFInfoManager infoManager, List<SVCoordinate> coordinateList, SVContig contig) {
        return super.parseInfo(infoManager, coordinateList, contig);
    }


}