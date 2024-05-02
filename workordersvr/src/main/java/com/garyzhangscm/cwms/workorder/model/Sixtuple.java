package com.garyzhangscm.cwms.workorder.model;


public class Sixtuple<S, T, U, V, W, X> {

    private final S first;
    private final T second;
    private final U third;
    private final V fourth;
    private final W fifth;
    private final X sixth;

    public Sixtuple(S first, T second, U third, V fourth, W fifth, X sixth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
        this.sixth = sixth;
    }

    public S getFirst() { return first; }
    public T getSecond() { return second; }
    public U getThird() { return third; }
    public V getFourth() { return fourth; }
    public W getFifth() { return fifth; }
    public X getSixth() { return sixth; }
}
