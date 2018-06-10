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
import com.intellij.psi.*;
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
import com.intellij.structuralsearch.plugin.replace.ui.ReplaceConfiguration;
import com.intellij.structuralsearch.plugin.replace.ui.ReplaceDialog;
import com.intellij.structuralsearch.plugin.ui.SearchContext;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class MyReplaceDialog extends ReplaceDialog {
    private Map<String, Integer> types = new HashMap<>();

    public MyReplaceDialog(SearchContext searchContext) {
        super(searchContext);
    }

    public MyReplaceDialog(SearchContext searchContext, boolean showScope, boolean runFindActionOnClose) {
        super(searchContext, showScope, runFindActionOnClose);
    }

    @Override
    protected boolean isValid() {
        return super.isValid();
    }

    @Override
    protected void startSearching() {
        final Project project = this.searchContext.getProject();
        MatchOptions matchOptions = myConfiguration.getMatchOptions();
        ReplaceOptions replaceOptions = ((ReplaceConfiguration)myConfiguration).getReplaceOptions();
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

        r.searchTreeContext = new TreeContext();
        ITree searchTree = nodesToTree(r.searchTreeContext, r.searchPattern.getNodes());
        r.searchTreeContext.validate();

        r.replaceTreeContext = new TreeContext();
        ITree replaceTree = nodesToTree(r.replaceTreeContext, r.replacePattern.getNodes());
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
        new MyReplaceCommand(myConfiguration, searchContext, r).startSearching();
    }

    private ITree nodesToTree(TreeContext context, NodeIterator it) {
        ITree root = context.createTree(-1, "my_root", "my_root");
        context.setRoot(root);
        for (it.reset(); it.hasNext(); it.advance()) {
            ITree newChild = elementToTree(context, it.current());
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

    private ITree elementToTree(TreeContext context, PsiElement elem) {
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
        int typeId = types.computeIfAbsent(typeLabel, key -> types.size());

        ITree treeNode = context.createTree(typeId, label, typeLabel);
        treeNode.setMetadata("psi", elem);
        for (PsiElement elemChild = elem.getFirstChild(); elemChild != null; elemChild = elemChild.getNextSibling()) {
            ITree newTreeChild = elementToTree(context, elemChild);
            if (newTreeChild != null) {
                newTreeChild.setParentAndUpdateChildren(treeNode);
            }
        }
        return treeNode;
    }
}
