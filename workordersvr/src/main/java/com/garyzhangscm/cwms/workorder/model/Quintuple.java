package com.garyzhangscm.cwms.workorder.model;


public class Quintuple<S, T, U, V, W> {

    private final S first;
    private final T second;
    private final U third;
    private final V fourth;
    private final W fifth;

    public Quintuple(S first, T second, U third, V fourth, W fifth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
    }

    public S getFirst() { return first; }
    public T getSecond() { return second; }
    public U getThird() { return third; }
    public V getFourth() { return fourth; }
    public W getFifth() { return fifth; }
}
