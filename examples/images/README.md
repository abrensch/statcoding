Image encoding example
======================

This is a simple example for encoding images using the statcoding library.

It's not of any particular use, just a demo of how to use the library and how competitive the compresion ratios are.


Usage
-----

 - Setup (Windows):

   cd examples\images
   set CLASSPATH=..\..\target\statcoding-0.0.1-SNAPSHOT.jar;.

 - Setup (Linux):

   cd examples/images
   export CLASSPATH=../../target/statcoding-0.0.1-SNAPSHOT.jar:.

 - Compile:

   javac *.java

 - Run the image-compression example (with a test.png of your choice):

   java EncodeImage test.png test.bin

   java DecodeImage test.bin test2.png
