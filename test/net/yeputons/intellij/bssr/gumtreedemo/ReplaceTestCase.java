package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.Matcher;
import com.intellij.structuralsearch.SyntacticalMatchResult;
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil;
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.util.CollectingMatchResultSink;


public abstract class ReplaceTestCase extends LightQuickFixTestCase {
    protected Matcher matcher;
    protected MatchOptions matchOptions;
    protected ReplaceOptions replaceOptions;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        matcher = new Matcher(getProject());
        matchOptions = new MatchOptions();
        matchOptions.setRecursiveSearch(true);
        replaceOptions = new ReplaceOptions(matchOptions);
   }

    @Override
    protected void tearDown() throws Exception {
        replaceOptions = null;
        matchOptions = null;
        matcher = null;
        super.tearDown();
    }

    protected String replaceAll(String in, String pattern, String with, int expectResults) {
        matchOptions.fillSearchCriteria(pattern);
        final PsiElement[] elements = MatcherImplUtil.createTreeFromText(in, PatternTreeContext.File, matchOptions.getFileType(), getProject());
        LocalSearchScope searchScope = new LocalSearchScope(elements);
        matchOptions.setScope(searchScope);

        replaceOptions.setReplacement(with);
        MyReplacer replacer = new MyReplacer(MyReplacementCompiler.compileReplacement(getProject(), matchOptions, replaceOptions));

        CollectingMatchResultSink sink = new CollectingMatchResultSink();
        matcher.testFindMatches(sink, matchOptions);

        assertEquals(expectResults, sink.getMatches().size());
        for (MatchResult match : sink.getMatches()) {
            SyntacticalMatchResult syntacticalMatch = match.getSyntacticalMatch();
            assertNotNull(syntacticalMatch);
            replacer.performReplacement(syntacticalMatch);
        }

        StringBuilder result = new StringBuilder();
        for (PsiElement el : searchScope.getScope()) {
            result.append(el.getText());
        }
        return result.toString();
    }
}