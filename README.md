# protovalidation

[![CI](https://github.com/lyang-highnote/protovalidation/actions/workflows/ci.yml/badge.svg)](https://github.com/lyang-highnote/protovalidation/actions/workflows/ci.yml)

gRPC Java project with [protovalidate](https://github.com/bufbuild/protovalidate) integration for request validation.

## Features

- Protobuf validation using protovalidate
- gRPC server interceptor for automatic request validation
- Buf for proto generation

## Build

```bash
mvn clean compile
```

## Test

```bash
mvn test
```

## Format

```bash
mvn spotless:apply
```
