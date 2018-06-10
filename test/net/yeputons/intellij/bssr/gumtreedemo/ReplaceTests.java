package net.yeputons.intellij.bssr.gumtreedemo;

public class ReplaceTests extends ReplaceTestCase {
    public void testReplaceIdentity() {
        String s = "class A { int x; void foo() {} void bar() {}}";
        assertEquals(s, replaceAll(s, "class '_1 { void 'x+(); }", "class $1$ { void $x$(); }", 2));
    }
}
