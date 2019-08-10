/*
 * Copyright 2019 Pablo Navais
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.pnavais.machine.api;

import lombok.*;

import java.util.Objects;

/**
 * A generic contract allowing to identify
 * the target destination after processing
 * the message.
 *
 * @param <N> the type of nodes of the transition
 */
@Getter
@AllArgsConstructor
public abstract class Transition<N extends Node, M extends Message> {

    /**
     * The source node of the transition
     */
    private final N origin;

    /**
     * The Message triggering the transition
     */
    private final M message;

    /**
     * The target node of the transition
     */
    private final N target;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transition<?, ?> that = (Transition<?, ?>) o;
        return Objects.equals(origin, that.origin) &&
                Objects.equals(message, that.message) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, message, target);
    }
}
