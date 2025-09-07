package com.lahuca.lane.data.ordered;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author _Neko1
 * @date 24.08.2025
 **/

public class OrderedData<T> {

    private String id;
    private T data;
    private double priority;
    private int ordering;
    private Predicate<Collection<OrderedData<T>>> forceInclude;

    public OrderedData(String id, T data, double priority, int ordering, Predicate<Collection<OrderedData<T>>> forceInclude) {
        this.id = id;
        this.data = data;
        this.priority = priority;
        this.ordering = ordering;
        this.forceInclude = forceInclude;
    }

    public OrderedData(String id, T data, double priority, int ordering) {
        this(id, data, priority, ordering, l -> false);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }

    public Predicate<Collection<OrderedData<T>>> getForceInclude() {
        return forceInclude;
    }

    public void setForceInclude(Predicate<Collection<OrderedData<T>>> forceInclude) {
        this.forceInclude = forceInclude;
    }
}
