# 过滤模式

相比于单核苷酸多态性(single-nucleotide polymorphism，SNP)而言，SV的过滤筛选往往缺乏统一标准，同时筛选的属性也更加多元(可能包含INFO、QUAL和GT字段等)。

为进行更全面的SV过滤，SDFA内置了丰富的过滤功能。具体而言，主要覆盖两方面的过滤：

- `Genotype level`：设置质控信息对基因型过滤——不满足条件的设置为`./.`
- `SV level`：设置自定义函数对当前SV记录的`CHR`、`ID`等多个VCF字段进行过滤

具体的指令为

``` shell
java -jar sdfa.jar filter -d [input_dir] -o [output_dir] [options]
```

## Genotype 过滤

使用下述指令进行过滤：
``` shell
--filter-gty <format_attr> <function>
```

上述命令行对指定的`format_attr`进行过滤，而`function`是一个返回`boolean`值的表达式。

为方便用户直接对每一个基因型的质控属性的值进行操作，我们在function中直接使用value代表单个基因型的值，下面我们举一个例子：

> [!NOTE|label:Example 1]
>
> 我们对`./data`文件夹下的所有SDF文件的基因型进行过滤，过滤质控属性为`GQ`，过滤条件为`(int)value>20`，输出到`./`文件夹中：
>
> ``` shell
> java -jar sdfa.jar filter -d ./data -o ./ --filter-gty GQ (int)value>20
> ```
>
> 如果SDF文件为多样本文件，依次对每个样本的基因型进行上述条件过滤。

为简化转化逻辑同时减少`metric`数据的存储空间，我们将常见的质控属性封装特定类型以方便用户进行直接操作，具体如下：

| 抽象类型             | 存储类型     | 获取值类型                | 质控属性                           | 操作示例                                          | 操作解释                                   |
| -------------------- | :----------- | :------------------------ | :--------------------------------- | :------------------------------------------------ | :----------------------------------------- |
| SingleIntValueBox    | `int`数组    | 单个`int`值               | `DP`,`DR`,`DV`<br />`GQ`,`MD`,`PP` | `--filter-gty DP (int)values>10`                  | 将DP属性值<=10的基因型为`./.`              |
| TwoIntValueBox       | `int`数组    | 2个`int`值组成的IntList类 | `AD`,`RA`                          | `--filter-gty AD ((IntList)values).get(0)>10`     | 将AD属性值的第一个int值<=10的基因型为`./.` |
| ThreeIntValueBox     | `int`数组    | 3个`int`值组成的IntList类 | `PL`                               | `--filter-gty PL ((IntList)values).get(0)>10`     | 将PL属性值的第一个int值<=10的基因型为`./.` |
| SingleStringValueBox | `string`数组 | 单个`string`值            | `FT` <br />others                  | `--filter-gty FT (string)values.equals(\"TRUE\")` | 将FT属性不为TURE的基因型设置为`./.`        |

## SV 过滤

与基因型的指令类似，SV过滤的指令如下：

``` shell
--filter-sv <sv_attr> <function>
```

上述命令行对指定的`sv_attr`进行过滤，而`function`是一个返回`boolean`值的JAVA表达式，下面举个例子：

> [!NOTE|label:Example 2]
>
> 我们对INFO字段中的`IMPRECISE`进行过滤，当存在该字段时我们过滤该SV：
>
> ``` shell
> java -jar sdfa.jar -d ./data -o ./ --filter-sv IMPRECISE value!=null
> ```

上述例子中的`sv_attr`为IMPRECISE，在SDFA设计中`sv_attr`的定位和获取是由一定顺序的，下面是全面的过滤顺序：

| INDEX | 匹配字段         | 获取值类型       | 操作解释                               |
| ----- | ---------------- | ---------------- | -------------------------------------- |
| 1     | `CHR`            | `string`         | 获取SV的染色体名称                     |
| 2     | `ID`             | `Bytes`          | 获取SV的ID值                           |
| 3     | `REF`            | `Bytes`          | 获取SV的REF值                          |
| 4     | `ALT`            | `Bytes`          | 获取SV的ALT值                          |
| 5     | `QUAL`           | `Bytes`          | 获取SV的QUAL值                         |
| 6     | `FILTER`         | `Bytes`          | 获取SV的FILTER值                       |
| 7     | `INFO`           | `List<Bytes>`    | 获取SV的所有INFO字段值                 |
| 8     | `FORMAT`         | `List<Bytes>`    | 获取SV的所有基因型的质控值             |
| 9     | `GT`             | `CacheGenotypes` | 获取SV下所有样本的基因型               |
| 10    | `LEN`            | `int`            | 获取SV的LEN值                          |
| 11    | `SDF_FIELD_NAME` | `Object`         | 获取SV在`SDF_FIELD_NAME`的值           |
| 12    | `INFO_ATTR`      | `Bytes`          | 获取SV在INFO中KEY为`INFO_ATTR`的值     |
| 13    | `FORMAT_ATTR`    | `Bytes`          | 获取SV在FORMAT中KEY为`FORMAT_ATTR`的值 |
| 14    | UNKNOWN          | ERROR            | .                                      |

详细获取值可以查看`SDFRecordWrapper`的`getV`函数

> [!TIP|label:过滤顺序]
>
> 值得注意的是，在实际代码执行中会首先进行`SV Level`的过滤，然后再进行`Genotype Level`的过滤。