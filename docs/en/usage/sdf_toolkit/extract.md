# Sample Extraction

In large datasets, it is often necessary to extract fixed samples for downstream analysis. For example, the UKB's Whole genome GraphTyper SV data [interim 150k release] is a merged 150k dataset. When we need case - control samples for certain diseases, we need to perform extraction operations.

After the VCF file is converted to an SDF file, use the following command to extract the SDF:

``` shell
java -jar sdfa.jar extract [options]
```

Here we use the PED file as the input file to extract the required samples from the population SDF file. At the same time, SDFA provides some basic SV screening functions for the SV after sample extraction.

> [!NOTE|label:Example 1]
>
> We extract all SDF files in the input folder:
>
> ``` shell
> java -jar sdfa.jar extract -d ./data -ped ./ped.ped -o ./
> ```

## Program parameters

``` shell
Grammar：extract -d input_dir -o output_dir -ped ped_path
Java-API: edu.sysu.pmglab.sdfa.toolkit.SDFExtract
About: Extract samples from PED in multiple SDF files
Parameters：
	*--output, -o 		Set the output folder.
										Format：-o <dir>
	*--dir, -d				Set the input folder.
										Format：-d <dir>
	*--ped-file, -ped	Set the PED file.
										Format：ped <file>
	--thread, -t.			Set the thread numbers.
	--max-maf					Set the maximum proportion of genotypes in the extracted samples
	--min-maf					Set the minimum proportion of genotypes in the extracted samples
```

## API Document

The API tool for extracting SDF files is SDFExtract, and the usage examples are as follows:

``` java
SDFExtract.of(file.toString(),
              sdfExtractProgram.pedFile,
              FileUtils.getSubFile(sdfExtractProgram.outputSDFDir, file.getName())
              )
					.setMaxMAF(sdfExtractProgram.maxMaf)
					.setMinMAF(sdfExtractProgram.minMaf)
					.submit();
```

