# Build SDF from VCF

SDFA provides the conversion from VCF (Variant Calling Format) files to SDF (Standardized Decomposition Format) files for standard diploid species, aiming to achieve efficient storage of SV data, rapid access and location of SV and its related attributes, and compression of VCF files.

## Quick Start

In the command line, use the following command to build an SDF archive for the genomic VCF file:

``` sh
java -jar sdfa.jar vcf2sdf [options]
```

Of course, the above command line supports converting GZ and BGZ compressed VCF files into SDF files. Here are a few simple examples:

- Build SDF from single VCF file

> [!NOTE|label:Example 1]
>
> Build an archive using the example file` HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf`:
>
> ``` shell
> java -jar sdfa.jar vcf2sdf -f ./HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf -o ./
> ```

- Build SDF from the fold

> [!NOTE|label:Example 2]
>
> Build an archive using a folder as input:
>
> ```shell
> java -jar sdfa.jar vcf2sdf -d ./data -o ./
> ```

- Build SDF by specifying the Calling Type of VCF files

> [!NOTE|label:Example 3]
>
> Unlike the VCF files of SNPs, the SV VCF formats of different calling tools vary (mainly reflected in the `INFO` field), and these differences can affect the extraction of SV coordinate positions. Therefore, we have implemented the extraction of SV VCFs from 13 mainstream SV calling tools:
>
> CuteSV2、CuteSV、Debreak、Delly、NanoSV、Nanovar、Pbsv、Picky、Sniffles2、Sniffle、Svim、Svision、Ukbb 
>
> In SDFA, the type of the selected Calling tool can be specified through `--calling-type` or` -ct`. It is worth noting that, by default or when an unknown `ct` is specified, the parsing will be carried out according to the [standard SV VCF4.3](https://samtools.github.io/hts-specs/VCFv4.3.pdf).
>
> ``` shell
>java -jar sdfa.jar vcf2sdf -ct cutesv -d ./data -o ./
> ```

## API Document

The API tool for converting VCF files to SDF files is SDSVManager. The usage examples are as follows:

``` java
// Convert all VCF files and their GZ and BGZ compressed files in a certain folder
int thread = 4;
SDSVManager.of("inputDir")
  .setOutput("outputDir")
  .setCallingType("cuteSV")
  .run(thread);
```

