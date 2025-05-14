# README

This repository provides functions to convert three types of reference genomes (refGene, GENCODE, and KnownGene) into a format compatible with KGGSeq.

**Reference Genomes**

1.	**refGene** and **GENCODE**: These formats require two input files:
      - **GTF File**: Contains records of all genes and transcripts.
    - Example: GRCh38_latest_genomic.gtf.gz, available from [RefGene](https://ftp.ncbi.nih.gov/refseq/H_sapiens/annotation/GRCh38_latest/refseq_identifiers/)
      - **FNA File**: Contains genomic sequences.
    - Example: GRCh38_latest_genomic.fna.gz, available from [RefGene](https://ftp.ncbi.nih.gov/refseq/H_sapiens/annotation/GRCh38_latest/refseq_identifiers/)

Alternatively, GTF files can be obtained from [GENCODE](https://www.gencodegenes.org/human/).

2. **KnownGene**: Unlike refGene and GENCODE, KnownGene data does not include a GTF file. Instead, the knownGene.txt file is used as the input. However, since this file does not contain gene names for each transcript, additional databases (such as GENCODE) are required to map and complete the gene names.
   In this repository, we use the gene mapping data from Ensembl’s [BioMart](https://asia.ensembl.org/info/data/biomart/index.html).

