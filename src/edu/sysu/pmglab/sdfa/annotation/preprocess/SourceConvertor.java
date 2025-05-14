package edu.sysu.pmglab.sdfa.annotation.preprocess;

import edu.sysu.pmglab.sdfa.annotation.source.Source;

import java.io.IOException;

/**
 * @author Wenjie Peng
 * @create 2024-09-07 20:05
 * @description
 */
public interface SourceConvertor {
    /**
     * convert annotation resource file to CCF annotation file
     * @return
     * @throws IOException
     */
    Source convert() throws IOException;
}
