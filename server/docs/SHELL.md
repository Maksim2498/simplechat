# Simple Chat Server Shell

## Table of Contents

- [Table of Contents](#table-of-contents);
- [About](#about);
- [Usage](#usage);
- [Commands](#commands).

## About

This document contains detailed description of the server shell.
[Usage section](#usage) explains how to send messages, execute commands and more.
[Commands section](#commands) contains table describing all the commands available.

## Usage

To send a message behalf a server just type it in and then press enter (message must
not begin with `/`). To execute a command first type `/` and then a command name.
Command names are case insensitive.

## Commands

| Name       | Arguments       | Description            |
|------------|-----------------|------------------------|
| `stop`     | -               | Stops shell and server |
| `help`     | -               | Prints help message    |
| `kick`     | `<name or #id>` | Kicks specified user   |
| `kick-all` | -               | Kicks all the users    |
| `list`     | -               | Prints user list       |
