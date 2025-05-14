# Filter Mode

Compared with single - nucleotide polymorphism (SNP), the filtering and screening of SV often lack a unified standard, and the screening attributes are more diverse (possibly including INFO, QUAL, GT fields, etc.).

To perform more comprehensive SV filtering, SDFA has a rich set of built - in filtering functions. Specifically, it mainly covers two aspects of filtering:

- `Genotype level`：Set quality control information for genotype filtering - set those that do not meet the conditions as`./.`
- `SV level`：Set a custom function to filter multiple VCF fields such as `CHR` and `ID` of the current SV record

The specific instructions are

``` shell
java -jar sdfa.jar filter -d [input_dir] -o [output_dir] [options]
```

## Genotype Filter

Use the following instructions for filtering:
``` shell
--filter-gty <format_attr> <function>
```

The above command line filters the specified `format_attr`, and `function` is an expression that returns a `boolean` value.

To facilitate users to directly operate on the values of the quality control attributes of each genotype, we directly use `value` in the function to represent the value of a single genotype. Now, let's take an example:

> [!NOTE|label:Example 1]
>
> We filter the genotypes of all SDF files in the `./data` folder. The filtering quality control attribute is `GQ`, and the filtering condition is `(int)value>20`. The output is saved to the `./folder`:
>
> ``` shell
> java -jar sdfa.jar filter -d ./data -o ./ --filter-gty GQ (int)value>20
> ```
>
> If the SDF file is a multi-sample file, perform the above-mentioned conditional filtering on the genotypes of each sample in sequence.

To simplify the conversion logic and reduce the storage space of `metric` field data, we encapsulate common quality control attributes into specific types to facilitate users to operate directly. The details are as follows:

| Storage Class        | Storage Type  | Value Type                                  | Quality control attributes         | Example                                           | Example Explanation                                          |
| -------------------- | :------------ | :------------------------------------------ | :--------------------------------- | :------------------------------------------------ | :----------------------------------------------------------- |
| SingleIntValueBox    | `int` List    | Single `int` value                          | `DP`,`DR`,`DV`<br />`GQ`,`MD`,`PP` | `--filter-gty DP (int)values>10`                  | Set the genotype to./ when the DP attribute value is <= 10.  |
| TwoIntValueBox       | `int` List    | IntList instance composed of 2 `int` values | `AD`,`RA`                          | `--filter-gty AD ((IntList)values).get(0)>10`     | Set the genotype to./ when the first int value of the AD attribute value is <= 10. |
| ThreeIntValueBox     | `int` List    | IntList instance composed of 3 `int` values | `PL`                               | `--filter-gty PL ((IntList)values).get(0)>10`     | Set the genotype to./ when the first int value of the PL attribute value is <= 10. |
| SingleStringValueBox | `string` List | Single `string` value                       | `FT` <br />others                  | `--filter-gty FT (string)values.equals(\"TRUE\")` | Set the genotype with the FT attribute not being TR          |

## SV Filter

Similar to the instructions for genotypes, the instructions for SV filtering are as follows:

``` shell
--filter-sv <sv_attr> <function>
```

The above command line filters the specified `sv_attr`, and `function` is a JAVA expression that returns a `boolean` value. Now, let's take an example:

> [!NOTE|label:Example 2]
>
> We filter for `IMPRECISE` in the INFO field. When this field exists, we filter the SV:
>
> ``` shell
> java -jar sdfa.jar -d ./data -o ./ --filter-sv IMPRECISE value!=null
> ```

In the above example, the `sv_attr` is IMPRECISE. In the SDFA design, there is a certain order for the positioning and acquisition of `sv_attr`. The following is the comprehensive filtering order:

| INDEX | Match Field      | Value Type       | Value Description                                            |
| ----- | ---------------- | ---------------- | ------------------------------------------------------------ |
| 1     | `CHR`            | `string`         | Obtain the chromosome name of SV                             |
| 2     | `ID`             | `Bytes`          | Obtain the ID value of SV                                    |
| 3     | `REF`            | `Bytes`          | Obtain the REF value of SV                                   |
| 4     | `ALT`            | `Bytes`          | Obtain the ALT value of SV                                   |
| 5     | `QUAL`           | `Bytes`          | Obtain the QUAL value of SV                                  |
| 6     | `FILTER`         | `Bytes`          | Obtain the FILTER value of SV                                |
| 7     | `INFO`           | `List<Bytes>`    | Obtain all INFO field values of SV                           |
| 8     | `FORMAT`         | `List<Bytes>`    | Obtain the quality control values of all genotypes of SV     |
| 9     | `GT`             | `CacheGenotypes` | Obtain the genotypes of all samples under SV                 |
| 10    | `LEN`            | `int`            | Obtain the LEN value of SV                                   |
| 11    | `SDF_FIELD_NAME` | `Object`         | Obtain the value of SV in `SDF_FIELD_NAME` <br />(for details, please refer to the SDF structure) |
| 12    | `INFO_ATTR`      | `Bytes`          | Obtain the value of SV in `INFO` where the KEY is `INFO_ATTR` |
| 13    | `FORMAT_ATTR`    | `Bytes`          | Obtain the value of SV in `FORMAT` where the KEY is `FORMAT_ATTR` |
| 14    | UNKNOWN          | ERROR            | .                                                            |

For detailed value retrieval, you can refer to the `getV` function of `SDFRecordWrapper`.

> [!TIP|label:Filter Order]
>
> It is worth noting that in the actual code execution, the SV Level filtering will be carried out first, and then the Genotype Level filtering.