Statcoding
==========

A library to help statistically encode and decode data. It is refactored from field-proven code from the BRouter project.

Refactoring is aimed towards a clear API with 64-bit default width, less performance hacks, a more educational structure, full coverage of edge cases and as few traps and glitches as possible.

The very heart of this library are the classes Bit(In/Out)putStream. They implement the Data(In/Out)put interface and are able to switch back and forth between byte-aligned and bitwise operations.

Huffman encoding and arithmetic encoding is included together with a concept for Codecs with fixed statistics and 2-pass encoding. These are fitted for write-once/read-many-times problems like the blockwise read-only random access database inside BRouter's RD5-Datafiles.

Arithmetic encoding was not included in BRouter, just added here for completeness, and is heavily inspired by: https://github.com/nayuki/Reference-arithmetic-coding


Usage
-----

 - Java 8 or higher

 - Compile the library using Maven:

   mvn install

   Recommended use is to just copy the java-sources + unit-tests you are actually using into your project.
   While in beta, I will not upload the statcoding library to maven-central, and reserve the right to change
   the binary format created by the library in an incompatible way.


Examples
--------

 -  Example Lidar data processing:  [`examples/lidar/`](examples/lidar/)

 -  Example QR code certificate:  [`examples/qr_certificate/`](examples/qr_certificate/)

 -  Example simple image encoding:  [`examples/images/`](examples/images/)


Release History
---------------

**2022.11.27** Version 0.0.1

**2023.03.19** Version 0.9.0
