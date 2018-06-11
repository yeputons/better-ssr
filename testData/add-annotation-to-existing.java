public class MyClass {
    @interface Foo { int x(); }
    @interface Baz { int z(); }
    @interface Bar { int y(); }

    @Foo(x = 10)
    @Baz(z = 10)
    void m1a() {}

    @Baz(z = 10)
    @Foo(x = 10)
    void m1b() {}

    @Foo(x = 10)
    void m2() {}

    @Baz(z = 10)
    void m3() {}
}
// =====
class '_1 {
        @Foo
        @Baz
        '_rt 'm+('_pt '_p*);
}
// =====
@Deprecated
class $1$ {
    @Foo
    @Bar(y = 3)
    @Baz
    $rt$ $m$($pt$ $p$);
}
// =====
// 2
// =====
public class MyClass {
    @interface Foo { int x(); }
    @interface Baz { int z(); }
    @interface Bar { int y(); }

    @Foo(x = 10)@Bar(y = 3)
    @Baz(z = 10)
    void m1a() {}

    @Baz(z = 10)
    @Foo(x = 10)@Bar(y = 3)
    void m1b() {}

    @Foo(x = 10)
    void m2() {}

    @Baz(z = 10)
    void m3() {}
}
