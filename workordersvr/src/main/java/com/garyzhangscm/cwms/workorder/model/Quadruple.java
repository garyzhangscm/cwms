package com.garyzhangscm.cwms.workorder.model;


public class Quadruple<S, T, U, V> {

    private final S first;
    private final T second;
    private final U third;
    private final V fourth;

    public Quadruple(S first, T second, U third, V fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public S getFirst() { return first; }
    public T getSecond() { return second; }
    public U getThird() { return third; }
    public V getFourth() { return fourth; }
}
