class A {
    {
        try {
            foo();
        } catch (Exception1 a) {
            int x;
            String y;
        } catch (Exception2 b) {
            int z1;
            String z2;
        }
    }
}
// =====
try {
    '_s1*;
} catch (Exception2 '_n) {
    '_s2*;
}
// =====
try {
    $s1$;
} catch (Exception2 $n$) {
    System.out.println("hello");
    $s2$;
}
// =====
// 1
// =====
class A {
    {
        try {
            foo();
        } catch (Exception1 a) {
            int x;
            String y;
        } catch (Exception2 b) {
            System.out.println("hello");int z1;
            String z2;
        }
    }
}
