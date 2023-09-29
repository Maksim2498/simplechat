# Simple Chat Server

## Table of Contents

- [Table of Contents](#table-of-contents);
- [About](#about);
- [Requirements](#requirements);
- [Building](#building);
- [Downloading](#downloading);
- [Running](#running);
- [Documentation](#documentation).

## About

This is a server for [Simple Chat](../README.md).

## Requirements

**For running:**

- [Java 17+](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html);

**For building:**

- [Scala 3.3.1+](https://www.scala-lang.org/download/3.3.1.html);
- [SBT 1.9.5+](https://www.scala-sbt.org/download.html).

## Building

First, go to the parent folder of the one in which this `README.md`` file is located (root folder).
Then execute the following command in your terminal:

```bash
sbt server/assembly
```

After this, if no errors where emitted, you should have a built `jar` file in
`server/target/scala-<version>/` folder relative to the root of the project.

## Downloading

***WIP***

## Running

Locate downloaded or built `jar` file and simply execute the following command in your terminal:

```bash
java -jar /path/to/server-<version>.jar
```

## Documentation

- [CLI](./docs/cli.md);
- [Shell](./docs/shell.md);
- [Protocol](../docs/protocol.md).
