package org.loklak.data;

/**
 * Created by hadoop on 16-8-4.
 */
public class TwoTuple<A, B> {
    public final A first;
    public final B second;

    public TwoTuple(A a, B b) {
        this.first = a;
        this.second = b;
    }
}
