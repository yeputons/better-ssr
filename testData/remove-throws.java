public class FooBar {
    class Foo extends Exception {}
    class ShouldBeRemoved extends Exception {}

    void foo1() {}
    void foo2() throws ShouldBeRemoved  {}
    void foo3() throws Foo, ShouldBeRemoved {}
    void foo4() throws ShouldBeRemoved, Foo {}
}
// =====
class '_1 {
    '_rt 'm+('_pt '_p*) throws ShouldBeRemoved;
}
// =====
class $1$ {
    $rt$ $m$($pt$ $p$);
}
// =====
// 3
// =====
public class FooBar {
    class Foo extends Exception {}
    class ShouldBeRemoved extends Exception {}

    void foo1() {}
    void foo2()   {}
    void foo3() throws Foo {}
    void foo4() throws  Foo {}
}
