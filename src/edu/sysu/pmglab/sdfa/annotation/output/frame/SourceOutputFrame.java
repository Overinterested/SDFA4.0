package edu.sysu.pmglab.sdfa.annotation.output.frame;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2024-09-18 02:08
 * @description
 */
public interface SourceOutputFrame<Type> {

    List<Bytes> getOutputColumnNames();

    default void write(List<Type> records, ByteStream cache) {

    }
}
