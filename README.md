# avaje-simple-logger

A SLF4J Logger built primarily for use with GraalVM Native image that writes JSON structured logs to `System.out`. 
It is designed to be used by applications that will be run in K8s or Lambda. 


## Background

This logger has been created from the Logback JSON Encoder from [avaje-logback-encoder](https://github.com/avaje/avaje-logback-encoder), 
and turned into a SLF4J Logger, thus removing the dependency on Logback and its associated XML configuration.


## How to use it

#### Step 1: Add the dependency

```xml
  <dependency>
    <groupId>io.avaje</groupId>
    <artifactId>avaje-simple-logger</artifactId>
    <version>0.1</version>
  </dependency>
```

#### Step 2: `src/main/resources/avaje-logger.properties`

Configure via adding a `src/main/resources/avaje-logger.properties`
```properties
## for testing we desire plain format than json format
logger.writer=plain

## specify the default log level to use for testing
logger.defaultLogLevel=warn

## specify some default log levels
log.level.com.foo.bar=DEBUG
log.level.io.avaje=INFO
```

#### Step 3: `src/test/resources/avaje-logger-test.properties`

For testing, we might desire to log in a plain format rather than JSON format. 
We also might want to define some test specific log levels.

Add test specific configuration via `src/test/resources/avaje-logger-test.properties`

```properties
## default log level to use when running tests
logger.defaultLogLevel=INFO

## some test specific log levels
log.level.io.ebean.test.containers=TRACE
log.level.io.ebean.DDL=TRACE
#log.level.io.ebean.SQL=DEBUG
#log.level.io.ebean.TXN=DEBUG
```

## Configure

### avaje-logger.properties & avaje-logger-test.properties

Configure the logger via main resource avaje-logger.properties 
and test resource avaje-logger-test.properties


```properties
## specify the default log level to use
logger.defaultLogLevel=warn

## specify to log as `json` or `plain` format (defaults to json)
logger.writer=json
logger.writer=plain

## specify if the logger name is abbreviated. Values:
## - full - use the full logger name
## - short - use the class name / suffix part of the logger name after the last `.`
## - (some integer e.g. 100) - abbreviate the logger name to the target length (shorten the package names)

logger.nameTargetLength=full
logger.nameTargetLength=short
logger.nameTargetLength=100

logger.nameTargetLength=100

## specify an explicit timezone to use, defaults to using default timezone
logger.timezone=UTC

## specify an explicit timestamp format to use, defaults to ISO_OFFSET_DATE_TIME
## valid values: ISO_OFFSET_DATE_TIME, ISO_ZONED_DATE_TIME, ISO_LOCAL_DATE_TIME, ISO_DATE_TIME, ISO_INSTANT
logger.timestampPattern=ISO_OFFSET_DATE_TIME

```


## Dynamic log level configuration

avaje-simple-logger automatically registers with avaje-config such that any configuration changes that
start with `log.level.` are logging level configuration changes, and these are applied.

avaje-config supports plugins like [AWS AppConfig](https://avaje.io/config/#aws-appconfig), where 
configuration changes can be dynamically made to the application. For example, `log.level.` changes
can be dynamically made this way.


## Don't need dynamic configuration?

If an application does not need dynamic configuration, then we can just use avaje-simple-json-logger.
This excludes the avaje-config dependency.

```xml
  <dependency>
    <groupId>io.avaje</groupId>
    <artifactId>avaje-simple-json-logger</artifactId>
    <version>0.1</version>
  </dependency>
```

Note that log levels can still be modified programmatically via:

```java
Map<String, String> nameLevels = new HashMap<>();
nameLevels.put("com.foo", "debug");
nameLevels.put("com.foo.bar", "info");
...
LoggerContext.get().putAll(nameLevels);
```
 
