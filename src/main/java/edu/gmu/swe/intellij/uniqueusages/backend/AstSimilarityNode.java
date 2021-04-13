package edu.gmu.swe.intellij.uniqueusages.backend;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class AstSimilarityNode {
    private CodeBlockUsage representativeElement;
    private UniqueUsageGroup group;

    public Set<CodeBlockUsage> getElements() {
        return elements;
    }

    public void setElements(Set<CodeBlockUsage> elements) {
        this.elements = elements;
    }

    private Set<CodeBlockUsage> elements = new HashSet<>();

    public AstSimilarityNode(CodeBlockUsage representativeElement, UniqueUsageGroup group) {
        this.representativeElement = representativeElement;
        this.group = group;
        elements.add(representativeElement);
    }

    public double getSimilarityTo(CodeBlockUsage o) {
//            return this.representativeElement.compareSimilarity(o);

        Stream<Double> similarityStream = elements.parallelStream().map(elem -> elem.compareSimilarity(o));
        Optional<Double> lowestSimilarity = similarityStream.min(Double::compareTo);
        if (!lowestSimilarity.isPresent()) {
            throw new RuntimeException("This should never happen");
        }
        return lowestSimilarity.get();
    }

    public CodeBlockUsage getRepresentativeElement() {
        return representativeElement;
    }

    public UniqueUsageGroup getGroup() {
        return group;
    }
}
