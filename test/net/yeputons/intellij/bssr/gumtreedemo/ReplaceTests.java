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

    public void testUpdateCatch() {
        runOnTestData("update-catch.java");
    }

    public void testAnnotationsHierarchy() {
        runOnTestData("annotations-hierarchy.java");
    }

    public void testRemoveThrows() {
        runOnTestData("remove-throws.java");
    }

    public void testWrapMethodBody() {
        runOnTestData("wrap-method-body.java");
    }

    public void testAddBeforeSequences() {
        runOnTestData("add-before-sequences.java");
    }

    public void testMoveSequences() {
        runOnTestData("move-sequences.java");
    }
}
