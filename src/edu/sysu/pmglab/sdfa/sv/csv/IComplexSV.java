package edu.sysu.pmglab.sdfa.sv.csv;

import edu.sysu.pmglab.sdfa.sv.SVCoordinate;

/**
 * @author Wenjie Peng
 * @create 2024-10-17 01:05
 * @description
 */
public interface IComplexSV extends Comparable<IComplexSV> {
    @Override
    int compareTo(IComplexSV o);

    int sizeOfCoordinates();

    SVCoordinate getCoordinate(int index);

}
