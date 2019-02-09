package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.structuralsearch.plugin.ui.SearchContext;

public class DemoAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        final SearchContext searchContext = new SearchContext(event.getDataContext());
        final Project project = searchContext.getProject();
        if (project == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        MyReplaceDialog replaceDialog = new MyReplaceDialog(searchContext);
        replaceDialog.show();
    }
}
