package ch.thp.mas.llm.variance.analyze.semantic;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HierarchicalClusterer {

    public int[] cluster(double[][] distances, HierarchicalConfig config) {
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            clusters.add(new ArrayList<>(List.of(i)));
        }

        while (clusters.size() > 1) {
            MergeCandidate candidate = closest(clusters, distances, config.linkage());
            if (candidate.distance() > config.threshold()) {
                break;
            }
            clusters.get(candidate.left()).addAll(clusters.get(candidate.right()));
            clusters.remove(candidate.right());
        }

        int[] labels = new int[distances.length];
        for (int clusterId = 0; clusterId < clusters.size(); clusterId++) {
            for (int point : clusters.get(clusterId)) {
                labels[point] = clusterId;
            }
        }
        return labels;
    }

    private MergeCandidate closest(List<List<Integer>> clusters, double[][] distances, HierarchicalLinkage linkage) {
        MergeCandidate best = null;
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                double distance = linkageDistance(clusters.get(i), clusters.get(j), distances, linkage);
                if (best == null || distance < best.distance()) {
                    best = new MergeCandidate(i, j, distance);
                }
            }
        }
        return best;
    }

    private double linkageDistance(
            List<Integer> left,
            List<Integer> right,
            double[][] distances,
            HierarchicalLinkage linkage
    ) {
        double sum = 0.0;
        double max = 0.0;
        int count = 0;
        for (int leftIndex : left) {
            for (int rightIndex : right) {
                double distance = distances[leftIndex][rightIndex];
                sum += distance;
                max = Math.max(max, distance);
                count++;
            }
        }
        return linkage == HierarchicalLinkage.COMPLETE ? max : sum / count;
    }

    private record MergeCandidate(int left, int right, double distance) {
    }
}
