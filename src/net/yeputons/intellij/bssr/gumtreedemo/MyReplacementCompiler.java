package net.yeputons.intellij.bssr.gumtreedemo;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.intellij.dupLocator.iterators.NodeIterator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.StructuralSearchProfile;
import com.intellij.structuralsearch.StructuralSearchUtil;
import com.intellij.structuralsearch.impl.matcher.compiler.PatternCompiler;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class MyReplacementCompiler {
    @NotNull
    static CompiledReplacement compileReplacement(Project project, MatchOptions matchOptions, ReplaceOptions replaceOptions) {
        StructuralSearchProfile profile = StructuralSearchUtil.getProfileByFileType(matchOptions.getFileType());
        assert profile != null;

        CompiledReplacement r = new CompiledReplacement();

        r.searchPattern = PatternCompiler.compilePattern(project, matchOptions, false);
        profile.checkSearchPattern(r.searchPattern);

        {
            String oldPattern = matchOptions.getSearchPattern();
            SearchScope oldScope = matchOptions.getScope();
            matchOptions.setSearchPattern(replaceOptions.getReplacement());
            matchOptions.setScope(new LocalSearchScope(new PsiElement[0]));
            r.replacePattern = PatternCompiler.compilePattern(project, matchOptions, false);
            matchOptions.setSearchPattern(oldPattern);
            matchOptions.setScope(oldScope);
        }

        Map<String, Integer> typesMapping = new HashMap<>();
        r.searchTreeContext = new TreeContext();
        ITree searchTree = nodesToTree(r.searchTreeContext, r.searchPattern.getNodes(), typesMapping);
        r.searchTreeContext.validate();

        r.replaceTreeContext = new TreeContext();
        ITree replaceTree = nodesToTree(r.replaceTreeContext, r.replacePattern.getNodes(), typesMapping);
        r.replaceTreeContext.validate();

        r.mappings = new MappingStore();
        r.searchTreeContext.importTypeLabels(r.replaceTreeContext);
        r.replaceTreeContext.importTypeLabels(r.searchTreeContext);
        Matcher m = new CompositeMatchers.ClassicGumtree(searchTree, replaceTree, r.mappings);
        m.match();
        {
            ActionGenerator g = new ActionGenerator(searchTree,
                    replaceTree, m.getMappings());
            g.generate();
            StringWriter wr = new StringWriter();
            try {
                ActionsIoUtils.toText(r.searchTreeContext, g.getActions(), m.getMappings()).writeTo(wr);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(wr);
        }
        return r;
    }

    private static ITree nodesToTree(TreeContext context, NodeIterator it, Map<String, Integer> typesMapping) {
        ITree root = context.createTree(-1, "my_root", "my_root");
        context.setRoot(root);
        for (it.reset(); it.hasNext(); it.advance()) {
            ITree newChild = elementToTree(context, it.current(), typesMapping);
            if (newChild != null) {
                newChild.setParentAndUpdateChildren(root);
            }
        }
        it.reset();
        return context.getRoot();
    }

    public static boolean ignorePsiElement(PsiElement elem) {
        if (elem instanceof PsiWhiteSpace) return true;
        if (elem instanceof PsiJavaToken) {
            if (elem.getText().equals("(")) return true;
            if (elem.getText().equals(")")) return true;
            if (elem.getText().equals("{")) return true;
            if (elem.getText().equals("}")) return true;
            if (elem.getText().equals(",")) return true;
            if (elem.getText().equals("throws")) return true;
        }
        if (elem instanceof XmlText && elem.getText().trim().isEmpty()) {
            return true;
        }
        return false;
//        return elem instanceof PsiWhiteSpace || elem.getClass().equals(PsiJavaTokenImpl.class) ||
//                (elem instanceof LeafPsiElement && elem.getText().equals(","));
    }

    private static ITree elementToTree(TreeContext context, PsiElement elem, Map<String, Integer> typesMapping) {
        if (ignorePsiElement(elem)) return null;
        String typeLabel;
        typeLabel = elem.getClass().getCanonicalName() + ";" + elem.getNode().getElementType().getIndex();
        if (elem instanceof PsiReferenceList) {
            typeLabel += ";" + ((PsiReferenceList) elem).getRole().name();
        }
        String label = typeLabel + ":";
        if (elem instanceof PsiJavaToken || elem instanceof LeafPsiElement) {
            assert elem.getFirstChild() == null;
            label = elem.toString();
        }
        if (elem instanceof XmlToken || elem instanceof XmlText) {
            label = elem.getText().trim();
        }
        int typeId = typesMapping.computeIfAbsent(typeLabel, key -> typesMapping.size());

        ITree treeNode = context.createTree(typeId, label, typeLabel);
        treeNode.setMetadata("psi", elem);
        for (PsiElement elemChild = elem.getFirstChild(); elemChild != null; elemChild = elemChild.getNextSibling()) {
            ITree newTreeChild = elementToTree(context, elemChild, typesMapping);
            if (newTreeChild != null) {
                newTreeChild.setParentAndUpdateChildren(treeNode);
            }
        }
        return treeNode;
    }
}
