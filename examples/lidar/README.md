Lidar data handling example
===========================

This example shows how the statcoding libraray can help handling big datasets
by encoding them into a working format which is compact and can be decoded very fast.

It handles digital elevation data from laser measurements which is available
for public download for most countries with 1m horizontal resolution and
30cm elevation accuracy.

The data tiles are transformed to a proprietary format which is
5 times smaller and decodes 20 times faster compared to the downloadable
formats. That allows much more efficient processing of such data.


Example Usage
-------------

 - Setup (Windows):

   cd examples\lidar
   set CLASSPATH=..\..\target\statcoding-0.0.1-SNAPSHOT.jar;.

 - Setup (Linux):

   cd examples/lidar
   export CLASSPATH=../../target/statcoding-0.0.1-SNAPSHOT.jar:.

 - Compile:

   javac *.java
 

 - get some 1m Lidar ground model data (DGM1) for Bad Homburg in the current directory:

   wget https://gds.hessen.de/downloadcenter/20230219/3D-Daten/Digitales%20Gel%C3%A4ndemodell%20(DGM1)/Hochtaunuskreis/Bad%20Homburg%20v.d.H%C3%B6he%20-%20DGM1.zip
   

 - recode it to a compact format:

   java RecodeLidarData


 - create a downscaled overview image:

   java CreateLidarImage dgm1 bad_homburg.png 464 5560 480 5570 4


 - zoom at "Kastell Saalburg" in full resolution:

   java CreateLidarImage dgm1 kastell_saalburg.png 468 5568 470 5570 1


  -> When viewing kastell_saalburg.png: Do you see the remains of the border wall "Roman Limes" passing north of the Kastell ?
