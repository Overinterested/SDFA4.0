package edu.sysu.pmglab.sdfa.sv.vcf;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 21:19
 * @description
 */
public class VCFHeaderManager {
    static boolean store = true;
    List<Bytes> header = new List<>();
    List<Bytes> infoNameSet = new List<>();
    List<String> contigNameSet = new List<>();
    List<String> subjectNameSet = new List<>();
    List<Bytes> formatNameSet = new List<>();

    // contig
    private static final byte[] CONTIG_HEADER = new byte[]{Constant.NUMBER_SIGN, Constant.NUMBER_SIGN,
            Constant.c, Constant.o, Constant.n, Constant.t, Constant.i, Constant.g};
    private static final Pattern contig_patter = Pattern.compile("##contig=<ID=(?<id>[^,]+),length=(?<length>\\d+)>");
    // info
    private static final byte[] INFO_HEADER = new byte[]{Constant.NUMBER_SIGN, Constant.NUMBER_SIGN,
            Constant.I, Constant.N, Constant.F, Constant.O};
    private static final Pattern info_pattern = Pattern.compile("##INFO=<ID=(?<id>\\w+),(?<fields>(?:[^,>]+=[^,>]+,?)+)Description=\"(?<description>[^\"]+)\">");
    // format
    private static final byte[] FORMAT_HEADER = new byte[]{Constant.NUMBER_SIGN, Constant.NUMBER_SIGN,
            Constant.F, Constant.O, Constant.R, Constant.M, Constant.A, Constant.T};
    private static final Pattern format_pattern = Pattern.compile("##FORMAT=<ID=(?<id>\\w+),(?<fields>(?:[^,>]+=[^,>]+,?)+)Description=\"(?<description>[^\"]+)\">");
    // subject
    private static final byte[] CHR_HEADER = new byte[]{Constant.NUMBER_SIGN, Constant.C, Constant.H};

    public void parse(Bytes src) {
        if (store) {
            header.add(src.detach());
        }
        if (src.startsWith(CONTIG_HEADER)) {
            Bytes name = parseName(src, contig_patter);
            if (name != null) {
                contigNameSet.add(name.toString());
            }
        } else if (src.startsWith(INFO_HEADER)) {
            Bytes name = parseName(src, info_pattern);
            if (name != null) {
                infoNameSet.add(name);
            }
        } else if (src.startsWith(FORMAT_HEADER)) {
            Bytes name = parseName(src, format_pattern);
            if (name != null) {
                formatNameSet.add(name);
            }
        } else if (src.startsWith(CHR_HEADER)) {
            Iterator<Bytes> fileHeader = src.split(Constant.TAB);
            int count = 0;
            while(fileHeader.hasNext()){
                Bytes item = fileHeader.next();
                if (count++<9){
                    continue;
                }
                subjectNameSet.add(item.toString());
            }
        }
    }

    public List<Bytes> getInfoNameSet() {
        return infoNameSet;
    }

    public List<String> getContigNameSet() {
        return contigNameSet;
    }

    public List<Bytes> getFormatNameSet() {
        return formatNameSet;
    }

    public List<String> getSubjectNameSet() {
        return subjectNameSet;
    }

    private Bytes parseName(Bytes src, Pattern pattern) {
        Matcher matcher = pattern.matcher(src.toString());
        if (matcher.find()) {
            String id = matcher.group("id");
            if (id != null) {
                return new Bytes(id).detach();
            }
        }
        return null;
    }

    public List<Bytes> getHeader() {
        return header;
    }

    public static String name() {
        return VCFHeaderManager.class.getName();
    }

    public static void storeHeader(boolean storeHeader) {
        VCFHeaderManager.store = storeHeader;
    }

    public void clear() {
        header.clear();
        infoNameSet.clear();
        contigNameSet.clear();
        subjectNameSet.clear();
        formatNameSet.clear();
    }
}
