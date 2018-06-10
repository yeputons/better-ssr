@SuppressWarnings({"foo", "bar"})
public final class Foo {
    {
        System.out.println("x");
    }

    int x;

    @interface Foo { int x() default 10; int y() default 10; }

    class Bar {
    }

    @Foo(x = 2)
    String y(@Foo String z) {
        return z;
    }

    @Foo
    void z(@Foo int a) {
        int b = a + a;
    }
}
// =====
class '1 {
    '_rt '_m*('_pt '_p*);
}
// =====
@Deprecated
class $1$ {
    @Deprecated
    $rt$ $m$($pt$ $p$);
}
// =====
// 3
// =====
@SuppressWarnings({"foo", "bar"})
public final@Deprecated class Foo {
    {
        System.out.println("x");
    }

    int x;

    @Deprecated@interface Foo { @Deprecated int x() default 10; @Deprecated int y() default 10; }

    @Deprecated class Bar {
    }

    @Foo(x = 2)@Deprecated
    String y(@Foo String z) {
        return z;
    }

    @Foo@Deprecated
    void z(@Foo int a) {
        int b = a + a;
    }
}
