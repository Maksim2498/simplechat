# Simple Chat Protocol

## Table of Contents

- [Table of Contents](#table-of-contents);
- [About](#about);
- [Protocol](#protocol);
  - [Commands](#commands);
  - [Responsed](#responses);
  - [Text Implementation](#text-implementation);
  - [Binary Implementation](#binary-implementation).

## About

This document contains detailed description of the network protocol
used by this chat application.

It begins with a high-level description of the protocol architecture
(["Protocol" section](#protocol)) and then continues with a detailed
description of the two low-level implementations of it:
[binary](#binary-implementation) and [textual](#text-implementation).

## Protocol

Protocol works in bidirectional request-response manner.
Client or server can both send *commands* and receive *requests*.
Every sent command must be reponded with a request by default.

### Commands

Every command consists of *code*, *command identifier*, and *command arguments*.
Code is used to identify to which command the given request refers to.
There are two groups of commands: client and server commands.
Description of these command groups is given in the following tables.

**Client commands:**

| Command        | Arguments | Description                                                            |
|----------------|-----------|------------------------------------------------------------------------|
| `Ping`         | -         | Checks is server is still alive                                        |
| `Close`        | -         | Closes connection gracefully                                           |
| `Set Name`     | `name`    | Sends set-name request. Name is set if server responds with `OK`       |
| `Send Message` | `text`    | Sends send-message request. Message is ent if server responds wth `OK` |

**Server commands:**

| Command        | Arguments        | Description                     |
|----------------|------------------|---------------------------------|
| `Ping`         | -                | Checks if client is still alive |
| `Close`        | -                | Closes connection gracefully    |
| `Send Message` | `author`, `text` | Sends message                   |

### Responses

Every response consists of *code*, and *status*.
Code must be the same as the one from a command to which this request is refered to.
Status indicates whether command is executed successfully or not.

**Statuses:**

- `OK` - command is executed successfully;
- `ERROR` - command cannot be executed due to some circumstances;
- `FATAL` - executor cannot proceed its work, connection must be closed.

### Text Implementation

In then text implementation of the protocol both commands and responses are represented by
one line of space-separated words. The structure of both command and request are the following:

**Request:**

```text
'c' code command {arg}
```

**Response:**

```text
'r' code status
```

Values enclosed within quotes mean "exact as inside quotes".

`code` is just a number of command code which may be negative.
Code must be in range [-32,768 to 32,767] (16-bit sigend).
"command" is a command name. The command names are following:

| Command        | Name       |
|----------------|------------|
| `Ping`         | `ping`     |
| `Close`        | `close`    |
| `Set Name`     | `set_name` |
| `Send Message` | `send-msg` |

`{arg}` must be read as "zero or more space-separated arguments".
All arguments are of string type and must be enclosed with double quotees ('"').
Argument strings may contain escape sequences starting with `\`.
Supported escape sequences are following:

| Escape Sequence | Description     |
|-----------------|-----------------|
| `\b`            | Backspace       |
| `\f`            | From feed       |
| `\r`            | Carriage return |
| `\n`            | New line        |
| `\t`            | Tab             |
| `"`             | Double quote    |
| `\\`            | Backslash       |

`status` is a status name.
Status names are exact the same as listed in [Responses section](#responses) and are case-insensitive.

**Example:**

If client would like to change its name he must sent a message like following:

```text
c 2498 set_name "Maksim"
```

The server (if accepted) will repond in the following way:

```text
r 2498 OK
```

### Binary Implementation

In the binary implementation of the protocol command packets have the following structure:

```text
type code command-id arg-count {args}
```

Where:

- `type` - 8-bit value set to 1;
- `code` - 16-bit command code number;
- `command-id` - 8-bit value identifying a command type;
- `arg-count` - 8-bit value specifying a number of `args`;
- `args` - array or 8-bit unisgned value specifying a length of argument and the argument it self.

The list of command IDs is the following:

| Command        | ID  |
|----------------|-----|
| `Ping`         | `0` |
| `Close`        | `1` |
| `Send Message` | `2` |
| `Set Name`     | `3` |

Response structure, in turn, is the following:

```text
type code status-id
```

Where:

- `type` - 8-bit value set to 0;
- `code` - 16-bit command code number;
- `status-id` - 8-bit value idenitifying a response status.

Status IDs are the following:

| Status  | ID  |
|---------|-----|
| `OK`    | `0` |
| `ERROR` | `1` |
| `FATAL` | `2` |
