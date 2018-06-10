package net.yeputons.intellij.bssr.gumtreedemo;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import com.google.common.collect.Lists;
import com.intellij.dupLocator.iterators.NodeIterator;
import com.intellij.dupLocator.iterators.SiblingNodeIterator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.structuralsearch.SyntacticalMatchResult;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer;
import com.intellij.util.containers.BidirectionalMap;
import com.intellij.util.containers.MultiMap;

import java.util.*;
import java.util.function.Function;

public class MyReplacer {
    private final CompiledReplacement replacement;

    public MyReplacer(CompiledReplacement replacement) {
        this.replacement = replacement;
    }

    public void performReplacement(SyntacticalMatchResult syntacticalMatchPattern) {
        BidirectionalMap<PsiElement, PsiElement> oldSearchPatternToNew =
                getCompiledPatternsMapping(replacement.searchPattern, syntacticalMatchPattern.getCompiledPattern());

        BidirectionalMap<PsiElement, ITree> oldSearchToItree = collectPsiElementsMapping(replacement.searchTreeContext.getRoot());
        BidirectionalMap<PsiElement, ITree> replaceToItree = collectPsiElementsMapping(replacement.replaceTreeContext.getRoot());

        Map<PsiElement, List<SmartPsiPointer>> newSearchToMatch = collectSyntacticalMatches(syntacticalMatchPattern);

        // Try to extend matches.
        for (;;) {
            Set<PsiElement> matchedElements = new HashSet<>();
            for (Map.Entry<PsiElement, List<SmartPsiPointer>> entry : newSearchToMatch.entrySet()) {
                for (SmartPsiPointer element : entry.getValue()) {
                    matchedElements.add(element.getElement());
                }
            }

            Map<PsiElement, List<SmartPsiPointer>> extraNewSearchToMatch = new HashMap<>();
            for (Map.Entry<PsiElement, List<SmartPsiPointer>> reliableMatch : newSearchToMatch.entrySet()) {
                Map<Class, List<PsiElement>> searchChildProfile = new HashMap<>();
                for (PsiElement child : reliableMatch.getKey().getChildren()) {
                    if (!newSearchToMatch.containsKey(child)) {
                        searchChildProfile.computeIfAbsent(child.getClass(), x -> new ArrayList<>()).add(child);
                    }
                }

                Set<Class> reliableClasses = searchChildProfile.keySet();
                for (SmartPsiPointer smartNode : reliableMatch.getValue()) {
                    Map<Class, List<PsiElement>> currentProfile = new HashMap<>();
                    for (PsiElement child : smartNode.getElement().getChildren()) {
                        if (!matchedElements.contains(child)) {
                            currentProfile.computeIfAbsent(child.getClass(), x -> new ArrayList<>()).add(child);
                        }
                    }
                    searchChildProfile.entrySet().removeIf(entry ->
                            !currentProfile.containsKey(entry.getKey()) ||
                                    currentProfile.get(entry.getKey()).size() != entry.getValue().size()
                    );
                }
                if (reliableClasses.isEmpty()) continue;

                for (SmartPsiPointer smartNode : reliableMatch.getValue()) {
                    Map<Class, List<PsiElement>> currentProfile = new HashMap<>();
                    for (PsiElement child : smartNode.getElement().getChildren()) {
                        if (reliableClasses.contains(child.getClass())) {
                            if (!matchedElements.contains(child)) {
                                currentProfile.computeIfAbsent(child.getClass(), x -> new ArrayList<>()).add(child);
                            }
                        }
                    }
                    for (Map.Entry<Class, List<PsiElement>> entry : currentProfile.entrySet()) {
                        List<PsiElement> searchChildren = searchChildProfile.get(entry.getKey());
                        List<PsiElement> currentChildren = entry.getValue();
                        assert searchChildren.size() == currentChildren.size();
                        for (int i = 0; i < searchChildren.size(); i++) {
                            extraNewSearchToMatch.computeIfAbsent(searchChildren.get(i), x -> new ArrayList<>()).add(new SmartPsiPointer(currentChildren.get(i)));
                        }
                    }
                }
            }
            if (extraNewSearchToMatch.isEmpty()) {
                break;
            }
            for (Map.Entry<PsiElement, List<SmartPsiPointer>> entry : extraNewSearchToMatch.entrySet()) {
                assert !newSearchToMatch.containsKey(entry.getKey());
                newSearchToMatch.put(entry.getKey(), entry.getValue());
            }
        }

        MultiMap<PsiElement, PsiElement> matchToNewSearch = new MultiMap<>();
        for (Map.Entry<PsiElement, List<SmartPsiPointer>> entry : newSearchToMatch.entrySet()) {
            for (SmartPsiPointer element : entry.getValue()) {
                if (matchToNewSearch.containsKey(element.getElement())) {
                    //assert matchToNewSearch.get(element.getElement()) == entry.getKey();
                }
                matchToNewSearch.putValue(element.getElement(), entry.getKey());
            }
        }

        // Find and perform the actual edit scenario.
        Map<ITree, List<SmartPsiPointer>> addedElements = new HashMap<>();
        MultiMap<PsiElement, ITree> addedElementToITree = new MultiMap<>();
        Function<ITree, List<SmartPsiPointer>> findReplaceOccurrences = replaceItree -> {
            if (addedElements.containsKey(replaceItree)) {
                return addedElements.get(replaceItree);
            }
            ITree searchItree = replacement.mappings.getSrc(replaceItree);
            if (searchItree != null) {
                PsiElement newSearchNode = oldSearchPatternToNew.get(oldSearchToItree.getKeysByValue(searchItree).get(0));
                assert newSearchNode != null;
                if (newSearchToMatch.containsKey(newSearchNode)) {
                    return newSearchToMatch.get(newSearchNode);
                }
            }
            return null;
        };

        new Object() {
            public void run() {
                addSubtrees(rescanIterator(replacement.replacePattern.getNodes()));
                moveSubtrees(rescanIterator(replacement.replacePattern.getNodes()));
                removeSubtrees(rescanIterator(replacement.searchPattern.getNodes()));
            }

            private void addSubtrees(List<PsiElement> replaceNodes) {
                for (PsiElement node : replaceNodes) {
                    addSubtrees(node);
                }
            }

            private void moveSubtrees(List<PsiElement> replaceNodes) {
                for (PsiElement node : replaceNodes) {
                    moveSubtrees(node);
                }
            }

            private void removeSubtrees(List<PsiElement> oldSearchNodes) {
                for (PsiElement oldSearchNode : oldSearchNodes) {
                    removeSubtrees(oldSearchNode);
                }
            }

            private void addSubtrees(PsiElement replaceNode) {
                if (MyReplacementCompiler.ignorePsiElement(replaceNode)) return;
                ITree replaceItree = replaceToItree.get(replaceNode);
                assert replaceItree != null;
                if (!replacement.mappings.hasDst(replaceItree)) {
                    // Add subtree
                    List<ITree> siblings = replaceItree.getParent().getChildren();
                    int replacePosition = replaceItree.getParent().getChildPosition(replaceItree);

                    List<SmartPsiPointer> addAfter = null, addBefore = null;
                    int addAfterDist = Integer.MAX_VALUE, addBeforeDist = Integer.MAX_VALUE;
                    for (int i = replacePosition - 1; i >= 0; i--) {
                        List<SmartPsiPointer> occurrences = findReplaceOccurrences.apply(siblings.get(i));
                        if (occurrences != null) {
                            addAfter = occurrences;
                            addAfterDist = replacePosition - i;
                            break;
                        }
                    }
                    for (int i = replacePosition + 1; i < siblings.size(); i++) {
                        List<SmartPsiPointer> occurrences = findReplaceOccurrences.apply(siblings.get(i));
                        if (occurrences != null) {
                            addBefore = occurrences;
                            addBeforeDist = i - replacePosition;
                            break;
                        }
                    }
                    if (addBeforeDist < addAfterDist) {
                        addAfter = null;
                        addAfterDist = Integer.MAX_VALUE;
                    }
                    if (addAfter != null) {
                        Set<PsiElement> addedToParent = new HashSet<>();
                        for (SmartPsiPointer addAfterSmartNode : Lists.reverse(addAfter)) {
                            PsiElement addAfterNode = addAfterSmartNode.getElement();
                            if (!addedToParent.add(addAfterNode.getParent())) {
                                continue;
                            }
                            PsiElement added = addAfterNode.getParent().addAfter(replaceNode.copy(), addAfterNode);
                            addElements(replaceItree, added);
                        }
                    } else if (addBefore != null) {
                        Set<PsiElement> addedToParent = new HashSet<>();
                        for (SmartPsiPointer addBeforeSmartNode : addBefore) {
                            PsiElement addBeforeNode = addBeforeSmartNode.getElement();
                            if (!addedToParent.add(addBeforeNode.getParent())) {
                                continue;
                            }
                            PsiElement added = addBeforeNode.getParent().addBefore(replaceNode.copy(), addBeforeNode);
                            addElements(replaceItree, added);
                        }
                    } else {
                        ITree searchParentItree = replacement.mappings.getSrc(replaceItree.getParent());
                        assert searchParentItree  != null;

                        PsiElement newSearchParentNode = oldSearchPatternToNew.get(oldSearchToItree.getKeysByValue(searchParentItree).get(0));
                        assert newSearchParentNode != null;
                        List<SmartPsiPointer> parents = newSearchToMatch.get(newSearchParentNode);
                        if (parents != null) {
                            for (SmartPsiPointer parent : parents) {
                                PsiElement added = parent.getElement().add(replaceNode.copy());
                                addElements(replaceItree, added);
                            }
                        }
                    }
                    // Subtree is only added as a whole
                } else {
                    addSubtrees(Arrays.asList(replaceNode.getChildren()));
                }
            }

            private void addElements(ITree replaceItree, PsiElement added) {
                addedElements.computeIfAbsent(replaceItree, x -> new ArrayList<>()).add(new SmartPsiPointer(added));
                addedElementToITree.putValue(added, replaceItree);
                List<PsiElement> children = new ArrayList<>();
                for (PsiElement child = added.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (MyReplacementCompiler.ignorePsiElement(child)) continue;
                    children.add(child);
                }
                assert replaceItree.getChildren().size() == children.size();
                for (int i = 0; i < children.size(); i++) {
                    addElements(replaceItree.getChild(i), children.get(i));
                }
            }

            private void moveSubtrees(PsiElement replaceNode) {
                if (MyReplacementCompiler.ignorePsiElement(replaceNode)) return;
                ITree replaceItree = replaceToItree.get(replaceNode);
                assert replaceItree != null;
                if (replacement.mappings.hasDst(replaceItree)) {
                    ITree searchItree = replacement.mappings.getSrc(replaceItree);
                    if (!replaceItree.getLabel().equals(searchItree.getLabel())) {
                        PsiElement newSearchNode = oldSearchPatternToNew.get((PsiElement) searchItree.getMetadata("psi"));
                        for (SmartPsiPointer smartNode : newSearchToMatch.get(newSearchNode)) {
                            PsiElement oldNode = replaceNode;
                            PsiElement newNode = smartNode.getElement().replace(replaceNode);
                            if (oldNode != newNode) {
                                renameNodeAndDescendants(oldNode, newNode);
                            }
                        }
                    } else if (replacement.mappings.getDst(searchItree.getParent()) != replaceItree.getParent()) {
                        PsiElement newSearchNode = oldSearchPatternToNew.get((PsiElement)searchItree.getMetadata("psi"));
                        List<SmartPsiPointer> toMove = newSearchToMatch.get(newSearchNode);
                        List<SmartPsiPointer> atPlace = addedElements.get(replaceItree);
                        assert atPlace != null;
                        for (SmartPsiPointer smartNewNode : atPlace) {
                            PsiElement newNode = smartNewNode.getElement();
                            PsiElement matchRoot = newNode;
                            while (true) {
                                PsiElement newMatchRoot = matchRoot.getParent();
                                if (newMatchRoot == null) break;
                                long cnt = atPlace.stream().filter(x -> PsiTreeUtil.isAncestor(newMatchRoot, x.getElement(), false)).count();
                                assert cnt >= 1;
                                if (cnt > 1) break;
                                matchRoot = newMatchRoot;
                            }
                            List<PsiElement> oldNodes = new ArrayList<>();
                            for (Iterator<SmartPsiPointer> it = toMove.iterator(); it.hasNext();) {
                                PsiElement oldNode = it.next().getElement();
                                if (PsiTreeUtil.isAncestor(matchRoot, oldNode, false)) {
                                    oldNodes.add(oldNode);
                                    it.remove();
                                }
                            }
                            if (oldNodes.size() == 0) {
                                newNode.delete();
                            } else {
                                PsiElement lastAdded = newNode;
                                lastAdded = newNode.replace(oldNodes.get(0));
                                for (int i = 1; i < oldNodes.size(); i++) {
                                    lastAdded = lastAdded .getParent().addAfter(oldNodes.get(i).copy(), lastAdded);
                                }
                            }
                            for (PsiElement oldNode : oldNodes) {
                                if (oldNode.getParent() != null) {
                                    oldNode.delete();
                                }
                            }
                        }
                    }
                }
                moveSubtrees(Arrays.asList(replaceNode.getChildren()));
            }

            private void renameNodeAndDescendants(PsiElement oldMatch, PsiElement newMatch) {
                if (matchToNewSearch.containsKey(oldMatch)) {
                    for (PsiElement newSearch : matchToNewSearch.get(oldMatch)) {
                        matchToNewSearch.putValue(newMatch, newSearch);
                        List<SmartPsiPointer> list = newSearchToMatch.get(newSearch);
                        assert list != null;
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getElement() == oldMatch) {
                                list.set(i, new SmartPsiPointer(newMatch));
                            }
                        }
                    }
                    matchToNewSearch.remove(oldMatch);
                }
                if (addedElementToITree.containsKey(oldMatch)) {
                    for (ITree tree : addedElementToITree.get(oldMatch)) {
                        addedElementToITree.putValue(newMatch, tree);
                        List<SmartPsiPointer> list = addedElements.get(tree);
                        assert list != null;
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getElement() == oldMatch) {
                                list.set(i, new SmartPsiPointer(newMatch));
                            }
                        }
                    }
                    addedElementToITree.remove(oldMatch);
                }
                PsiElement[] oldChildren = oldMatch.getChildren();
                PsiElement[] newChildren = newMatch.getChildren();
                assert oldChildren.length == newChildren.length;
                for (int i = 0; i < oldChildren.length; i++) {
                    renameNodeAndDescendants(oldChildren[i], newChildren[i]);
                }
            }

            private void removeSubtrees(PsiElement oldSearchNode) {
                if (MyReplacementCompiler.ignorePsiElement(oldSearchNode)) return;
                PsiElement newSearchNode = oldSearchPatternToNew.get(oldSearchNode);
                assert newSearchNode != null;
                ITree searchItree = oldSearchToItree.get(oldSearchNode);
                assert searchItree != null;
                if (!replacement.mappings.hasSrc(searchItree)) {
                    // Remove subtree
                    List<SmartPsiPointer> occurrences = newSearchToMatch.get(newSearchNode);
                    if (occurrences != null) {
                        for (SmartPsiPointer smartNode : occurrences) {
                            PsiElement node = smartNode.getElement();
                            if (node != null) {
                                node.delete();
                            }
                        }
                    }
                    // If we're unable to delete whole subtree, it's worth
                    // trying to delete some of its subtrees
                }
                removeSubtrees(Arrays.asList(oldSearchNode.getChildren()));
            }
        }.run();
    }

    private static List<PsiElement> rescanIterator(NodeIterator iter) {
        iter.reset();
        ArrayList<PsiElement> result = new ArrayList<>();
        for (; iter.hasNext(); iter.advance()) {
            result.add(iter.current());
        }
        return result;
    }

    private static Map<PsiElement, List<SmartPsiPointer>> collectSyntacticalMatches(SyntacticalMatchResult root) {
        Map<PsiElement, List<SmartPsiPointer>> result = new HashMap<>();
        new Object() {
            public void go(SyntacticalMatchResult node) {
                if (node.getPatternElement() != null) {
                    List<SmartPsiPointer> list = result.computeIfAbsent(node.getPatternElement(), x -> new ArrayList<>());
                    if (!list.stream().anyMatch(x -> x.getElement() == node.getMatchedElement().getElement())) {
                        list.add(node.getMatchedElement());
                    }
                }
                for (SyntacticalMatchResult child : node.getChildren()) {
                    go(child);
                }
            }
        }.go(root);
        return result;
    }

    private static BidirectionalMap<PsiElement, PsiElement> getCompiledPatternsMapping(CompiledPattern patternA, CompiledPattern patternB) {
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

    private static BidirectionalMap<PsiElement, ITree> collectPsiElementsMapping(ITree root) {
        BidirectionalMap<PsiElement, ITree> result = new BidirectionalMap<>();
        TreeUtils.preOrderIterator(root).forEachRemaining(node ->
                {
                    if (node.getType() >= 0) {
                        result.put((PsiElement) node.getMetadata("psi"), node);
                    }
                }
        );
        return result;
    }
}
