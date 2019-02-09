package net.yeputons.intellij.bssr.gumtreedemo;


import com.intellij.psi.PsiElement;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyntacticalMatchResultImpl implements SyntacticalMatchResult {
    private PsiElement myPatternElement;
    private SmartPsiPointer myMatchedElement;
    private CompiledPattern myCompiledPattern;
    private final @NotNull List<SyntacticalMatchResult> children = new ArrayList<>();

    public SyntacticalMatchResultImpl() {
        myPatternElement = null;
        myMatchedElement = null;
    }

    public SyntacticalMatchResultImpl(PsiElement patternElement, SmartPsiPointer matchedElement) {
        assignMatch(patternElement, matchedElement);
    }

    void assignMatch(PsiElement patternElement, SmartPsiPointer matchedElement) {
        assert myPatternElement == null && myMatchedElement == null;
        myPatternElement = patternElement;
        myMatchedElement = matchedElement;
    }

    @Override
    public String toString() {
        return "SyntacticalMatchResultImpl:" + myPatternElement + ":" + myMatchedElement;
    }

    @Override
    public PsiElement getPatternElement() {
        return myPatternElement;
    }

    @Override
    public SmartPsiPointer getMatchedElement() {
        return myMatchedElement;
    }

    @Override
    @NotNull
    public List<SyntacticalMatchResult> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(@NotNull SyntacticalMatchResult child) {
        children.add(child);
    }

    public void setCompiledPattern(CompiledPattern pattern) {
        myCompiledPattern = pattern;
    }

    @Override
    public CompiledPattern getCompiledPattern() {
        return myCompiledPattern;
    }

    public void popChildren(int results) {
        while (results > 0 && !children.isEmpty()) {
            children.remove(children.size() - 1);
            results--;
        }
    }
}