package io.avaje.simplelogger.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

/**
 * GraalVM Native Image build Feature that initialises Simple Logger classes at Build time.
 * <p>
 * Use this when using slf4j-jdk-platform-logging to redirect System.Logger to slf4j. This
 * is needed as GraalVM by default initialised JDK logging at build time. Adding this
 * feature supports redirecting System.Logger to avaje simple logger by also initialising
 * the avaje simple logger classes at build time.
 * </p>
 */
public class BuildInitialization implements Feature {

  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {

    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.FilterBuilder$PatternFilter"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.FilterBuilder$ReflectiveInvocation"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.FilterBuilder$SpringFilter"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.FilterBuilder$Generated"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.FilterBuilder$Group"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.FilterBuilder$JDKInternals"));

    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.StackElementFilter"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.AbbreviatorByLength"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.AbbreviatorCaching"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.StackHasher"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.ThrowableConverter"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.JsonEncoder"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.JsonWriter"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.SimpleLogger"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.simplelogger.encoder.PlainLogWriter"));


    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.json.stream.core.JsonNames"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.json.stream.core.CoreJsonStream"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.json.stream.core.HybridBufferRecycler$XorShiftThreadProbe"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.json.stream.core.HybridBufferRecycler"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.json.stream.core.HybridBufferRecycler$StripedLockFreePool"));
    RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("io.avaje.json.stream.core.Recyclers$ThreadLocalPool"));

    // check for optional SLF4JPlatformLogger
    Class<?> slfPlatform = access.findClassByName("org.slf4j.jdk.platform.logging.SLF4JPlatformLogger");
    if (slfPlatform != null) {
      RuntimeClassInitialization.initializeAtBuildTime(slfPlatform);
      RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("org.slf4j.jdk.platform.logging.SLF4JPlatformLoggerFactory"));
      RuntimeClassInitialization.initializeAtBuildTime(access.findClassByName("org.slf4j.jdk.platform.logging.SLF4JSystemLoggerFinder"));
    }
  }

}
