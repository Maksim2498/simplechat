# Simple Chat Server CLI

## Table of Contents

- [Table of Contents](#table-of-contents)
- [About](#about);
- [Usage](#usage);
- [Options](#options).

## About

This document contains detailed description of the server command line interface (CLI).
[Usage section](#usage) contains basic syntax and semantic notes.
[Options section](#options) contains table describing all available CLI option.

## Usage

You can start server either with options or without.
To start server without options just execute something like this in your terminal:

```bash
java -jar /path/to/server-<version>.jar
```

To start server with options execute something like this in your terminal:

```bash
java -jar /path/to/server-<version>.jar option-0 option-1 ... option-n
```

Options starting with `-` represents one-letter (short) options.
Options starting with `--` represent multi-letter (long) options.

Short options can be grouped like this: `-hd` -- this the same as passing
two options like this: `-h -d`.

If long option requires an argument you can pass it separating with space or `=`.
It will look like this: `--port 1502` or `--port=1502`.

## Options

| Name                     | Arguments Type      | Default Value | Description                                                                                                                            |
|--------------------------|---------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `--help`, `-h`           | -                   | -             | Prints help message and quits                                                                                                          |
| `--version`, `-v`        | -                   | -             | Prints version and quits                                                                                                               |
| `--debug`, `-d`          | -                   | -             | Enables debug mode                                                                                                                     |
| `--buffering-duration`   | `duration`          | `0s`          | Specifies how long messages should be buffered before broadcasting them (if 0s then message buffering is disabled)                     |
| `--broadcast-messages`   | `boolean`           | `true`        | Enables or disables broadcasting of received messages to all clients                                                                   |
| `--print-messages`       | `boolean`           | `true`        | Enables or disables printing of received messages                                                                                      |
| `--port`, `-p`           | `port`              | `24982`       | Specifies server port                                                                                                                  |
| `--backlog`              | `uint`              | `50`          | Sepcifies limit of simultaneous connection attempts to the server                                                                      |
| `--max-pending-commands` | `uint`              | `50`          | Specifies how many commands may be sent to client without response before connection closure (if 0 then all responses will be ignored) |
| `--name`, `-n`           | `string`            | `"<Server>"`  | Specifies server name                                                                                                                  |
| `--ping-interval`        | `duration`          | `10s`         | Specifies delay between clients pinging (if 0 then pinging is disabled)                                                                |
| `--protocol`, `-P`       | `"text" | "binary"` | `"text"`      | Specifies server protocol type                                                                                                         |
| `--multicast`, `-m`      | -                   |               | Enables message multicasting (if disabled then server sends messages to the clients via TCP unicast, else it uses UDP mulicast)        |
| `--multicast-address`    | `address`           | `233.0.0.1`   | Specifies multicast address                                                                                                            |
| `--multicast-port`       | `port`              | `24982`       | Specifies multicast port                                                                                                               |
