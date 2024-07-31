/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 30-7-2024 at 20:27 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.queue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * The record containing the queue request parameter that define to what queue should be looked at.
 * @param parameters A sorted array where the lowest index defines the most important parameter.
 */
public record QueueRequestParameters(ArrayList<Set<QueueRequestParameter>> parameters) {

    public static QueueRequestParameters lobbyParameters = create().add(QueueRequestParameter.lobbyParameter).build();

    public static Builder create() {
        return new Builder();
    }

    public static QueueRequestParameters create(Function<Builder, Builder> apply) {
        return apply.apply(new Builder()).build();
    }

    public static class Builder {

        private final ArrayList<Set<QueueRequestParameter>> parameters;

        public Builder(ArrayList<Set<QueueRequestParameter>> parameters) {
            this.parameters = parameters == null ? new ArrayList<>() : parameters;
        }

        public Builder(QueueRequestParameters parameters) {
            this.parameters = parameters.parameters() == null ? new ArrayList<>() : parameters.parameters();
        }

        public Builder() {
            parameters = new ArrayList<>();
        }

        public final Builder clear() {
            parameters.clear();
            return this;
        }

        /**
         * Adds new parameters at the same priority at the end of existing parameters.
         * @param parameters The parameters to add.
         * @return The builder.
         */
        public final Builder add(QueueRequestParameter... parameters) {
            if(parameters.length < 1) return this;
            this.parameters.add(Set.of(parameters));
            return this;
        }

        /**
         * Adds new parameters at the same priority at the end of existing parameters.
         * @param parameters The parameters' builders to add.
         * @return The builder.
         */
        public final Builder add(QueueRequestParameter.Builder... parameters) {
            if(parameters.length < 1) return this;
            HashSet<QueueRequestParameter> set = new HashSet<>();
            for(QueueRequestParameter.Builder parameter : parameters) {
                set.add(parameter.build());
            }
            this.parameters.add(set);
            return this;
        }

        /**
         * Adds new parameters at the same priority at the end of existing parameters.
         * @param parameters The functions to build the new parameters.
         * @return The builder.
         */
        @SafeVarargs
        public final Builder add(Function<QueueRequestParameter.Builder, QueueRequestParameter.Builder>... parameters) {
            if(parameters.length < 1) return this;
            HashSet<QueueRequestParameter> set = new HashSet<>();
            for(Function<QueueRequestParameter.Builder, QueueRequestParameter.Builder> parameter : parameters) {
                set.add(parameter.apply(new QueueRequestParameter.Builder()).build());
            }
            this.parameters.add(set);
            return this;
        }

        /**
         * Adds new parameters with different priorities at the end of existing parameters.
         * @param parameters The parameters to add.
         * @return The builder.
         */
        public final Builder addSeparate(QueueRequestParameter... parameters) {
            if(parameters.length < 1) return this;
            for(QueueRequestParameter parameter : parameters) {
                this.parameters.add(Set.of(parameter));
            }
            return this;
        }

        /**
         * Adds new parameters with different priorities at the end of existing parameters.
         * @param parameters The parameters' builders to add.
         * @return The builder.
         */
        public final Builder addSeparate(QueueRequestParameter.Builder... parameters) {
            if(parameters.length < 1) return this;
            for(QueueRequestParameter.Builder parameter : parameters) {
                this.parameters.add(Set.of(parameter.build()));
            }
            return this;
        }

        /**
         * Adds new parameters with different priorities at the end of existing parameters.
         * @param parameters The functions to build the new parameters.
         * @return The builder.
         */
        @SafeVarargs
        public final Builder addSeparate(Function<QueueRequestParameter.Builder, QueueRequestParameter.Builder>... parameters) {
            if(parameters.length < 1) return this;
            for(Function<QueueRequestParameter.Builder, QueueRequestParameter.Builder> parameter : parameters) {
                this.parameters.add(Set.of(parameter.apply(new QueueRequestParameter.Builder()).build()));
            }
            return this;
        }

        public final QueueRequestParameters build() {
            return new QueueRequestParameters(parameters);
        }

    }

}
