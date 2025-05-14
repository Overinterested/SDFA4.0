package edu.sysu.pmglab.sdfa.sv.vcf.calling;

import java.util.LinkedHashMap;

/**
 * @author Wenjie Peng
 * @create 2024-08-28 20:48
 * @description
 */
enum CallingTypeLoader {
    INSTANCE;
    private final LinkedHashMap<String, ICallingType> parserSet = new LinkedHashMap<>();
    CallingTypeLoader() {
    }

    public static ICallingType get(String key) {
        if (!INSTANCE.parserSet.containsKey(key)) {
            throw new RuntimeException("The field type '" + key + "' was not initialized. Available field type: " + INSTANCE.parserSet.keySet());
        } else {
            return INSTANCE.parserSet.get(key);
        }
    }

    public static boolean contains(String key) {
        return INSTANCE.parserSet.containsKey(key);
    }

    public static synchronized void add(ICallingType type) {
        if (type == null) {
            throw new NullPointerException();
        } else if (INSTANCE.parserSet.containsKey(type.getName())) {
            throw new RuntimeException("The field type '" + type.getName() + "' has been initialized");
        } else {
            INSTANCE.parserSet.put(type.getName(), type);
        }
    }

    static {
        CallingType[] var0 = CallingType.values();
        for (CallingType type : var0) {
            add(type);
        }
    }
}
