[![Build Status](https://travis-ci.org/khazelrigg/Willow.svg?branch=master)](https://travis-ci.org/khazelrigg/Willow)

Willow
==========

**Willow** is a Java application built using Stanford's Core NLP library with the main purpose 
of analysing a file to generate human readable text files and charts that show  word counts and Parts Of Speech
 distribution in a text.

---

### Usage:

    usage: Willow [OPTIONS] [FILE]
    Acceptable file types: Plain text and pdf
     -c,--csv             Create CSV output
     -e,--economy         Run in economy mode, greatly reduces memory usage at
                          the cost of completion speed. Useful for computers
                          with less memory
     -h,--help            Print help
     -i,--images          Create image outputs
     -j,--json            Create JSON output
     -o,--overwrite       Overwrite any existing results
     -t,--threads <arg>   Max number of threads to run, 0 = Use number of CPUs
                          available; default = 0
     -v,--verbose         Verbose output

### Installing
1. Get the [latest release of this program](https://github.com/khazelrigg/Willow/releases)

2. Executing the jar:
     
        java -jar Willow.jar -h
        
    Example usage to analyse directory with the name "Books" and create P.O.S. and difficulty charts
        
        java -jar Willow.jar -i Books
        
4. Result outputs will be created as a folder in the working directory, use `cd results` to view the generated output

---
### Web demo
A small web demo that shows an application of this program can be found [here](http://bl.ocks.org/khazelrigg/raw/287b2e8a648bf85313de686bfe7ed540/)

### License:
This project is licensed under the Apache 2.0 license
