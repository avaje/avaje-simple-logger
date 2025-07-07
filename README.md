# avaje-simple-logger

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
