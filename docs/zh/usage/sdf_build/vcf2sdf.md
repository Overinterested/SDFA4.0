# 从 VCF 构建 SDF 存档

SDFA为标准的二倍体物种提供了从 VCF (Variant Calling Format)文件到 SDF(Standardized Decomposition Format) 文件的转化，以实现 SV 数据的高效存储、SV 及其相关属性的快速访问、定位和VCF 文件的压缩等目的。

## 快速入门

在命令行中，使用如下指令为基因组 VCF 文件构建 SDF 存档：

``` sh
java -jar sdfa.jar vcf2sdf [options]
```
当然上述命令行支持把VCF的GZ、BGZ压缩文件转为SDF文件，我们提供几个简单的例子：
- 指定文件作为输入

> [!NOTE|label:Example 1]
>
> 使用示例文件`HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf`构建存档：
>
> ``` shell
> java -jar sdfa.jar vcf2sdf -f ./HG01258_HiFi_aligned_GRCh38_winnowmap.sniffles.vcf -o ./
> ```

- 指定文件夹作为输入

> [!NOTE|label:Example 2]
>
> 使用文件夹作为输入构建存档：
>
> ```shell
> java -jar sdfa.jar vcf2sdf -d ./data -o ./
> ```

- 指定输入数据的Calling类型

> [!NOTE|label:Example 3]
>
> 不同于SNP的VCF文件，不同Calling工具的SV VCF格式存在差异(主要体现在`INFO`字段)，并且这些差异会影响对SV 坐标位置的提取。因此我们实现了对13种主流的SV Calling工具的SV VCF提取:
>
> CuteSV2、CuteSV、Debreak、Delly、NanoSV、Nanovar、Pbsv、Picky、Sniffles2、Sniffle、Svim、Svision、Ukbb 
>
> 可以通过`--calling-type`或者`-ct`指定所选择的Calling工具类型。值得注意的是，默认情况下或者指定未知`ct`情况下会按照[标准SV VCF4.3](https://samtools.github.io/hts-specs/VCFv4.3.pdf)进行解析。
>
> ``` shell
> java -jar sdfa.jar vcf2sdf -ct cutesv -d ./data -o ./
> ```

## API工具

将 VCF 文件转换为 SDF 文件的 API 工具是 SDSVManager，使用示例如下：

``` java
// 对某个文件夹下的所有VCF及其GZ、BGZ压缩文件进行转换
int thread = 4;
SDSVManager.of("inputDir")
  .setOutput("outputDir")
  .setCallingType("cuteSV")
  .run(thread);
```

