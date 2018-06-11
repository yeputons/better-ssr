public class FooBar {
    @interface Foo { Class expect(); }
    @interface Bar {}

    void expectClass(Class e, Runnable r) {}

    @Foo(expect = NullPointerException.class)
    @Bar
    void foo() {
        int z = 123;
        int x;
    }

    @Bar
    @Foo(expect = IOException.class)
    void bar() {
        String s1 = "";
        String s2 = "";
    }

    @Foo(expect = IllegalStateException.class)
    void baz() {
    }
}
// =====
class '_1 {
    @Foo(expect = '_e)
    '_rt 'm+ ('_pt '_p*) {
        '_s*;
    }
}
// =====
class $1$ {
    $rt$ $m$ ($pt$ $p$) {
        expectClass($e$, () -> {
            $s$;
        });
    }
}
// =====
// 3
// =====
public class FooBar {
    @interface Foo { Class expect(); }
    @interface Bar {}

    void expectClass(Class e, Runnable r) {}

    @Bar
    void foo(){expectClass(NullPointerException.class, () -> {
        int z = 123;
        int x;
    });};

    @Bar
    void bar(){expectClass(IOException.class, () -> {
        String s1 = "";
        String s2 = "";
    });};

    void baz(){expectClass(IllegalStateException.class, () -> {
    });};
}
