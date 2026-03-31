package com.cmbchina.datadirect.caliber.application.service.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class HashEmbeddingSupport {

    private final GraphRuntimeProperties graphRuntimeProperties;

    public HashEmbeddingSupport(GraphRuntimeProperties graphRuntimeProperties) {
        this.graphRuntimeProperties = graphRuntimeProperties;
    }

    public List<Double> embed(String text) {
        int dimension = Math.max(8, graphRuntimeProperties.getEmbeddingDimension());
        double[] values = new double[dimension];
        String normalized = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            for (String token : normalized.split("[^\\p{IsAlphabetic}\\p{IsDigit}_]+")) {
                if (token == null || token.isBlank()) {
                    continue;
                }
                int index = Math.floorMod(token.hashCode(), dimension);
                values[index] += 1d;
            }
        }
        double norm = 0d;
        for (double value : values) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> result = new ArrayList<>(dimension);
        for (double value : values) {
            result.add(norm == 0d ? 0d : value / norm);
        }
        return result;
    }

    public double cosine(List<Double> left, List<Double> right) {
        int size = Math.min(left == null ? 0 : left.size(), right == null ? 0 : right.size());
        if (size == 0) {
            return 0d;
        }
        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int index = 0; index < size; index += 1) {
            double l = left.get(index);
            double r = right.get(index);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0d || rightNorm == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
