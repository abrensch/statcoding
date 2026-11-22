Statcoding
==========

A library to help statistically encode and decode data. It is refactored from field-proven code from the BRouter project.

Refactoring is aimed towards a clear API with 64-bit default width, less performance hacks, a more educational structure, full coverage of edge cases and as few traps and glitches as possible.

The very heart of this library are the classes Bit(In/Out)putStream. They implement the Data(In/Out)put interface and are able to switch back and forth between byte-aligned and bitwise operations. The bitwise operations use the "MSB first" bit ordering.

Huffman encoding and arithmetic encoding is included together with a concept for Codecs with fixed statistics and 2-pass encoding. These are fitted for write-once/read-many-times problems like the blockwise read-only random access database inside BRouter's RD5-Datafiles.

Arithmetic encoding was not included in BRouter, just added here for completeness, and is heavily inspired by: https://github.com/nayuki/Reference-arithmetic-coding


Usage
-----

 - Java 8 or higher

 - Compile the library using Maven:

   mvn install

 - Use it as a Maven dependency:

   ```xml
   <dependency>
     <groupId>io.github.abrensch</groupId>
     <artifactId>statcoding</artifactId>
     <version>0.9.0</version>
   </dependency>
   ```

Releasing to Maven Central
--------------------------

The project is configured for the Sonatype Central Publisher Portal. Before the first release, verify the
`io.github.abrensch` namespace in the Portal and create a user token. Add the token to your Maven
`settings.xml` (do not commit it):

```xml
<servers>
  <server>
    <id>central</id>
    <username>YOUR_TOKEN_USERNAME</username>
    <password>YOUR_TOKEN_PASSWORD</password>
  </server>
</servers>
```

Configure a GPG key locally, then set the project version in `pom.xml` to a release version without
`-SNAPSHOT`. Run:

```sh
mvn -Pcentral-release clean deploy
```

The release profile creates source and Javadoc JARs, signs the artifacts, and uploads a deployment for
validation. Publishing is intentionally not automatic: review and publish the validated deployment in the
[Central Publisher Portal](https://central.sonatype.com/publishing/deployments).

Examples
--------

 -  Example Lidar data processing:  [`examples/lidar/`](examples/lidar/)

 -  Example QR code certificate:  [`examples/qr_certificate/`](examples/qr_certificate/)

 -  Example simple image encoding:  [`examples/images/`](examples/images/)


Release History
---------------

**2022.11.27** Version 0.0.1

**2023.03.19** Version 0.9.0
