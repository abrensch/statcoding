QR Code carrying a Digitial Certificate
=======================================

This is a no-frills version of a digital certificate. The example
imitates the "European Digital Covid Certificate", but please think
of any piece of information that is to be packed into a QR code
together with a digital signature (could be event-tickets,
medical documents, Proof of ability, ...)


Example Usage
-------------

 - Setup:

```
     Windows:
       cd examples\lidar
       set CLASSPATH=..\..\target\statcoding-0.0.1-SNAPSHOT.jar;.

     Linux:
       cd examples/lidar
       export CLASSPATH=../../target/statcoding-0.0.1-SNAPSHOT.jar:.
``` 

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
