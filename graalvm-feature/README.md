# avaje-simple-logger-graalvm-feature

When we want to use `slf4j-jdk-platform-logging` with graalvm native-image like:
```xml
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-jdk-platform-logging</artifactId>
      <version>2.0.17</version>
    </dependency>
```
we can have the issue that native-image wants to initialise the JDK logging at *BUILD* time.
So this means that if we want to use `avaje-simple-logger`, we also need to initialise it's
classes at build time as well - this is what the `avaje-simple-logger-graalvm-feature` does.

```xml
  <dependency> <!-- redirect System.Logger to slf4j -->
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-jdk-platform-logging</artifactId>
    <version>2.0.17</version>
  </dependency>

  <dependency> <!-- logger implementation for sl4j-->
    <groupId>io.avaje</groupId>
    <artifactId>avaje-simple-logger</artifactId>
    <version>1.0</version>
  </dependency>

  <dependency> <!-- with graalvm, use build time initialisation -->
    <groupId>io.avaje</groupId>
    <artifactId>avaje-simple-logger-graalvm-feature</artifactId>
    <version>0.1</version>
  </dependency>

```

## How to use

Add the dependency below. It will automatically register with graalvm native-image
compilation to use the `io.avaje.simplelogger.graalvm.BuildInitialization` feature.

```xml
  <dependency> <!-- with graalvm, use build time initialisation -->
    <groupId>io.avaje</groupId>
    <artifactId>avaje-simple-logger-graalvm-feature</artifactId>
    <version>0.1</version>
  </dependency>

```

That's it.

Anything using JDK System.Logger, should now be logged using avaje-simple-logger.
