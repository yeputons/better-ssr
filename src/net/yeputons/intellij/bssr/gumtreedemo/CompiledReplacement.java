package net.yeputons.intellij.bssr.gumtreedemo;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.TreeContext;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;

import java.util.List;

public class CompiledReplacement {
    public CompiledPattern searchPattern, replacePattern;
    public TreeContext searchTreeContext, replaceTreeContext;
    public MappingStore mappings;
}
