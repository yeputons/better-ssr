public class Hierarchy {
    @interface Foo { int[] v(); }
    @interface FooHierarchy { Foo[] in(); int extra(); }
    @interface Bar {}
    @interface Baz {}

    class Base {}
    class Base2 {}

    @Bar
    @Foo(v = {12, 3, 4}, extra = 5)
    @Baz
    class X {
        int y = 0;
    }
}
// =====
@Foo(v = {12, '_a*})
class '_c {}
// =====
@FooHierarchy(
    @Foo(v = {1}),
    @Foo(v = {2}),
    @Foo(v = {$a$}),
)
class $c$ {}
// =====
// 1
// =====
public class Hierarchy {
    @interface Foo { int[] v(); }
    @interface FooHierarchy { Foo[] in(); int extra(); }
    @interface Bar {}
    @interface Baz {}

    class Base {}
    class Base2 {}

    @Bar
    @Baz@FooHierarchy(
    @Foo(v = {1}),
    @Foo(v = {2}),
    @Foo(v = { 3, 4}, extra = 5),
)
    class X {
        int y = 0;
    }
}
