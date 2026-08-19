package com.demo.mota.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigInteger;

public final class GameNumber implements Comparable<GameNumber> {
    public static final GameNumber ZERO = new GameNumber(BigInteger.ZERO);

    private final BigInteger value;

    private GameNumber(BigInteger value) {
        this.value = value;
    }

    @JsonCreator
    public static GameNumber of(long v) {
        return v == 0 ? ZERO : new GameNumber(BigInteger.valueOf(v));
    }

    public static GameNumber of(int v) {
        return v == 0 ? ZERO : new GameNumber(BigInteger.valueOf(v));
    }

    public static GameNumber fromBigInteger(BigInteger v) {
        return BigInteger.ZERO.equals(v) ? ZERO : new GameNumber(v);
    }

    public GameNumber plus(GameNumber other) {
        return new GameNumber(value.add(other.value));
    }

    public GameNumber minus(GameNumber other) {
        return new GameNumber(value.subtract(other.value));
    }

    public GameNumber times(GameNumber other) {
        return new GameNumber(value.multiply(other.value));
    }

    public GameNumber dividedBy(GameNumber other, boolean floor) {
        if (floor) {
            BigInteger res = value.divide(other.value);
            return new GameNumber(res);
        }else {
            double res = value.doubleValue() / other.value.doubleValue();
            return new GameNumber(BigInteger.valueOf((long) Math.ceil(res)));
        }
    }

    public double dividedBy(GameNumber other) {
        return value.doubleValue() / other.value.doubleValue();
    }
    public GameNumber clampMin(GameNumber min) {
        return this.compareTo(min) < 0 ? min : this;
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public boolean isNonPositive() {
        return value.signum() <= 0;
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    @JsonValue
    public long toLong() {
        return value.longValue();
    }

    public long toLongExact() {
        return value.longValueExact();
    }

    @Override
    public int compareTo(GameNumber other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameNumber other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
