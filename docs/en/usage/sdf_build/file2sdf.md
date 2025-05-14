# Build SDF From other file type

Currently, the main recording and storage format of SV is the VCF file. Therefore, in SDFA, there is no command - line tool to directly build an SDF archive from other file formats. However, based on the extensive compatibility of SV files and the SV analysis tools developed subsequently, SDFA provides an API to build an SDF archive.

## SDFWriter build SDF Archives

The `SDFWriter` is a class specifically designed to directly build an SDF by constructing `SDFWriterRecord`. In essence, it is a process of `obtaining a record`, `setting the record`, `writing the record`, `resetting the record` and `ending the record`. Therefore, users need to consider how to convert an input file format into an `SDFWriterRecord` record.

Now, let's give an example of the use of `SDFWriter`:

> [!NOTE|label:Example 1]
>
> The essence of `SDFWriterRecord` is an SV record. Therefore, we need to construct an SV record and manually write it into a file.
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

The above example is to construct an SV and write it. Therefore, for an input file in any format, what the user needs to construct is the process of converting Line → Record.

## API Document

Specifically, users can check the SDF archive built by the `SDFWriter` class.

