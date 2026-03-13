package com.demo.mota.engine.map;

import com.demo.mota.engine.enums.Direction;

import java.util.Objects;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

public class Position {
    private short x_index;
    private short y_index;

    public Position(short x_index, short y_index) {
        this.x_index = x_index;
        this.y_index = y_index;
    }

    public short getX_index() {
        return x_index;
    }

    public void setX_index(short x_index) {
        this.x_index = x_index;
    }

    public short getY_index() {
        return y_index;
    }

    public void setY_index(short y_index) {
        this.y_index = y_index;
    }

    public Position getAdjacentPosition(Direction direction) {
        return switch (direction) {
            case UP -> new Position(x_index, y_index == 0 ? 0 : (short) (y_index - 1));
            case DOWN -> new Position(x_index, y_index == MAP_SIDE_LENGTH - 1 ? y_index : (short) (y_index + 1));
            case LEFT -> new Position(x_index == 0 ? 0 : (short) (x_index - 1), y_index);
            case RIGHT -> new Position(x_index == MAP_SIDE_LENGTH - 1 ? x_index : (short) (x_index + 1), y_index);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x_index == position.x_index && y_index == position.y_index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x_index, y_index);
    }

    @Override
    public String toString() {
        return "(" + x_index + ", " + y_index + ")";
    }
}
