Statcoding
==========

Statcoding is a library to help statistically encoding and decoding data,
refactored from field-proven code from the BRouter project.

Refactoring is aimed towards a clearer API with 64-bit default width,
less performance hacks and a more educational structure,
minimal edge-cases and glitches

There's also arithmetic encoding (not used in BRouter)

The arithemtic code is heavily inspired by:
https://github.com/nayuki/Reference-arithmetic-coding


Usage
-----

 - Compile the library:

   mvn install

Examples
--------

 - Compile the image-compression example:

   cd statcoding
   javac -cp ../target/statcoding-0.0.1-SNAPSHOT.jar;. -d . *.java

 - Run the image-compression example:

   java -cp ../target/statcoding-0.0.1-SNAPSHOT.jar;. EncodeImage test.png test.bin

   java -cp ../target/statcoding-0.0.1-SNAPSHOT.jar;. DecodeImage test.bin test2.png


Release History
---------------

**2022.11.27** Version 0.0.1
