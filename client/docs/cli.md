# Simple Chat Client CLI

## Table of Contents

- [Table of Contents](#table-of-contents)
- [About](#about);
- [Usage](#usage);
- [Options](#options).

## About

This document contains detailed description of the client command line interface (CLI).
[Usage section](#usage) contains basic syntax and semantic notes.
[Options section](#options) contains table describing all available CLI option.

## Usage

You can start client either with options or without.
To start client without options just execute something like this in your terminal:

```bash
java -jar /path/to/server-<version>.jar
```

To start client with options execute something like this in your terminal:

```bash
java -jar /path/to/server-<version>.jar option-0 option-1 ... option-n
```

Options starting with `-` represents one-letter (short) options.
Options starting with `--` represent multi-letter (long) options.

Short options can be grouped like this: `-hd` -- this the same as passing
two options like this: `-h -d`.

If long option requires and argument you can pass it separating with space or `=`.
It will look like this: `--port=1502` or `--port 1502`.

## Options

| Name                     | Arguments Type      | Default Value | Description                                                                                                                            |
|--------------------------|---------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `--help`, `-h`           | -                   | -             | Prints help message and quits                                                                                                          |
| `--version`, `-v`        | -                   | -             | Prints version and quits                                                                                                               |
| `--debug`, `-d`          | -                   | -             | Enables debug mode                                                                                                                     |
| `--print-messages`       | `boolean`           | `true`        | Enables or disables printing of received messages                                                                                      |
| `--address`, `-a`        | `int`               | `24982`       | Specifies server address                                                                                                               |
| `--port`, `-p`           | `int`               | `24982`       | Specifies server port                                                                                                                  |
| `--max-pending-commands` | `int`               | `50`          | Specifies how many commands may be sent to server without response before connection closure (if 0 then all responses will be ignored) |
| `--name`, `-n`           | `string`            | -             | Specifies client name                                                                                                                  |
| `--ping-interval`        | `duration`          | `10s`         | Specifies delay between server pinging (if 0 then pinging is disabled)                                                                 |
| `--protocol`, `-P`       | `"text" | "binary"` | `"text"`      | Specifies client protocol type                                                                                                         |
