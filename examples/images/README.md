Image encoding example
======================

This is a simple example for encoding images using the statcoding library.

It's not of any particular use, just a demo of how to use the library and how competitive the compresion ratios are.


Usage
-----

 - Compile the image-compression example:

   cd examples/images

   javac -cp ../../target/statcoding-0.0.1-SNAPSHOT.jar;. -d . *.java

 - Run the image-compression example (with a test.png of your choice):

   java -cp ../../target/statcoding-0.0.1-SNAPSHOT.jar;. EncodeImage test.png test.bin

   java -cp ../../target/statcoding-0.0.1-SNAPSHOT.jar;. DecodeImage test.bin test2.png
