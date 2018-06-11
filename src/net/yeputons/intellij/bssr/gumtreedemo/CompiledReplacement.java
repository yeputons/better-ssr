package net.yeputons.intellij.bssr.gumtreedemo;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;
import com.intellij.dupLocator.iterators.NodeIterator;
import com.intellij.dupLocator.iterators.SiblingNodeIterator;
import com.intellij.psi.PsiElement;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.util.containers.BidirectionalMap;
import org.apache.xalan.xsltc.cmdline.Compile;

import java.util.List;
import java.util.stream.Collectors;

public class CompiledReplacement {
    public CompiledPattern searchPattern, replacePattern;
    public TreeContext searchTreeContext, replaceTreeContext;
    public MappingStore mappings;

    public void remapSearchPattern(CompiledPattern newSearchPattern) {
        BidirectionalMap<PsiElement, PsiElement> searchPatternToNew = getCompiledPatternsMapping(searchPattern, newSearchPattern);
        for (ITree node : TreeUtils.preOrder(searchTreeContext.getRoot())) {

            if (node.getType() >= 0) {
                PsiElement oldElement = (PsiElement) node.getMetadata("psi");
                assert oldElement != null;
                PsiElement newElement = searchPatternToNew.get(oldElement);
                assert newElement != null;
                node.setMetadata("psi", newElement);
            }
        }
        searchPattern = newSearchPattern;
    }

    static BidirectionalMap<PsiElement, PsiElement> getCompiledPatternsMapping(CompiledPattern patternA, CompiledPattern patternB) {
        patternA.getNodes().reset();
        patternB.getNodes().reset();
        BidirectionalMap<PsiElement, PsiElement> result = new BidirectionalMap<>();
        new Object() {
            void mapTrees(NodeIterator nodesA, NodeIterator nodesB) {
                while (nodesA.hasNext() || nodesB.hasNext()) {
                    if (nodesA.hasNext() != nodesB.hasNext()) {
                        throw new IllegalArgumentException("Mismatched iterators: " + nodesA.hasNext() + " vs " + nodesB.hasNext());
                    }
                    PsiElement a = nodesA.current(), b = nodesB.current();
                    if (a.getClass() != b.getClass()) {
                        throw new IllegalArgumentException("Mismatched classes: " + a.getClass() + " vs " + b.getClass());
                    }
                    result.put(a, b);
                    mapTrees(new SiblingNodeIterator(a.getFirstChild()), new SiblingNodeIterator(b.getFirstChild()));
                    nodesA.advance();
                    nodesB.advance();
                }
            }
        }.mapTrees(patternA.getNodes(), patternB.getNodes());
        return result;
    }
}
