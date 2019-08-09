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

/**
 * A generic contract allowing to identify
 * the target destination after processing
 * the message.
 *
 * @param <N> the type of nodes of the transition
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Transition<N extends Node, M extends Message> {

    /**
     * The source node of the transition
     */
    private N origin;

    /**
     * The Message triggering the transition
     */
    private M message;

    /**
     * The target node of the transition
     */
    private N target;

}
