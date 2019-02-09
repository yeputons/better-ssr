package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.psi.PsiElement;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SyntacticalMatchResult {
    PsiElement getPatternElement();
    SmartPsiPointer getMatchedElement();
    CompiledPattern getCompiledPattern();

    @NotNull List<SyntacticalMatchResult> getChildren();
}