package edu.sysu.pmglab.easytools;

import edu.sysu.pmglab.bytecode.Bytes;

/**
 * @author Wenjie Peng
 * @create 2025-04-27 08:41
 * @description
 */
public enum VCFHeaderItem {
    CHR("CHR" ),
    ID("ID" ),
    REF("REF" ),
    ALT("ALT" ),
    QUAL("QUAL" ),
    FILTER("FILTER" ),
    INFO("INFO" ),
    FORMAT("FORMAT" ),
    GT("GT" ),;

    String itemString;
    Bytes itemBytes;

    VCFHeaderItem(String itemString) {
        this.itemString = itemString;
        this.itemBytes = new Bytes(itemString);
    }

    public String getItemString() {
        return itemString;
    }

    public Bytes getItemBytes() {
        return itemBytes;
    }
}