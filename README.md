# avaje-simple-logger

A SLF4J Logger built primarily for use with GraalVM Native image that writes JSON structured logs (or plain format)
to `System.out`. It is designed to be used by applications that will be run in **K8s** or **Lambda**.

It supports GraalVM Native image, and also supports dynamic log levels. It is built with simple configuration
in mind based on properties (no reflection).

It is not intended to be a full featured logging framework with appenders, filters, etc (sticking to `System.out`).


## How to use it

#### Step 1: Add the dependency

```xml
  <dependency>
    <groupId>io.avaje</groupId>
    <artifactId>avaje-simple-logger</artifactId>
    <version>1.3</version>
  </dependency>
```

#### Step 2: `src/main/resources/avaje-logger.properties`

Configure via adding a `src/main/resources/avaje-logger.properties`
```properties
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
## for testing we desire plain format than json format
logger.format=plain

## default log level to use when running tests
logger.defaultLogLevel=INFO

## some test specific log levels
log.level.io.ebean.test.containers=TRACE
log.level.io.ebean.DDL=TRACE
#log.level.io.ebean.SQL=DEBUG
#log.level.io.ebean.TXN=DEBUG
```

## External configuration

We can override the configuration via setting a system property `logger.config`.

```sh
java -Dlogger.config=/path/to/my-logger.properties -jar myapp.jar
```

Note that `~` can be used to represent the user home directory like:
```sh
java -Dlogger.config=~/my-logger.properties -jar myapp.jar
```

When we specify an external configuration file via `logger.config`, then it is merged
with the default resource `avaje-logger.properties` configuration, with the external
configuration overriding the default configuration.


## GraalVM Native Image

Note that GraalVM native image wants to initialize as much as possible at build time,
and specifically the `System.Logger` is initialized at build time. We want avaje-simple-logger to
be the `System.Logger` implementation, so avaje-simple-logger classes are also initialized at build time.

In the native-image build in `user-specific feature(s):` section we will see
`io.avaje.simplelogger.graalvm.BuildInitialization` indicating that is happening.


## Debugging

To debug avaje-simple-logger set the log level for `io.avaje.simplelogger` to `DEBUG`

```properties
log.level.io.avaje.simplelogger=DEBUG
```

If you are also using `io.avaje:avaje-aws-appconfig`, then you can additionally set `io.avaje.config` to `TRACE`.

```properties
log.level.io.avaje.config=TRACE
```


## Configure

Configure the logger via main resource `avaje-logger.properties`
and test resource `avaje-logger-test.properties`


```properties
## specify the default log level to use
logger.defaultLogLevel=warn

## specify to log as `json` or `plain` format (defaults to json)
#logger.format=json
logger.format=plain

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

## Structured JSON - logger.format=json

By default, the log format is JSON. Example:

```json
{
  "component":"my-application",
  "env":"dev",
  "timestamp":"2025-07-14T13:44:44.230959+12:00",
  "level":"TRACE",
  "logger":"io.avaje.config",
  "message":"load from [resource:application-test.properties]",
  "thread":"main"
}
```

To override the json property names use `logger.propertyNames` delimited by `=` and `,` like:
```properties
## delimited by comma and equals
logger.propertyNames=logger=loggerName,env=environment,timestamp=@timestamp
```

Example overriding a single property:
```properties
logger.propertyNames=logger=loggerName
```

#### component

A `component` key value is added if:
- There is a `logger.component` property set in `avaje-logger.properties`
- There is a `COMPONENT` environment variable set
- K8s is detected, it will be derived from the `HOSTNAME` environment variable

This key value is expected to represent the application component that is the source of
the logs.

Examples:
```properties
## a literal value
logger.component=my-application

## uses system property `SERVICE_NAME` or environment variable `SERVICE_NAME`
logger.component=${SERVICE_NAME}

## uses system property `service.name` or environment variable `SERVICE_NAME`
logger.component=${service.name}

```

#### env - Environment

An `env` key value is added automatically if:
- There is a `logger.environment` property set in `avaje-logger.properties`
- There is an `ENVIRONMENT` environment variable set

Examples:
```properties
## uses system property `app.env` or environment variable `APP_ENV`, defaults to `localdev`
logger.environment=${app.env:localdev}

## literal value
logger.environment=DEV
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
    <version>1.0</version>
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

