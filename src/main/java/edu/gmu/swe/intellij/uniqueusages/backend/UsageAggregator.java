package edu.gmu.swe.intellij.uniqueusages.backend;

import com.github.gumtreediff.matchers.Mapping;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class UsageAggregator {

    private final static AstComparator AST_COMPARATOR = new AstComparator();
    private final static double SIMILIAR_THRESHOLD = 0.5;



    private Map<String, UniqueUsageGroup> usageToUniqueUsageGroup = new HashMap<String, UniqueUsageGroup> ();
    private Map<String, UsageInfo> usageToUsageInfo = new HashMap<String, UsageInfo> ();
    private Set<String> codeBlockSet = new HashSet<>();

    private Map<UniqueUsageGroup, CodeBlockUsage> usageInfoToCodeBlockMap = new HashMap<>();

    private SortedMap<CodeBlockUsage, UniqueUsageGroup> codeBlockIntegerSortedMap = new TreeMap<>();

    private List<AstSimilarityNode> astSimilarityList = new LinkedList<>();

    private static class AstSimilarityNode {
        private CodeBlockUsage representativeElement;
        private UniqueUsageGroup group;

        public AstSimilarityNode(CodeBlockUsage representativeElement, UniqueUsageGroup group) {
            this.representativeElement = representativeElement;
            this.group = group;
        }

        public CodeBlockUsage getRepresentativeElement() {
            return representativeElement;
        }

        public UniqueUsageGroup getGroup() {
            return group;
        }
    }



    public synchronized UsageGroup getAggregateUsage(Usage usage) {
        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
        String key = usageInfo.getElement().getContext().getText();

        AstComparator astComparator = new AstComparator();

        PsiElement currentElement = usageInfo.getElement();
        while (currentElement != null && !currentElement.toString().equals("PsiCodeBlock")) {
            currentElement = currentElement.getContext();
        }
        PsiElement codeBlockElement = currentElement; // TODO actually do something with this like AST diffing.

        CodeBlockUsage codeBlockUsage;
        if (codeBlockElement != null) {
            codeBlockUsage = new CodeBlockUsage(codeBlockElement);

            if (codeBlockSet.contains(codeBlockUsage.getCode())) {
                return null;
            }
            codeBlockSet.add(codeBlockUsage.getCode());

            UniqueUsageGroup mostSimilarAstKey = null;
            double highestSimilarityRating = 0.0;
            for (AstSimilarityNode astSimilarityNode: astSimilarityList) {
                double similiarityRating = astSimilarityNode.getRepresentativeElement().astPercentageSimilarTo(codeBlockUsage);
                if (similiarityRating > highestSimilarityRating) {
                    highestSimilarityRating = similiarityRating;
                    mostSimilarAstKey = astSimilarityNode.getGroup();
                }
            }

            if (highestSimilarityRating > SIMILIAR_THRESHOLD) {
                // Check if returning exact match because classic find usages is weird.
                if (highestSimilarityRating == 1.0) {
                    return null; // exact match so return nothing we've got this already.
                }
                mostSimilarAstKey.incrementUsageCount();
                return mostSimilarAstKey;
            }
            else {
                // Create and return a codeblock usage key
                UniqueUsageGroup newAstKey = new UniqueUsageGroup("Similar Usage");
                astSimilarityList.add(new AstSimilarityNode(codeBlockUsage, newAstKey));
                return newAstKey;
            }
        }
        else {
            return null;
//            return usageToUniqueUsageGroup.get(key);
        }
    }

    static class _InvalidUsageGroup implements UsageGroup {
        @Nullable
        @Override
        public Icon getIcon(boolean isOpen) {
            return null;
        }

        @NotNull
        @Override
        public String getText(@Nullable UsageView view) {
            return null;
        }

        @Nullable
        @Override
        public FileStatus getFileStatus() {
            return null;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public void update() {

        }

        @Override
        public void navigate(boolean requestFocus) {

        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public int compareTo(@NotNull UsageGroup o) {
            return 0;
        }
    }

    static class CodeBlockUsage {
        private PsiElement codeBlock;
        public CodeBlockUsage(PsiElement codeBlock) {
            this.codeBlock = codeBlock;
        }
        public String getCode() {
            return codeBlock.getText();
        }

        public double astPercentageSimilarTo(@NotNull CodeBlockUsage o) {
            String fakeBeginStub = "class Foo { void foo() ";
            String fakeEndStub = "\n}";
            String myFakeAST = fakeBeginStub + this.getCode() + fakeEndStub;
            String otherFakeAST = fakeBeginStub + o.getCode() + fakeEndStub;
            Diff astDiff = AST_COMPARATOR.compare(myFakeAST, otherFakeAST);

            List<Operation> differences = astDiff.getRootOperations();
            Set<Mapping> similiarities = astDiff.getMappingsComp().asSet();

            double percentageSimilar = ((double) similiarities.size()) / (differences.size() + similiarities.size());
            return percentageSimilar;
        }

        @Override
        public String toString() {
            return this.getCode();
        }
    }
}
