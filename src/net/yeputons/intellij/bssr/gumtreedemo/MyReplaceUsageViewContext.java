package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.structuralsearch.MatchResult;
import com.intellij.structuralsearch.SSRBundle;
import com.intellij.structuralsearch.SyntacticalMatchResult;
import com.intellij.structuralsearch.impl.matcher.MatchResultImpl;
import com.intellij.structuralsearch.plugin.ui.Configuration;
import com.intellij.structuralsearch.plugin.ui.SearchContext;
import com.intellij.structuralsearch.plugin.ui.UsageViewContext;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MyReplaceUsageViewContext extends UsageViewContext {
    private final HashMap<Usage, MatchResult> usage2MatchResult = new HashMap<>();
    private UsageView myUsageView;
    private CompiledReplacement replacement;

    protected MyReplaceUsageViewContext(SearchContext searchContext, Configuration configuration, Runnable searchStarter, CompiledReplacement replacement) {
        super(configuration, searchContext, searchStarter);
        this.replacement = replacement;
    }

    @Override
    public void setUsageView(UsageView usageView) {
        this.myUsageView = usageView;
    }

    public void addReplaceUsage(Usage usage, MatchResult result) {
        this.usage2MatchResult.put(usage, result);
    }

    @Override
    protected void configureActions() {
        this.myUsageView.addButtonToLowerPane(() -> {
            this.replace(this.myUsageView.getSortedUsages());
        }, SSRBundle.message("do.replace.all.button", new Object[0]));
        this.myUsageView.addButtonToLowerPane(() -> {
            this.replace(new ArrayList<>(this.myUsageView.getSelectedUsages()));
        }, SSRBundle.message("replace.selected.button", new Object[0]));
    }

    private void replace(List<Usage> selectedUsages) {
        final LocalHistoryAction localHistoryAction = LocalHistory.getInstance().startAction("Gum Tree Structural Replace");
        MyReplacer replacer = new MyReplacer(replacement);
        try {
            WriteCommandAction.runWriteCommandAction(mySearchContext.getProject(), () -> {
                for (Usage usage : selectedUsages) {
                    MatchResultImpl matchResult = (MatchResultImpl) usage2MatchResult.get(usage);
                    @NotNull SyntacticalMatchResult syntacticalMatchPattern = matchResult.getSyntacticalMatch();
                    replacer.performReplacement(syntacticalMatchPattern);
                }
            });
        } finally {
            localHistoryAction.finish();
        }
    }

}
