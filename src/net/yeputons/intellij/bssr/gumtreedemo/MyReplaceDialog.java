package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.openapi.project.Project;
import com.intellij.structuralsearch.MatchOptions;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.replace.ui.ReplaceConfiguration;
import com.intellij.structuralsearch.plugin.replace.ui.ReplaceDialog;
import com.intellij.structuralsearch.plugin.ui.SearchContext;

public class MyReplaceDialog extends ReplaceDialog {
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
        CompiledReplacement r = MyReplacementCompiler.compileReplacement(project, matchOptions, replaceOptions);
        new MyReplaceCommand(myConfiguration, searchContext, r).startSearching();
    }

}
