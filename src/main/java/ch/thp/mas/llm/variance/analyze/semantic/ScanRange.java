package ch.thp.mas.llm.variance.analyze.semantic;

import ch.thp.mas.llm.variance.analyze.AnalysisException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public record ScanRange(@JsonIgnore int fromHundredths, @JsonIgnore int toHundredths) {

    public ScanRange {
        if (fromHundredths < 0 || toHundredths < 0) {
            throw new AnalysisException("Scan range bounds must be non-negative.");
        }
        if (fromHundredths > toHundredths) {
            throw new AnalysisException("Scan range from must be less than or equal to to.");
        }
    }

    public static ScanRange ofHundredths(int fromHundredths, int toHundredths) {
        return new ScanRange(fromHundredths, toHundredths);
    }

    public static ScanRange of(double from, double to, double increment, String name) {
        int incrementHundredths = hundredths(increment, "analysis.scanIncrement");
        int fromHundredths = hundredths(from, name + ".from");
        int toHundredths = hundredths(to, name + ".to");
        if ((toHundredths - fromHundredths) % incrementHundredths != 0) {
            throw new AnalysisException(name + ".to does not align to scanIncrement " + increment + ": " + to);
        }
        return new ScanRange(fromHundredths, toHundredths);
    }

    public static int incrementHundredths(double increment) {
        int incrementHundredths = hundredths(increment, "analysis.scanIncrement");
        if (incrementHundredths < 1) {
            throw new AnalysisException("analysis.scanIncrement must be at least 0.01.");
        }
        return incrementHundredths;
    }

    public List<Double> values(double increment) {
        int incrementHundredths = incrementHundredths(increment);
        List<Double> values = new ArrayList<>();
        for (int value = fromHundredths; value <= toHundredths; value += incrementHundredths) {
            values.add(toDouble(value));
        }
        return values;
    }

    @JsonProperty("from")
    public double from() {
        return toDouble(fromHundredths);
    }

    @JsonProperty("to")
    public double to() {
        return toDouble(toHundredths);
    }

    private static int hundredths(double value, String name) {
        BigDecimal decimal = BigDecimal.valueOf(value).movePointRight(2);
        try {
            return decimal.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException e) {
            throw new AnalysisException(name + " must align to hundredths: " + value, e);
        }
    }

    private static double toDouble(int hundredths) {
        return BigDecimal.valueOf(hundredths, 2).doubleValue();
    }
}
