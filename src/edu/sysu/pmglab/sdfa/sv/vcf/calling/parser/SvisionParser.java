package edu.sysu.pmglab.sdfa.sv.vcf.calling.parser;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.bytecode.BytesSplitter;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.sdfa.sv.SVContig;
import edu.sysu.pmglab.sdfa.sv.SVCoordinate;
import edu.sysu.pmglab.sdfa.sv.vcf.VCFInfoManager;
import edu.sysu.pmglab.sdfa.sv.vcf.calling.AbstractCallingParser;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 22:23
 * @description
 */
public class SvisionParser extends AbstractCallingParser {
    public static final byte[] CSV_BYTES = new byte[]{60, Constant.C, Constant.S, Constant.V};
    public static final Bytes BKPS_BYTECODE = new Bytes(new byte[]{Constant.B, Constant.K, Constant.P, Constant.S});

    @Override
    public boolean parseInfo(VCFInfoManager infoManager, List<SVCoordinate> coordinateList, SVContig contig) {
        // BKPS=TYPE1:LEN1-START1-END1,TYPE2-LEN2-START2-END2,...
        Bytes bkps = infoManager.getAttrValue(BKPS_BYTECODE);
        int index = 0, length = bkps.length();
        while (index < length) {
            // 解析TYPE（直到遇到':'）
            StringBuilder typeBuilder = new StringBuilder();
            while (index < length && bkps.byteAt(index) != Constant.COLON) {
                typeBuilder.append((char) bkps.byteAt(index));
                index++;
            }
            String type = typeBuilder.toString();
            // 跳过':'
            index++;

            // 解析LEN（直到遇到'-'）
            StringBuilder lenBuilder = new StringBuilder();
            while (index < length && bkps.byteAt(index) != Constant.MINUS) {
                lenBuilder.append((char) bkps.byteAt(index));
                index++;
            }
            int len = bkps.subBytes(index - lenBuilder.length(), index).toInt();
            // 跳过'-'
            index++;

            // 解析START（直到遇到'-'）
            StringBuilder startBuilder = new StringBuilder();
            while (index < length && bkps.byteAt(index) != Constant.MINUS) {
                startBuilder.append((char) bkps.byteAt(index));
                index++;
            }
            int start = bkps.subBytes(index - startBuilder.length(), index).toInt();
            // 跳过'-'
            index++;

            // 解析END（直到遇到','或结束）
            StringBuilder endBuilder = new StringBuilder();
            while (index < length && bkps.byteAt(index) != Constant.COMMA) {
                endBuilder.append((char) bkps.byteAt(index));
                index++;
            }
            int end = bkps.subBytes(index - endBuilder.length(), index).toInt();

            // 在这里处理解析出的一组数据
            coordinateList.add(new SVCoordinate(start, end, infoManager.getIndexContigIndex()));

            // 如果还有下一组数据，跳过','继续解析
            if (index < length && bkps.byteAt(index) == Constant.COMMA) {
                index++; // 跳过','
            }
        }
        return true;

    }
}
