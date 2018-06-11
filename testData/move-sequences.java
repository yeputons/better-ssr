public class FooBar {
    void foo() {
        System.out.println("a");
        System.out.println("b");
        System.out.println("c");
    }

    void bar(char c, int x) {
        System.out.println("a");
        System.out.println("d");
    }

    void bar(String z) {
        System.out.println("a");
    }
}
// =====
class '_1 {
    '_rt 'm+('_pt '_p*) {
        System.out.println("a");
        '_s*;
    }
}
// =====
class $1$ {
    $rt$ $m$ ($pt$ $p$) {
        System.out.println("a");
        if (false) {
            $s$;
        }
    }
}
// =====
// 3
// =====
public class FooBar {
    void foo() {
        System.out.println("a");if (false) {System.out.println("b");System.out.println("c");}
        }

    void bar(char c, int x) {
        System.out.println("a");if (false) {System.out.println("d");}
        }

    void bar(String z) {
        System.out.println("a");if (false) {}
    }
}