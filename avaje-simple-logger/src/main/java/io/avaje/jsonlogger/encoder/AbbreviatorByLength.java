package io.avaje.jsonlogger.encoder;

final class AbbreviatorByLength implements Abbreviator {

    private final int targetLength;

    AbbreviatorByLength(int targetLength) {
        this.targetLength = targetLength;
    }

    @Override
    public String abbreviate(String fqClassName) {
        if (fqClassName == null) {
            throw new IllegalArgumentException("Class name may not be null");
        } else {
            int inLen = fqClassName.length();
            if (inLen < this.targetLength) {
                return fqClassName;
            } else {
                StringBuilder buf = new StringBuilder(inLen);
                int rightMostDotIndex = fqClassName.lastIndexOf(46);
                if (rightMostDotIndex == -1) {
                    return fqClassName;
                } else {
                    int lastSegmentLength = inLen - rightMostDotIndex;
                    int leftSegments_TargetLen = this.targetLength - lastSegmentLength;
                    if (leftSegments_TargetLen < 0) {
                        leftSegments_TargetLen = 0;
                    }

                    int leftSegmentsLen = inLen - lastSegmentLength;
                    int maxPossibleTrim = leftSegmentsLen - leftSegments_TargetLen;
                    int trimmed = 0;
                    boolean inDotState = true;

                    int i;
                    for(i = 0; i < rightMostDotIndex; ++i) {
                        char c = fqClassName.charAt(i);
                        if (c == '.') {
                            if (trimmed >= maxPossibleTrim) {
                                break;
                            }

                            buf.append(c);
                            inDotState = true;
                        } else if (inDotState) {
                            buf.append(c);
                            inDotState = false;
                        } else {
                            ++trimmed;
                        }
                    }

                    buf.append(fqClassName.substring(i));
                    return buf.toString();
                }
            }
        }
    }
}
