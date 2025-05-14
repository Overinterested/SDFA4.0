package edu.sysu.pmglab.easytools.constant;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.gtb.genome.genotype.Genotype;
import edu.sysu.pmglab.gtb.genome.genotype.container.SparseGenotypes;
import edu.sysu.pmglab.gtb.genome.genotype.encoder.Encoder;

/**
 * @author Wenjie Peng
 * @create 2024-12-21 01:44
 * @description
 */
public class GenotypeConstant {
    public static final byte[] GT = new byte[]{Constant.G, Constant.T};
    /**
     * MISSING_GTY means ./.
     */
    public static final Genotype MISSING_GTY = Genotype.of(-1, -1);
    /**
     * EMPTY_GENOTYPES means EMPTY
     */
    public static final Genotype Wild_TYPE_Homozygous = Genotype.of(0, 0);

    public static final Genotype Heterozygous_0_1 = Genotype.of(0, 1);
    public static final Genotype Heterozygous_1_0 = Genotype.of(1, 0);

    public static final Genotype Missing_Heterozygous_Variant = Genotype.of(-1, 1);

    public static final Genotype Mutant_Homozygous = Genotype.of(1, 1);

    public static final Genotype Missing_Wild_TYPE_1 = Genotype.of(-1, 0);
    public static final Genotype Missing_Wild_TYPE_2 = Genotype.of(0, -1);

    // EMPTY GTY
    public static final SparseGenotypes EMPTY_ENUMERATED_GENOTYPES = new SparseGenotypes(
            1,Missing_Heterozygous_Variant
    ).set(0, Missing_Heterozygous_Variant);

    public static final Bytes EMPTY_GTY_ENCODE = new Encoder().encode(EMPTY_ENUMERATED_GENOTYPES);
    // EMPTY GTY METRICS
    public static final List<Bytes> EMPTY_GTY_METRIC = List.wrap(new Bytes[]{Constant.EMPTY});
}
