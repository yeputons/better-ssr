package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.Matcher;
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil;
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.util.CollectingMatchResultSink;
import com.intellij.structuralsearch.plugin.util.DuplicateFilteringResultSink;
import org.testng.reporters.Files;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public abstract class ReplaceTestCase extends LightQuickFixTestCase {
    protected Matcher matcher;
    protected SyntacticalMatchResultBuilder builder;
    protected MatchOptions matchOptions;
    protected ReplaceOptions replaceOptions;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        matcher = new Matcher(getProject());
        builder = new SyntacticalMatchResultBuilder();
        matcher.addListener(builder);
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

    protected String readData(String fileName) {
        try {
            return Files.readFile(new File(getClass().getClassLoader().getResource(fileName).toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void runOnTestData(String fileName) {
        String data = readData(fileName);
        String[] parts = data.split("//\\s*={3,}\n");
        assertEquals(5, parts.length);
        String code = parts[0];
        String pattern = parts[1];
        String replacement = parts[2];
        int expectMatches = Integer.parseInt(parts[3].split("//")[1].trim());
        String result = parts[4];
        assertEquals(result, replaceAll(code, pattern, replacement, expectMatches));
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
            SyntacticalMatchResult syntacticalMatch = builder.getMatchResults().get(match);
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