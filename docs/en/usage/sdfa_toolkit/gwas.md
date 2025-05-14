# SV-based GWAS

SV-based GWA studies have emerged. Since PLINK integrates many practical statistical testing methods, SDFA provides an SV-based GWA workflow for further exploring vulnerable SVs.

Taking the UKBB database as an example, when the sample size is massive, considering the huge amount of data, the overall SV data is split into multiple VCF files, with each file storing a part of the entire SV data. At the same time, since PLINK has integrated many GWA-related statistical analysis tools, SDFA provides the SDF2Plink tool to convert SDF files into Plink input files for subsequent SV-based GWA analysis.

SDFA has established a VCF ⇒ SDF ⇒ Plink pattern, which mainly consists of 4 processes:

- VCF2SDF: Convert VCF to SDF files and perform operations such as SV filtering and information extraction.
- SDFConcat: Integrate multiple SDF files into one SDF file.
- SDFExtract: Extract partial sample information from the integrated SDF file.
- SDF2Plink: Convert the extracted samples into Plink file format, namely `.fam`,`.bed` and `.bim` files.

> [!NOTE|label:Example 1]
>
> Here we take an example. First, integrate multiple SDF files in `./test/resource/gwas` folder. Then, extract the samples from `./test/resource/gwas/sample.ped` file. Finally, convert them into PLINK files. And use the PLINK files for analysis.
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