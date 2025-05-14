# 样本提取

在大型数据集中往往需要提取固定的样本进行下游分析，例如UKB的 Whole genome GraphTyper SV data [interim 150k release] 是已经被合并好的150k 合并的数据集，此时我们需要某些疾病的case-control样本时就需要进行提取操作。

当该VCF文件被转为SDF文件后，使用下述指令对SDF进行提取：

``` shell
java -jar sdfa.jar extract [options]
```

这里我们使用PED文件作为输入文件以从群体SDF文件中提取所需的样本，同时SDFA为提取样本后的SV提供了一些基本的SV筛选函数。

> [!NOTE|label:Example 1]
>
> 我们对输入文件夹的所有SDF文件进行提取：
>
> ``` shell
> java -jar sdfa.jar extract -d ./data -ped ./ped.ped -o ./
> ```

## 程序参数

``` shell
语法：extract -d input_dir -o output_dir -ped ped_path
Java-API: edu.sysu.pmglab.sdfa.toolkit.SDFExtract
关于：提取多个 SDF 文件中的PED中的样本
参数：
	*--output, -o 		设置输出文件夹.
										格式：-o <dir>
	*--dir, -d				设置输入文件夹.
										格式：-d <dir>
	*--ped-file, -ped	设置PED文件
										格式：ped <file>
	--thread, -t.			设置线程数
	--max-maf					设置提取样本中含有基因型的最大比例
	--min-maf					设置提取样本中含有基因型的最小比例
```

## API工具

对 SDF 文件进行提取的 API 工具是SDFExtract，使用示例如下：

``` java
SDFExtract.of(file.toString(),
              sdfExtractProgram.pedFile,
              FileUtils.getSubFile(sdfExtractProgram.outputSDFDir, file.getName())
              )
					.setMaxMAF(sdfExtractProgram.maxMaf)
					.setMinMAF(sdfExtractProgram.minMaf)
					.submit();
```

