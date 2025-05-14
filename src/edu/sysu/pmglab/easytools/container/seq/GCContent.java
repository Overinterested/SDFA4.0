package edu.sysu.pmglab.easytools.container.seq;

/**
 * @author Wenjie Peng
 * @create 2023-04-12 20:16
 * @description
 */
public class GCContent {

    public static void main(String[] args) {
        test1();
    }

    public static void test() {
        long l = System.nanoTime();
        for (int k = 0; k < 1; k++) {
            int i = 0;
            int max = Integer.MAX_VALUE;
            int tmp = 0;
            int count = 0;
            while (i != max) {
                tmp = i;
                count = 0;
                for (int j = 0; j < 32; j++) {
                    count += tmp & 0x01;
                    tmp = tmp >>> 1;
                }
                i++;
            }
        }

        System.out.println(System.nanoTime() - l);
    }

    public static void test1() {
        long l = System.currentTimeMillis();
        for (int k = 0; k < 100000; k++) {
            int i = Integer.MIN_VALUE;
            int max = Integer.MAX_VALUE;
            int tmp = 0;
            int count = 0;
            while (i != max) {
                tmp = i;
                count = 0;
                tmp = tmp - ((tmp >>> 1) & 0x55555555);
                tmp = (tmp & 0x33333333) + ((tmp >> 2) & 0x33333333);
                tmp = (tmp + (tmp >>> 4)) & 0x0f0f0f0f;
                tmp = tmp + (tmp >>> 8);
                tmp = tmp + (tmp >>> 16);
                count = tmp & 0x3f;
                i++;
            }
        }

        System.out.println(System.currentTimeMillis() - l);
    }
}
