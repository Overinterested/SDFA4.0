package edu.sysu.pmglab.sdfa.sv.vcf.calling;


/**
 * @author Wenjie Peng
 * @create 2024-08-28 20:30
 * @description
 */
public interface ICallingType {
    String getName();

    static void add(ICallingType type) {
        CallingTypeLoader.add(type);
    }

    static ICallingType get(String name){return CallingTypeLoader.get(name);}

    AbstractCallingParser getParser();
}
