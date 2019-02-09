package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.structuralsearch.*;
import com.intellij.structuralsearch.plugin.StructuralSearchPlugin;
import com.intellij.structuralsearch.plugin.replace.ui.ReplaceCommand;
import com.intellij.structuralsearch.plugin.ui.Configuration;
import com.intellij.structuralsearch.plugin.ui.SearchCommand;
import com.intellij.structuralsearch.plugin.ui.SearchContext;
import com.intellij.structuralsearch.plugin.ui.UsageViewContext;
import com.intellij.usages.Usage;

public class MyReplaceCommand extends SearchCommand {
    private MyReplaceUsageViewContext myReplaceUsageViewContext;
    private MatchingProcess myProcess;
    private final SyntacticalMatchResultBuilder syntacticalMatchResultBuilder = new SyntacticalMatchResultBuilder();
    CompiledReplacement replacement;

    public MyReplaceCommand(Configuration configuration, SearchContext searchContext, CompiledReplacement replacement) {
        super(configuration, searchContext);
        this.replacement = replacement;
        this.addListener(syntacticalMatchResultBuilder);
    }

    protected UsageViewContext createUsageViewContext() {
        Runnable searchStarter = () -> {
            (new ReplaceCommand(this.myConfiguration, this.mySearchContext)).startSearching();
        };
        this.myReplaceUsageViewContext = new MyReplaceUsageViewContext(this.mySearchContext, this.myConfiguration, searchStarter, replacement, syntacticalMatchResultBuilder.getMatchResults());
        return this.myReplaceUsageViewContext;
    }

    protected void findStarted() {
        super.findStarted();
        StructuralSearchPlugin.getInstance(this.mySearchContext.getProject()).setReplaceInProgress(true);
    }

    protected void findEnded() {
        StructuralSearchPlugin.getInstance(this.mySearchContext.getProject()).setReplaceInProgress(false);
        super.findEnded();
    }

    protected void foundUsage(MatchResult result, Usage usage) {
        super.foundUsage(result, usage);
        this.myReplaceUsageViewContext.addReplaceUsage(usage, result);
    }

    @Override
    public void stopAsyncSearch() {
        if (myProcess != null) myProcess.stop();
    }
}
