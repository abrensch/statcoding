QR Code carrying a Digitial Certificate
=======================================

This is a no-frills version of a digital certificate. The example
imitates the "European Digital Covid Certificate" (EU DCC), but in plain
vanilla and without the dependency-hell.

But please think of any piece of structured information that is to be
packed into a QR code together with a digital signature (could be
event-tickets, medical documents, Proof of ability, ...)

To compare with the EU DCC reference implemenation see https://github.com/diggsweden/dgc-java
They use a "binary JSON Twin" to encode the payload, but they shorten the attribute names
to 2 characters to save space. This is an indication that it is a stupid idea to store
meta data in QR codes.

Our example uses a data structure that does not contain metadata, but still allows
schema evolution by prefixing version and size information before each data record.

The digital signature uses 256 Bit ECDSA (same for EU DCC), using about 70 bytes for the signature.

Please note that the encoded result does not contain a certificate chain (Would
be challenging to put a certificate chain into a QR code..)


Example Usage
-------------

 - Setup:

```
     Windows:
       cd examples\qr_certificate
       set CLASSPATH=..\..\target\statcoding-0.0.1-SNAPSHOT.jar;.

     Linux:
       cd examples/qr_certificate
       export CLASSPATH=../../target/statcoding-0.0.1-SNAPSHOT.jar:.
``` 

 - Compile:

   javac *.java
 

 - encode the dummy certificate:

   java EncodeDummyCertificate
   

 - decode a certificate and verify signature (place above output or a QR-Code-Scan as the argument):

   java DecodeCertificate HC7:...

Example code does not include QR-Code generation or parsing. Please use

- Libre-Office to create QR-Codes (select -> Einfuegen -> (OLE-)Objekt -> QR-Code)
- your Phone to scan QR-Codes
