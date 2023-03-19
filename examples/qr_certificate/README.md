QR Code carrying a Digitial Certificate
=======================================

This is a no-frills version of a digital certificate. The demo imitates the "European Digital Covid Certificate" (EU-DCC). Following picture shows on the left side an official EU-DCC QR-Code. On the right side is the exact same payload and digital signature, encoded with the help of the statcoding library to about 2/3 the size of the EU-DCC:

<img src="https://raw.githubusercontent.com/abrensch/statcoding/main/examples/qr_certificate/example_qr_codes.png" width="100%"/>

For a wonderful description of the structure and contents of the EU-DCC see <https://www.bartwolff.com/Blog/2021/08/08/decoding-the-eu-digital-covid-certificate-qr-code>. They use a "binary JSON Twin" to encode the payload, but shorten the attribute names to 2 characters to save space. This is an indication that storing attribute names in a QR code is not the best idea.

Our demo uses a data structure that does not contain any meta information, but still allows schema evolution by prefixing version and size information before each data record.

You see that our QR code is about 250 bytes in size, whereof 72 bytes account for the signature. Maximum QR code capacity is 2.956 Byte, so there's plenty of space to store all kinds of digitally signed documents in QR codes, opening up many possible applications.


Example Usage
-------------

 - Setup:

```
     Windows:
       cd examples\qr_certificate
       set CLASSPATH=..\..\target\statcoding-0.9.0-SNAPSHOT.jar;.

     Linux:
       cd examples/qr_certificate
       export CLASSPATH=../../target/statcoding-0.9.0-SNAPSHOT.jar:.
``` 

 - Compile:

   javac *.java
 

 - encode the dummy certificate:

   java EncodeDummyCertificate
   

 - decode a certificate and verify signature (place above output or a QR-Code-Scan as the argument):

   java DecodeCertificate HC7:...

Our demo does not include QR-Code generation or parsing. Please use

- Libre-Office-Writer to create QR-Codes (select -> Einfuegen -> (OLE-)Objekt -> QR-Code)
- your Phone to scan QR-Codes
