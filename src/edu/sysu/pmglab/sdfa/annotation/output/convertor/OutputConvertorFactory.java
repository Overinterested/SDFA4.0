package edu.sysu.pmglab.sdfa.annotation.output.convertor;


import edu.sysu.pmglab.easytools.Constant;

import java.util.HashMap;

/**
 * @author Wenjie Peng
 * @create 2024-09-09 01:27
 * @description
 */
public class OutputConvertorFactory {
    private static final HashMap<String, OutputConvertor> otherFunctions = new HashMap<>();

    public static OutputConvertor getOutputConvertor(String type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case "min":
                return new MathOutputConvertor.MinOutputConvertor();
            case "max":
                return new MathOutputConvertor.MaxOutputConvertor();
            case "count":
                return new CountValueOutputConvertor();
            case "concat":
                return new ConcatOutputConvertor(Constant.COLON);
            case "middle":
                return new MathOutputConvertor.MiddleOutputConvertor();
            case "unique":
                return new UniqueValueCountOutputConvertor();
            case "average":
                return new MathOutputConvertor.AverageOutputConvertor();
            default:
                break;
        }
        if (otherFunctions.containsKey(type)) {
            return otherFunctions.get(type);
        }
        throw new UnsupportedOperationException("No convertor is called " + type+", and you can define it by implementing OutputConvert interface");
        // 可以在此扩展更多类型的转换
    }

    public static void add(String type, OutputConvertor function) {
        otherFunctions.put(type, function);
    }
}
