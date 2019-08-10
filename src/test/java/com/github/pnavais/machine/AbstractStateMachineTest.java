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
package com.github.pnavais.machine;

import com.github.pnavais.machine.api.Message;
import com.github.pnavais.machine.model.State;
import com.github.pnavais.machine.model.StateTransition;
import com.github.pnavais.machine.model.StringMessage;
import com.github.pnavais.machine.utils.StateTransitionPrintOptions;
import com.github.pnavais.machine.utils.StateTransitionPrinter;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for state machine tests
 */
public abstract class AbstractStateMachineTest {

    /** The state printer */
    private static StateTransitionPrintOptions<State, Message> statePrinterOptions;

    /** The state printer */
    private static StateTransitionPrinter<State, Message, StateTransition> statePrinter;

    @BeforeAll
    protected static void init() {
        statePrinterOptions = StateTransitionPrintOptions.builder()
                .stateFormatter(s -> s.getName()+" ["+s.getId()+"]")
                .build().fillDefaults();

        statePrinter = getStatePrinterBuilder().build();
    }

    /**
     * Retrieves the state printer options
     *
     * @return the state printer options
     */
    protected static StateTransitionPrintOptions<State,Message> getStatePrinterOptions() {
        return statePrinterOptions;
    }

    /**
     * Retrieves the state transitions printer builder
     *
     * @return the state transitions printer builder
     */
    protected static StateTransitionPrinter.StateTransitionPrinterBuilder<State, Message, StateTransition> getStatePrinterBuilder() {
         return StateTransitionPrinter.builder();
    }

    /**
     * Retrieves the state transitions printer
     *
     * @return the state transitions printer
     */
    protected static StateTransitionPrinter<State, Message, StateTransition> getStatePrinter() {
        return statePrinter;
    }


    /**
     * Retrieves a simple State Machine with the following transitions :
     *  +--------+---------+--------+
     *  | Source | Message | Target |
     *  +--------+---------+--------+
     *  |   A    |    1    |   B    |
     *  +--------+---------+--------+
     *  |   B    |    2    |   C    |
     *  +--------+---------+--------+
     *
     *  Which is equivalent to :
     *  +--------+------------+
     *  | Source |   Target   |
     *  +--------+------------+
     *  |   A    | [ 1 -> B ] |
     *  +--------+------------+
     *  |   B    | [ 2 -> C ] |
     *  +--------+------------+
     *  |   C    |     []     |
     *  +--------+------------+
     *
     * @return the test state machine
     */
    protected StateMachine createStateMachine() {
        StateMachine machine = new StateMachine();

        machine.add(new StateTransition(new State("A"), new StringMessage("1"), new State("B")));
        machine.add(new StateTransition(new State("B"), new StringMessage("2"), new State("C")));
        return machine;
    }
}
