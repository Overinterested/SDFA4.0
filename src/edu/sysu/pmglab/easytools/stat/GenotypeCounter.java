package edu.sysu.pmglab.easytools.stat;

import edu.sysu.pmglab.container.list.IntList;

public enum GenotypeCounter {
    INSTANCE;

    /**
     * This class is not instantiable.
     */
    private GenotypeCounter() {
    }

    public static int[] countByAltIndex(int[][] gtys,  int currentAltIndex, int subjectStartID, IntList subIDs) {
        int refAllele = 0;
        int altAllele = 0;
        int refHom = 0;
        int het = 0;
        int altHom = 0;

        for (int i = 0; i < gtys[0].length; i++) {
            if (gtys[0][i] == gtys[1][i] && gtys[0][i] == 0) {
                refHom++;
            } else if (gtys[0][i] == gtys[1][i] && gtys[0][i] == currentAltIndex) {
                altHom++;
            } else if (gtys[0][i] != gtys[1][i] && (gtys[0][i] == currentAltIndex || gtys[1][i] == currentAltIndex)) {
                //it requires the heterozygous must contain at least one currentAltIndex
                het++;
            }
            //not the alternative allele homozygouse will count twice
            if (gtys[0][i] == 0) {
                refAllele++;
            } else if (gtys[0][i] == currentAltIndex) {
                altAllele++;
                subIDs.add(i+subjectStartID);
            }
            if (gtys[1][i] == 0) {
                refAllele++;
            } else if (gtys[1][i] == currentAltIndex) {
                altAllele++;
                subIDs.add(i+subjectStartID);
            }
        }
        int[] nums = new int[]{refAllele, altAllele, refHom, het, altHom};
        return nums;
    }

    public static int[] countByAltIndex(int[] leftGtys, int[] rightGtys, int currentAltIndex) {
        int refAllele = 0;
        int altAllele = 0;
        int refHom = 0;
        int het = 0;
        int altHom = 0;

        for (int i = 0; i < leftGtys.length; i++) {
            if (leftGtys[i] == rightGtys[i] && leftGtys[i] == 0) {
                refHom++;
            } else if (leftGtys[i] == rightGtys[i] && leftGtys[i] == currentAltIndex) {
                altHom++;
            } else if (leftGtys[i] != rightGtys[i] && (leftGtys[i] == currentAltIndex || rightGtys[i] == currentAltIndex)) {
                //it requires the heterozygous must contain at least one currentAltIndex
                het++;
            }
            if (leftGtys[i] == 0) {
                refAllele++;
            } else if (leftGtys[i] == currentAltIndex) {
                altAllele++;
            }
            if (rightGtys[i] == 0) {
                refAllele++;
            } else if (rightGtys[i] == currentAltIndex) {
                altAllele++;
            }
        }
        int[] nums = new int[]{refAllele, altAllele, refHom, het, altHom};
        return nums;
    }
}
