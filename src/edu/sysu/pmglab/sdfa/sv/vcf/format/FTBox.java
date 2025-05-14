package edu.sysu.pmglab.sdfa.sv.vcf.format;

import edu.sysu.pmglab.bytecode.Bytes;

/**
 * @author Wenjie Peng
 * @create 2024-12-18 00:32
 * @description
 */
public class FTBox extends SingleStringValueBox.SingleStringValueBoxInstance {
    private static final String EXIST = ".";
    private static final Bytes PASS = new Bytes("PASS");

    @Override
    public String getBriefName() {
        return "FT";
    }

    @Override
    public String getDescription() {
        return "Filter. PASS or FAILN where N is a number";
    }

    @Override
    public SingleStringValueBox newInstance() {
        return new FTBox();
    }

    @Override
    public void loadOne(Bytes item) {
        if (item.equals(PASS)) {
            value.add(EXIST);
        }else {
            value.add(item.toString());
        }
    }

    @Override
    public Object getByIndex(int index) {
        String item = value.fastGet(index);
        if (item.equals(EXIST)){
            return PASS;
        }
        return item;
    }
    @Override
    public void forceLoadOne(Object item) {
        if (item.equals(PASS)) {
            value.add(EXIST);
        }else {
            value.add(item.toString());
        }
    }
}
