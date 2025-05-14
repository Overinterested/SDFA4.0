# 基于SV的GWAS分析流程

基于 SV 的 GWA 研究已经出现。由于 PLINK 集成了许多实用的统计测试方法，SDFA 为进一步挖掘易受影响的 SV 提供了基于 SV 的 GWA 工作流程。

以 UKBB 数据库为例，当样本量为海量时，考虑到数据量巨大，将总体 SV 数据拆分为多个 VCF 文件，每个文件存储整个 SV 数据的一部分。同时，由于 PLINK 已经集成了许多 GWA 相关的统计分析工具，SDFA 提供了 SDF2Plink 工具，将 SDF 文件转换为 Plink 输入文件，用于后续基于 SV 的 GWA 分析。

SDFA 已经建立了一个 VCF ⇒ SDF ⇒ Plink 的模式，它主要包含4个过程：

- `VCF2SDF`：将 VCF 转换为 SDF 文件，并执行 SV 过滤、信息提取等操作。
- `SDFConcat`：将多个 SDF 文件集成到一个 SDF 文件中。
- `SDFExtract`：从集成的 SDF 文件中提取部分样本信息。
- `SDF2Plink`：将提取的样本转换为 Plink 文件格式，即`.fam`、`.bed` 和`.bim` 文件。

> [!NOTE|lable:Example 1]
>
> 这里我们举一个例子，首先对`./test/resource/gwas`中的多个SDF文件进行集成，接着提取`./test/resource/gwas/sample.ped`中的样本，最后转化为PLINK文件。最后使用PLINK文件进行分析
>
> ``` shell
> java -jar ./SDFA.jar gwas \
> -dir ./test/resource/gwas \
> -o ./test/resource/gwas/output \
> --ped-file ./test/resource/gwas/sample.ped \
> --concat \
> -t 4
> 
> # plink
> ## 1. filter
> ./test/resource/gwas/plink2 --bfile ./test/resource/gwas/output/ \
> --geno 0.2 \
> --mind 0.8 \
> --hwe 1e-6 \
> --maf 0.05 \
> --make-bed \
> --out ./test/resource/gwas/output/geno_0.2_mind_0.8_maf_0.05
> ## 2. association
> ./test/resource/gwas/plink2 --bfile ./test/resource/gwas/output/geno_0.2_mind_0.8_maf_0.05 \
> -adjust \
> --glm \
> allow-no-covars \
> --out ./test/resource/gwas/output/geno_0.2_maf_0.05_res
> 
> ```