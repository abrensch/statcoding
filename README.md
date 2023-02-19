Statcoding
==========

Statcoding is a library to help statistically encode and decode data. It is refactored from field-proven code from the BRouter project.

Refactoring was aimed towards a clear API with 64-bit default width, less performance hacks and a more educational structure, minimal edge-cases and glitches.

The very heart of this library are the classes Bit(In/Out)putStream. They implement the Data(In/Out)put interface and are able to switch back and forth between byte-aligned and bitwise operations.

Huffman encoding and arithmetic encoding is included together with a concept for Codecs with fixed statistics and 2-pass encoding. These are fitted for write-once/read-many-times problems like the blockwise read-only random access database inside BRouter's RD5-Datafiles.

Arithmetic encoding was not included in BRouter, just added here for completeness and is heavily inspired by: https://github.com/nayuki/Reference-arithmetic-coding


Usage
-----

 - Compile the library:

   mvn install

Examples
--------

 -  Example Lidar data processing:  [`examples/lidar/README.md`](examples/lidar/README.md).

 -  Example simple image encoding:  [`examples/images/README.md`](examples/images/README.md).


Release History
---------------

**2022.11.27** Version 0.0.1
