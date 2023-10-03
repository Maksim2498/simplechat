# Simple Chat

## Table of Contents

- [Table of Contents](#table-of-contents);
- [About](#about);
- [Building](#building);
- [Dowloading](#downloading);
- [Running](#running);
- [Documentation](#documentation).

## About

This is a simple terminal chatting application.
Project is created in self-education purposes and has no real-world use potential.

It consists of:

- [`Core`](./core/README.md) - utility library;
- [`Client`](./client/README.md);
- [`Server`](./server/README.md).

For more info on each of it's parts see links above.

## Requirements

**For running:**

- [Java 17+](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html);

**For building:**

- [Scala 3.3.1+](https://www.scala-lang.org/download/3.3.1.html);
- [SBT 1.9.5+](https://www.scala-sbt.org/download.html).

## Building

First, set your current working directory to the one in which this `README.md` file is located.
Second, simply execute the following command in your terminal:

```bash
sbt assembly
```

After this, if no errors where emitted, you should have the following artifacts built:

- `client/target/scala-<scala version>/client-<client version>.jar` - client `jar` archive;
- `server/target/scala-<scala version>/server-<client version>.jar` - client `jar` archive;

Replace `<scala version>`, `<client verions>`, and `<server version>` with appropriate versions.

## Downloading

***WIP***

## Running

First, locate built or downloaded `jar` archives and simply run them with a command like the following:

```bash
java -jar /path/to/achive.jar
```

## Documentation

- [Protocol](./docs/protocol.md) (***WIP***).
- [Client CLI](./client/docs/cli.md);
- [Client shell](./client/docs/shell.md)
- [Server CLI](./server/docs/cli.md);
- [Server shell](./server/docs/shell.md);
- [Code of Conduct](./docs/CODE_OF_CONDUCT.md);
- [Security](./docs/SECURITY.md).
