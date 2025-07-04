/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.avaje.simplelogger.encoder;

/**
 * formats stack traces by doing the following:
 *
 * <ul>
 *   <li>Limits the number of stackTraceElements per throwable (applies to each individual
 *       throwable. e.g. caused-bys and suppressed). See {@link #maxDepthPerThrowable}.
 *   <li>Limits the total length in characters of the trace. See {@link #maxLength}.
 *   <li>Abbreviates class names based. See setShortenedClassNameLength.
 *   <li>Outputs in either 'normal' order (root-cause-last), or root-cause-first. See {@link
 *       #rootCauseFirst}.
 * </ul>
 *
 * <p>The other options can be listed in any order and are interpreted as follows:
 *
 * <ul>
 *   <li>"rootFirst" - indicating that stacks should be printed root-cause first
 *   <li>"inlineHash" - indicating that hexadecimal error hashes should be computed and inlined
 *   <li>"inline" - indicating that the whole stack trace should be inlined, using "\n" as
 *       separator
 *   <li>"omitCommonFrames" - omit common frames
 *   <li>"keepCommonFrames" - keep common frames
 *   <li>evaluator name - name of evaluators that will determine if the stacktrace is ignored
 *   <li>exclusion pattern - pattern for stack trace elements to exclude
 * </ul>
 */
final class ThrowableConverter {

  private static final int REGULAR_EXCEPTION_INDENT = 1;
  private static final int SUPPRESSED_EXCEPTION_INDENT = 1;
  private static final String ELLIPSIS = "...";
  private static final String CAUSED_BY = "Caused by: ";
  private static final String SUPPRESSED = "Suppressed: ";
  private static final String WRAPPED_BY = "Wrapped by: ";

  private final String lineSeparator = "\n"; // System.getProperty("line.separator");
  /** Maximum number of stackTraceElements printed per throwable. */
  private final int maxDepthPerThrowable = Integer.getInteger("avaje.logback.maxDepthPerThrowable", Integer.MAX_VALUE);
  /** Maximum number of characters in the entire stacktrace. */
  private final int maxLength = Integer.getInteger("avaje.logback.maxThrowableLength", 20_000);
  /** Abbreviator used to shorten the classnames.*/
  private final Abbreviator abbreviator = Abbreviator.create(100);
  private final StackElementFilter stackElementFilter = StackElementFilter.any();
  private final boolean rootCauseFirst = false;
  private final int bufferSize;

  ThrowableConverter() {
    this.bufferSize = Math.min(4096, this.maxLength + 100 > 0 ? this.maxLength + 100 : this.maxLength);
  }

  String convert(Throwable throwableProxy) {
    final StringBuilder builder = new StringBuilder(bufferSize);
    if (rootCauseFirst) {
      appendRootCauseFirst(builder, null, REGULAR_EXCEPTION_INDENT, throwableProxy);
    } else {
      appendRootCauseLast(builder, null, REGULAR_EXCEPTION_INDENT, throwableProxy);
    }
    if (builder.length() > this.maxLength) {
      builder.setLength(this.maxLength - ELLIPSIS.length() - lineSeparator.length());
      builder.append(ELLIPSIS).append(lineSeparator);
    }
    return builder.toString();
  }

  /**
   * Appends a throwable and recursively appends its causedby/suppressed throwables in "normal"
   * order (Root cause last).
   */
  private void appendRootCauseLast(StringBuilder builder, String prefix, int indent, Throwable throwableProxy) {
    if (throwableProxy == null || builder.length() > this.maxLength) {
      return;
    }

    appendFirstLine(builder, prefix, indent, throwableProxy);
    appendStackTraceElements(builder, indent, throwableProxy);

    final Throwable[] suppressedThrowableProxies = throwableProxy.getSuppressed();
    if (suppressedThrowableProxies != null) {
      for (final Throwable suppressedThrowableProxy : suppressedThrowableProxies) {
        // stack hashes are not computed/inlined on suppressed errors
        appendRootCauseLast(
            builder,
            SUPPRESSED,
            indent + SUPPRESSED_EXCEPTION_INDENT,
            suppressedThrowableProxy);
      }
    }
    appendRootCauseLast(builder, CAUSED_BY, indent, throwableProxy.getCause());
  }

  /**
   * Appends a throwable and recursively appends its causedby/suppressed throwables in "reverse"
   * order (Root cause first).
   */
  private void appendRootCauseFirst(StringBuilder builder, String prefix, int indent, Throwable throwableProxy) {
    if (throwableProxy == null || builder.length() > this.maxLength) {
      return;
    }

    if (throwableProxy.getCause() != null) {
      appendRootCauseFirst(builder, prefix, indent, throwableProxy.getCause());
      prefix = WRAPPED_BY;
    }

    appendFirstLine(builder, prefix, indent, throwableProxy);
    appendStackTraceElements(builder, indent, throwableProxy);

    final Throwable[] suppressedThrowableProxies = throwableProxy.getSuppressed();
    if (suppressedThrowableProxies != null) {
      for (final Throwable suppressedThrowableProxy : suppressedThrowableProxies) {
        // stack hashes are not computed/inlined on suppressed errors
        appendRootCauseFirst(
            builder,
            SUPPRESSED,
            indent + SUPPRESSED_EXCEPTION_INDENT,
            suppressedThrowableProxy);
      }
    }
  }

  /** Appends the frames of the throwable. */
  private void appendStackTraceElements(StringBuilder builder, int indent, Throwable throwableProxy) {
    if (builder.length() > this.maxLength) {
      return;
    }
    final StackTraceElement[] stackTraceElements = throwableProxy.getStackTrace();
    final int commonFrames = 0;// isOmitCommonFrames() ? throwableProxy.getCommonFrames() : 0;

    boolean appendingExcluded = false;
    int consecutiveExcluded = 0;
    int appended = 0;
    StackTraceElement previousWrittenStackTraceElement = null;

    int i = 0;
    for (; i < stackTraceElements.length - commonFrames; i++) {
      if (this.maxDepthPerThrowable > 0 && appended >= this.maxDepthPerThrowable) {
        // We reached the configured limit. Bail out.
        break;
      }
      final StackTraceElement stackTraceElement = stackTraceElements[i];
      if (i < 1 || isIncluded(stackTraceElement)) { // First 2 frames are always included
        // We should append this line
        // consecutiveExcluded will be > 0 if we were previously skipping lines based on excludes
        if (consecutiveExcluded >= 2) {
          // Multiple consecutive lines were excluded, so append a placeholder
          appendPlaceHolder(builder, indent, consecutiveExcluded, "frames excluded");
          consecutiveExcluded = 0;
        } else if (consecutiveExcluded == 1) {
          // We only excluded one line, so just go back and include it
          // instead of printing the excluding message for it.
          appendingExcluded = true;
          consecutiveExcluded = 0;
          i -= 2;
          continue;
        }
        appendStackTraceElement(builder, indent, stackTraceElement, previousWrittenStackTraceElement);
        previousWrittenStackTraceElement = stackTraceElement;
        appendingExcluded = false;
        appended++;
      } else if (appendingExcluded) {
        // We're going back and appending something we previously excluded
        appendStackTraceElement(builder, indent, stackTraceElement, previousWrittenStackTraceElement);
        previousWrittenStackTraceElement = stackTraceElement;
        appended++;
      } else {
        consecutiveExcluded++;
      }

      // if (shouldTruncateAfter(stackTraceElement)) {
      //   // Truncate after this line. Bail out.
      //   break;
      // }
    }

    // We did not process the stack up to the last element (max depth, truncate line)
    if (i + commonFrames < stackTraceElements.length) {
      // We were excluding elements but we want the truncateAfter element to be printed
      if (consecutiveExcluded > 0) {
        consecutiveExcluded--;
        appendPlaceHolder(builder, indent, consecutiveExcluded, "frames excluded");

        appendStackTraceElement(builder, indent, stackTraceElements[i], previousWrittenStackTraceElement);
        appended++;
      }

      if (commonFrames > 0) {
        appendPlaceHolder(
            builder,
            indent,
            stackTraceElements.length - appended - consecutiveExcluded,
            "frames truncated (including " + commonFrames + " common frames)");
      } else {
        appendPlaceHolder(
            builder,
            indent,
            stackTraceElements.length - appended - consecutiveExcluded,
            "frames truncated");
      }
    } else {
      if (consecutiveExcluded > 0) {
        // We were excluding stuff at the end, so append a placeholder
        appendPlaceHolder(builder, indent, consecutiveExcluded, "frames excluded");
      }

      if (commonFrames > 0) {
        // Common frames found, append a placeholder
        appendPlaceHolder(builder, indent, commonFrames, "common frames omitted");
      }
    }
  }

  /** Appends a placeholder indicating that some frames were not written. */
  private void appendPlaceHolder(StringBuilder builder, int indent, int consecutiveExcluded, String message) {
    indent(builder, indent);
    builder
        .append(ELLIPSIS)
        .append(" ")
        .append(consecutiveExcluded)
        .append(" ")
        .append(message)
        .append(lineSeparator);
  }

  /**
   * Return {@code true} if the stack trace element is included (i.e. doesn't match any exclude
   * patterns).
   *
   * @return {@code true} if the stacktrace element is included
   */
  private boolean isIncluded(StackTraceElement step) {
    return stackElementFilter.accept(step);
  }

  /** Appends a single stack trace element. */
  private void appendStackTraceElement(StringBuilder builder, int indent, StackTraceElement step, StackTraceElement previousStep) {
    if (builder.length() > this.maxLength) {
      return;
    }
    indent(builder, indent);

    final String fileName = step.getFileName();
    final int lineNumber = step.getLineNumber();
    builder
        .append("at ")
        .append(abbreviator.abbreviate(step.getClassName()))
        .append(".")
        .append(step.getMethodName())
        .append("(")
        .append(fileName == null ? "Unknown Source" : fileName);

    if (lineNumber >= 0) {
      builder.append(":").append(lineNumber);
    }
    builder.append(")");
    builder.append(lineSeparator);
  }

  /** Appends the first line containing the prefix and throwable message */
  private void appendFirstLine(StringBuilder builder, String prefix, int indent, Throwable throwableProxy) {
    if (builder.length() > this.maxLength) {
      return;
    }
    indent(builder, indent - 1);
    if (prefix != null) {
      builder.append(prefix);
    }
    builder
        .append(abbreviator.abbreviate(throwableProxy.getClass().getName())) //throwableProxy.getClassName()))
        .append(": ")
        .append(throwableProxy.getMessage())
        .append(lineSeparator);
  }

  private void indent(StringBuilder builder, int indent) {
      builder.append(" ".repeat(Math.max(0, indent)));
  }

//  /**
//   * Set the length to which class names should be abbreviated. Cannot be used if a custom {@link
//   * Abbreviator} has been set through {@link #setClassNameAbbreviator(Abbreviator)}.
//   *
//   * @param length the desired maximum length or {@code -1} to disable the feature and allow for any
//   *     arbitrary length.
//   */
//  public void setShortenedClassNameLength(int length) {
//    if (!(this.abbreviator instanceof TrimPackageAbbreviator)) {
//      throw new IllegalStateException(
//          "Cannot set shortenedClassNameLength on non default Abbreviator");
//    }
//    ((TrimPackageAbbreviator) this.abbreviator).setTargetLength(length);
//  }
//
//  /**
//   * Get the class name abbreviation target length. Cannot be used if a custom {@link Abbreviator}
//   * has been set through {@link #setClassNameAbbreviator(Abbreviator)}.
//   *
//   * @return the abbreviation target length
//   */
//  public int getShortenedClassNameLength() {
//    if (this.abbreviator instanceof TrimPackageAbbreviator) {
//      return ((TrimPackageAbbreviator) this.abbreviator).getTargetLength();
//    }
//    throw new IllegalStateException(
//        "Cannot invoke getShortenedClassNameLength on non default abbreviator");
//  }
//
//  /**
//   * Set a custom {@link Abbreviator} used to shorten class names.
//   *
//   * @param abbreviator the {@link Abbreviator} to use.
//   */
//  // @DefaultClass(TrimPackageAbbreviator.class)
//  public void setClassNameAbbreviator(Abbreviator abbreviator) {
//    this.abbreviator = Objects.requireNonNull(abbreviator);
//  }
//
//  public Abbreviator getClassNameAbbreviator() {
//    return this.abbreviator;
//  }
//
//  /**
//   * Set a limit on the number of stackTraceElements per throwable. Use {@code -1} to disable the
//   * feature and allow for an unlimited depth.
//   *
//   * @param maxDepthPerThrowable the maximum number of stacktrace elements per throwable or {@code
//   *     -1} to disable the feature and allows for an unlimited amount.
//   */
//  public void setMaxDepthPerThrowable(int maxDepthPerThrowable) {
//    if (maxDepthPerThrowable <= 0 && maxDepthPerThrowable != -1) {
//      throw new IllegalArgumentException(
//          "maxDepthPerThrowable must be > 0, or -1 to disable the feature");
//    }
//    if (maxDepthPerThrowable == -1) {
//      maxDepthPerThrowable = FULL_MAX_DEPTH_PER_THROWABLE;
//    }
//    this.maxDepthPerThrowable = maxDepthPerThrowable;
//  }
//
//  public int getMaxDepthPerThrowable() {
//    return maxDepthPerThrowable;
//  }
//
//  /**
//   * Set a hard limit on the size of the rendered stacktrace, all throwables included. Use {@code
//   * -1} to disable the feature and allows for any size.
//   *
//   * @param maxLength the maximum size of the rendered stacktrace or {@code -1} for no limit.
//   */
//  public void setMaxLength(int maxLength) {
//    if (maxLength <= 0 && maxLength != -1) {
//      throw new IllegalArgumentException("maxLength must be > 0, or -1 to disable the feature");
//    }
//    if (maxLength == -1) {
//      maxLength = FULL_MAX_LENGTH;
//    }
//    this.maxLength = maxLength;
//  }
//
//  public int getMaxLength() {
//    return maxLength;
//  }
//
//  /**
//   * Control whether common frames should be omitted for nested throwables or not.
//   *
//   * @param omitCommonFrames {@code true} to omit common frames
//   */
//  public void setOmitCommonFrames(boolean omitCommonFrames) {
//    this.omitCommonFrames = omitCommonFrames;
//  }
//
//  public boolean isOmitCommonFrames() {
//    return this.omitCommonFrames;
//  }
//
//  public boolean isRootCauseFirst() {
//    return rootCauseFirst;
//  }
//
//  public void setRootCauseFirst(boolean rootCauseFirst) {
//    this.rootCauseFirst = rootCauseFirst;
//  }
//
//  public void addExclude(String exclusionPattern) {
//    excludes.add(Pattern.compile(exclusionPattern));
//  }
//
//  public void setExcludes(List<String> patterns) {
//    this.excludes = new ArrayList<>(patterns.size());
//    for (final String pattern : patterns) {
//      addExclude(pattern);
//    }
//  }
//
//  public List<String> getExcludes() {
//    return this.excludes.stream().map(Pattern::pattern).collect(Collectors.toList());
//  }
//
//  public void addTruncateAfter(String regex) {
//    this.truncateAfterPatterns.add(Pattern.compile(regex));
//  }
//
//  public List<String> getTruncateAfters() {
//    return this.truncateAfterPatterns.stream().map(Pattern::pattern).collect(Collectors.toList());
//  }
//
//  public void setTruncateAfters(List<String> patterns) {
//    this.truncateAfterPatterns = new ArrayList<>(patterns.size());
//    for (final String pattern : patterns) {
//      addTruncateAfter(pattern);
//    }
//  }

}
