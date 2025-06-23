/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 30-7-2024 at 20:14 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lane.queue;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Optional;

public record QueueRequest(QueueRequestReason reason, Component reasonMessage, QueueRequestParameters parameters, ArrayList<QueueStage> stages) {

    public QueueRequest(QueueRequestReason reason, QueueRequestParameters parameters) {
        this(reason, null, parameters, new ArrayList<>());
    }

    public QueueRequest(QueueRequestReason reason, Component reasonMessage, QueueRequestParameters parameters) {
        this(reason, reasonMessage, parameters, new ArrayList<>());
    }

    public Optional<Component> getReasonMessage() {
        return Optional.ofNullable(reasonMessage);
    }

    public Optional<QueueStage> getFirstStage() {
        return stages == null || stages.isEmpty() ? Optional.empty() : Optional.of(stages.get(stages.size() - 1));
    }

    public Optional<QueueStage> getLastStage() {
        return stages == null || stages.isEmpty() ? Optional.empty() : Optional.of(stages.get(0));
    }

}
