package edu.sysu.pmglab.debug;

import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2025-05-05 15:39
 * @description
 */
public class ListTest {
    public static void main(String[] args) {
        List<Object> objects = new List<>(1);
        objects.fill(null, 1);
        objects.fastSet(0,1);
    }
}
