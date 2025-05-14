package edu.sysu.pmglab.sdfa.nagf.analyze;

/**
 * @author Wenjie Peng
 * @create 2025-04-14 16:55
 * @description
 */
public class A {
    public static void main(String[] args) {
        int a=1, b=1, c=0;
        judge(a);
        judge(b);
        judge(c);
    }
    public static void judge(int a){
        System.out.println(a == 0?"=0":"≠0");
    }
}
