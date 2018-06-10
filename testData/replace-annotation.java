public class MyClass {
    @Foo
    void m1a() {}

    @Foo(x = 10)
    void m1b() {}

    @Foo(x = 10, y = 20)
    void m1c() {}

    @Bar
    void m2a()

    void m2b()
}
// =====
class '_1 {
    @Foo
    '_rt 'm*('_pt '_p*);
}
// =====
class $1$ {
    @Bar
    $rt$ $m$($pt$ $p$);
}
// =====
// 3
// =====
public class MyClass {
    @Bar
    void m1a() {}

    @Bar(x = 10)
    void m1b() {}

    @Bar(x = 10, y = 20)
    void m1c() {}

    @Bar
    void m2a()

    void m2b()
}
