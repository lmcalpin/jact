package com.metatrope.utils;

public class Pair<L, R> {
    final L left;
    final R right;

    private Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static <L,R> Pair<L,R> of(L left, R right) {
        return new Pair<L,R>(left, right);
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
}
