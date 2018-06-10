package net.yeputons.intellij.bssr.gumtreedemo;

public class ReplaceTests extends ReplaceTestCase {
    public void testIdentity() {
        runOnTestData("identity.java");
    }

    public void testAddAnnotation() {
        runOnTestData("add-annotation.java");
    }

    public void testAddAnnotationToExisting() {
        runOnTestData("add-annotation-to-existing.java");
    }

    public void testReplaceAnnotation() {
        runOnTestData("replace-annotation.java");
    }
}
