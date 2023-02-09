Statcoding
==========

Statcoding is a library to help statistically encode and decode data. I is refactored from field-proven code from the BRouter project.

Refactoring was aimed towards a clear API with 64-bit default width, less performance hacks and a more educational structure, minimal edge-cases and glitches.

The very heart of this library are the classes Bit(In/Out)putStream that are able to switch back and forth between the byte-aligned operations of the Data(In/Out)put interface, a byte-aligned variable-length code and bitwise operations.

Huffman encoding and arithmetic encoding is included together with a concept for Codecs with fixed statistics and 2-pass encoding. These are fitted for write-once/read many times problems like the blockwise read-only random access databases inside BRouter's RD5-Datafiles.

Arithmetic encoding was not included in BRouter but is heavily inspired by: https://github.com/nayuki/Reference-arithmetic-coding


Usage
-----

 - Compile the library:

   mvn install

Examples
--------

 - Compile the image-compression example:

   cd examples

   javac -cp ../target/statcoding-0.0.1-SNAPSHOT.jar;. -d . *.java

 - Run the image-compression example:

   java -cp ../target/statcoding-0.0.1-SNAPSHOT.jar;. EncodeImage test.png test.bin

   java -cp ../target/statcoding-0.0.1-SNAPSHOT.jar;. DecodeImage test.bin test2.png


Release History
---------------

**2022.11.27** Version 0.0.1
