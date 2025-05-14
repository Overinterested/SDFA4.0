package edu.sysu.pmglab.sdfa.sv.vcf.format;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Wenjie Peng
 * @create 2024-12-19 20:48
 * @description
 */
public enum FormatAttrType {
    AD(new ADBox()),
    DP(new DPBox()),
    DR(new DRBox()),
    DV(new DVBox()),
    FT(new FTBox()),
    GQ(new GQBox()),
    MD(new MDBox()),
    PL(new PLBox()),
    PP(new PPBox()),
    RA(new RABox()),
    GT(new GTBox());
    FormatAttrBox box;

    FormatAttrType(FormatAttrBox box) {
        this.box = box;
    }

    static ConcurrentHashMap<String, FormatAttrBox> formatAttrTypeList = new ConcurrentHashMap<>();

    static {
        FormatAttrType[] values = FormatAttrType.values();
        for (FormatAttrType value : values) {
            formatAttrTypeList.put(value.box.getBriefName(), value.box);
        }
    }

    public static void addFormatAttrType(FormatAttrBox attrBox) {
        formatAttrTypeList.put(attrBox.getBriefName(), attrBox.newInstance());
    }

    public static FormatAttrBox getByName(String briefName) {
        FormatAttrBox box = formatAttrTypeList.get(briefName);
        if (box == null) {
            return new SingleStringValueBox.SingleStringValueBoxInstance()
                    .setBriefName(briefName)
                    .setDescription(null);
        }else {
            box = box.newInstance();
        }
        return box;
    }
}
