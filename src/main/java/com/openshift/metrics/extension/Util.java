package com.openshift.metrics.extension;

public class Util {
    public static String encodeCronExpression(String cronExpression) {
        // need to convert spaces to underscores
        String encoded = cronExpression.replaceAll(" ", "_");

        // need to convert all *s to something else, e.g. ^
        encoded = encoded.replaceAll("\\*", "^");

        return encoded;
    }

    public static String decodeCronExpression(String cronExpression) {
        // convert underscores back to spaces
        String decoded = cronExpression.replaceAll("_", " ");

        // convert ^s back to *s
        decoded = decoded.replaceAll("\\^", "*");

        return decoded;
    }
}
