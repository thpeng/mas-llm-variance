package ch.thp.mas.llm.variance.metanalysis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class ManualEvaluationId {

    static final long SAMPLE_SEED = 20260529L;
    static final String SCHEMA = "manual-hallucination-evaluation-sample-v1";

    private ManualEvaluationId() {
    }

    static String id(String seriesId, int index) {
        return "bms-" + letterHash(SCHEMA + "|" + SAMPLE_SEED + "|" + seriesId + "|" + index, 24);
    }

    private static String letterHash(String value, int length) {
        try {
            String alphabet = "abcdef";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(length);
            int index = 0;
            while (builder.length() < length) {
                int valueByte = hash[index % hash.length] & 0xff;
                builder.append(alphabet.charAt(valueByte % alphabet.length()));
                index++;
            }
            return builder.toString();
        } catch (Exception e) {
            throw new MetaAnalysisException("Could not hash manual evaluation sample id.", e);
        }
    }
}
