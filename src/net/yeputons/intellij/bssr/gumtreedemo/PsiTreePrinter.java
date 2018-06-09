package net.yeputons.intellij.bssr.gumtreedemo;

import com.intellij.psi.PsiElement;

import java.util.List;

public class PsiTreePrinter {
    private final StringBuilder sb = new StringBuilder();
    private int offset = 0;

    @Override
    public String toString() {
        return sb.toString();
    }

    public PsiTreePrinter print(List<PsiElement> nodes) {
        printOffset();
        sb.append("[nodes");
        offset += 2;
        boolean hasChild = false;
        for (PsiElement node : nodes) {
            sb.append("\n");
            hasChild = true;
            print(node);
        }
        offset -= 2;
        if (hasChild) {
            sb.append("\n");
            printOffset();
        }
        sb.append("]");
        return this;
    }

    public PsiTreePrinter print(PsiElement e) {
        printOffset();
        sb.append("[\\verb~" + e.getClass().getCanonicalName() + "!" + e.toString() + "~");
        offset += 2;
        boolean hasChild = false;
        for (PsiElement child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (MyReplaceDialog.ignorePsiElement(child)) continue;
            sb.append("\n");
            hasChild = true;
            print(child);
        }
        offset -= 2;
        if (hasChild) {
            sb.append("\n");
            printOffset();
        }
        sb.append("]");
        return this;
    }

    protected void printOffset() {
        for (int i = 0; i < offset; i++)
            sb.append(' ');
    }
}
