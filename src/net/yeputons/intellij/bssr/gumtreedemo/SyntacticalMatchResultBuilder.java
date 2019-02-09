package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.MatchingDetailsListener;
import com.intellij.structuralsearch.StructuralSearchUtil;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer;
import com.intellij.util.containers.Stack;
import com.intellij.util.containers.hash.HashMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class SyntacticalMatchResultBuilder implements MatchingDetailsListener {
    private Stack<SyntacticalMatchResultImpl> syntacticalResults = new Stack<>(new SyntacticalMatchResultImpl());
    private final Stack<Stack<SyntacticalMatchResultImpl>> oldContexts = new Stack<>();
    private final Map<MatchResult, SyntacticalMatchResult> matchResults = new HashMap<>();
    private CompiledPattern pattern;

    @Override
    public void setPattern(CompiledPattern pattern) {
        assert oldContexts.empty();
        assert syntacticalResults.size() == 1;
        syntacticalResults.peek().setCompiledPattern(pattern);
        this.pattern = pattern;
    }

    @Override
    public void clear() {
        newMatchContext();
        oldContexts.clear();
        pattern = null;
    }

    @Override
    public void pushNewMatchContext() {
        oldContexts.add(syntacticalResults);
        newMatchContext();
    }

    @Override
    public void newMatchContext() {
        syntacticalResults = new Stack<>(new SyntacticalMatchResultImpl());
        syntacticalResults.peek().setCompiledPattern(pattern);
    }

    @Override
    public void popNewMatchContext() {
        syntacticalResults = oldContexts.pop();
    }

    @Override
    public void enterTryMatching(PsiElement patternElement, PsiElement matchedElement) {
        syntacticalResults.add(new SyntacticalMatchResultImpl(patternElement, new SmartPsiPointer(matchedElement)));
    }

    @Override
    public void exitTryMatching(boolean matched) {
        SyntacticalMatchResultImpl newChild = syntacticalResults.pop();
        if (matched) {
            newChild.setCompiledPattern(pattern);;
            syntacticalResults.peek().addChild(newChild);
        }
    }

    @Override
    public void removeLastMatches(int numberOfResults) {
        syntacticalResults.peek().popChildren(numberOfResults);
    }

    @Override
    public void onAddedResult(MatchResult result) {
        if (result.isTarget()) {
            PsiElement match = result.getMatch();
            PsiElement willBeUsedForReplace = StructuralSearchUtil.getPresentableElement(match);
            assert PsiTreeUtil.isAncestor(willBeUsedForReplace, match, false);

            assert !syntacticalResults.isEmpty();
            if (syntacticalResults.size() == 1) {
                assert syntacticalResults.get(0).getPatternElement() == null;
                assert syntacticalResults.get(0).getMatchedElement() == null;
            } else {
                int i = syntacticalResults.size();
                while (i > 1) {
                    PsiElement newCandidate = syntacticalResults.get(i - 1).getMatchedElement().getElement();
                    if (PsiTreeUtil.isAncestor(willBeUsedForReplace, newCandidate, false)) {
                        i--;
                    } else {
                        break;
                    }
                }
                if (i < syntacticalResults.size()) {
                    assert i >= 1;
                    matchResults.put(result, syntacticalResults.get(i));
                    for (SyntacticalMatchResult child : syntacticalResults.get(i - 1).getChildren()) {
                        if (child == syntacticalResults.get(i)) {
                            continue;
                        }
                        assert child.getMatchedElement() != null;
                        PsiElement childMatched = child.getMatchedElement().getElement();
                        assert childMatched != null;
                        assert !PsiTreeUtil.isAncestor(willBeUsedForReplace, childMatched, false);
                    }
                }
            }
        } else {
            matchResults.put(result, syntacticalResults.peek());
        }
    }

    @Override
    public void onNoSubstitutionMatch(List<PsiElement> matchedNodes, MatchResult result) {
        matchResults.put(result, syntacticalResults.peek());
    }

    @Override
    public void moveMatchResult(@NotNull MatchResult from, @NotNull MatchResult to) {
        SyntacticalMatchResult result = matchResults.remove(from);
        if (result != null) {
            matchResults.put(to, result);
        }
    }

    public Map<MatchResult, SyntacticalMatchResult> getMatchResults() {
        return matchResults;  // Should be a reference for real-time search..?
    }
}
