# 从其他文件格式构建SDF存档

目前SV主要的记录和存储格式为VCF文件，因此在 SDFA 中不提供直接由其他文件格式构建SDF存档的命令行工具。但是基于广泛的SV文件兼容性和后续开发的SV分析工具，SDFA 提供了一个 API 已构建 SDF 存档。

## SDFWriter 构建 SDF 存档

`SDFWriter`是一个专门用于通过构造`SDFWriterRecord`直接构建 SDF 的类，其本质就是`获得记录-设置记录-写入记录-重置记录-结束记录`的过程。所以用户需要思考如何将一个输入的文件格式转为一条`SDFWriterRecord`记录。

下面我们给出一个`SDFWriter`的使用实例：

> [!NOTE|label:Example 1]
>
> SDFWriterRecord的本质是一个SV记录，因此我们需要构建一个SV记录并手动写入文件中。
>
> ``` shell
> public static void main(String[] args) throws IOException, InterruptedException {
>         String[] names = new String[1000];
>         for (int i = 0; i < 1000; i++) {
>             names[i] = String.valueOf(i);
>         }
>         SDFWriter writer1 = SDFWriter.SDFWriterBuild.of(new File("/Users/wenjiepeng/Desktop/tmp/yg/1.sdf"))
>                 .addFormat("GT")
>                 .addFormat("AD")
>                 .addInfoKeys("PRECISE", "READS_SUPPORT")
>                 .addIndividuals(names)
>                 .build();
>         SDFWriter.SDFWriterRecord item = writer1.getTemplateSV();
>         item.setInfo("PRECISE", "true")
>                 .setInfo("READS_SUPPORT", "3")
>                 .setAlt(new Bytes("ACGAGGGCCCCAA"))
>                 .setChrName("chr1")
>                 .setType(SVTypeSign.getByName("DEL"))
>                 .setID(new Bytes("ID_0"))
>                 .setRef(new Bytes("ACGAGGGCCCCAA"))
>                 .setPos(1000)
>                 .setEnd(2000)
>                 .setLength(1000)
>                 .setQuality(new Bytes("."))
>                 .setFilter(new Bytes("PASS"));
>         for (int i = 0; i < 1000; i++) {
>             item.addInitFormatAttrs(i, "1/1;3,2");
>         }
>         writer1.write(item);
>         writer1.write(item.setPos(999));
>         writer1.write(item.setPos(1001));
>         writer1.close();
>     }
> ```
>

上述实例主要构建 SV 并写入 SDF 文件中，因此对于一个任意格式的输入文件，用户可以通过`line -> SV`的形式来进行文件转化。

## API文档

具体可以查看`SDFWriter`类已构建SDF存档



