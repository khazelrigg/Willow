[![Build Status](https://travis-ci.org/khazelrigg/wordCount.svg?branch=1.1.0)](https://travis-ci.org/khazelrigg/wordCount)

Word Count
==========

**Word Count** is a Java application built using Stanford's Core NLP library with the main purpose 
of analysing a file to generate human readable text files and charts that show  word counts and Parts Of Speech
 distribution in a text.

---

### Usage:

    usage: wordCount [OPTION]... [FILE]...
     -h,--help          Print help
     -i,--images        Create image outputs
     -j,--json          Create JSON output
     -k,--interactive   Run interactive mode, choose options when run instead
                        of in command line
     -o,--overwrite     Overwrite any results

### Installing
1. Get the [latest release of this program](https://github.com/khazelrigg/wordCount/releases)

2. Executing the jar:
     
        java -jar WordCount.jar -h
        
    Example usage to analyse directory with the name "Books" and create P.O.S. and difficulty charts
        
        java -jar WordCount.jar -i Books
        
4. Result outputs will be created as a folder in the working directory, use `cd results` to view the generated output

---
### Web demo
A small web demo that shows an application of this program can be found [here](http://bl.ocks.org/khazelrigg/raw/287b2e8a648bf85313de686bfe7ed540/)

### License:
This project is licensed under the Apache 2.0 license
