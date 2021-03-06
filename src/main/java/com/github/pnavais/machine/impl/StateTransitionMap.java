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
package com.github.pnavais.machine.impl;

import com.github.pnavais.machine.api.exception.NullStateException;
import com.github.pnavais.machine.api.exception.ValidationException;
import com.github.pnavais.machine.api.message.Message;
import com.github.pnavais.machine.api.transition.TransitionIndex;
import com.github.pnavais.machine.api.validator.TransitionValidator;
import com.github.pnavais.machine.api.validator.ValidationResult;
import com.github.pnavais.machine.model.State;
import com.github.pnavais.machine.model.StateTransition;
import lombok.Getter;
import lombok.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The State Machine contains a simple map of Transitions between
 * different nodes (States) triggered by incoming messages.
 *
 * This transition index is implemented as a map using as key
 * the source of the transition and as key another map containing
 * the association between messages and destination states.
 *<p>
 * For example, the transition [ A -- m --&gt; B ] would be stored in a map
 * as represented in the table below :
 * </p>
 * <pre>
 * Key    | Transitions
 * --------------------
 * A      | [ m -&gt;  B ]
 * B      | []
 * </pre>
 * It is important to note that target states will be also stored
 * as key in the transitions map.
 *
 * In case a new transition from A is added , [ A --- n ---&gt; C ] ,
 * the transitions would be updated as :
 * <pre>
 * Key    | Transitions
 * --------------------
 * A      | [ m -&gt;  B,  n -&gt;  C ]
 * B      | []
 * C      | []
 * </pre>
 *
 * In case a transition is added from this state using
 * the same message [ A --- m ---&gt; C, the transition map would be updated as follows :
 * <pre>
 * Key    | Transitions
 * --------------------
 * A      | [ m -&gt;  C,  n -&gt;  C ]
 * B      | []
 * C      | []
 * </pre>
 *
 * After this operation State B is not reachable
 */
@Getter
public class StateTransitionMap implements TransitionIndex<State, Message, StateTransition> {

    /**
     * The transitions stored by the state machine
     */
    private Map<State, Map<Message, State>> transitionMap;

    /**
     * The transition validator
     */
    private TransitionValidator<State, Message, StateTransition> transitionValidator;

    /**
     * Creates the state machine.
     */
    public StateTransitionMap() {
        this(new StateTransitionValidator());
    }

    /**
     * Creates the state machine with a given transition map.
     *
     * @param transitionMap the transition map
     */
    public StateTransitionMap(@NonNull Map<State, Map<Message, State>> transitionMap) {
        this(transitionMap, new StateTransitionValidator());
    }

    /**
     * Creates the state machine with a custom transition validator.
     *
     * @param transitionValidator transition validator
     */
    public StateTransitionMap(@NonNull TransitionValidator<State, Message, StateTransition> transitionValidator) {
        this(new LinkedHashMap<>(), transitionValidator);
    }

    /**
     * Creates the state machine with a custom transition map
     * and validator.
     * @param transitionMap the transition map
     * @param transitionValidator transition validator
     */
    public StateTransitionMap(@NonNull Map<State, Map<Message, State>> transitionMap, @NonNull TransitionValidator<State, Message, StateTransition> transitionValidator) {
        this.transitionMap = transitionMap;
        this.transitionValidator = transitionValidator;
    }

    /**
     * Adds a new Transition to the statemachine.
     * If already present, it is replaced silently
     * i.e if a transition is found from a source state with
     * the input message, the destination is updated by the
     * one given in the transition.
     *
     * @param transition the transition to add
     */
    @Override
    public void add(StateTransition transition) {
        validateAndExecute(transition,TransitionValidator.Operation.ADD, this::addTransition);
    }

    /**
     * Adds the transition to the transition map.
     * A validation has been performed previously so it
     * is safe to assume the transition is fine.
     * @param transition transition to add
     */
    private void addTransition(StateTransition transition) {
        // Retrieve the current transitions mapping
        Map<Message, State> messageStateMap = Optional.ofNullable(transitionMap.get(transition.getOrigin())).orElse(new LinkedHashMap<>());

        // Update origin with mappings
        messageStateMap.put(transition.getMessage(), find(transition.getTarget().getName()).orElse(transition.getTarget()));

        // Update the transition map
        if (!transitionMap.containsKey(transition.getOrigin())) {
            transitionMap.put(transition.getOrigin(), messageStateMap);
        }

        // Add target to the index if not found
        if (!transitionMap.containsKey(transition.getTarget())) {
            transitionMap.put(transition.getTarget(), new LinkedHashMap<>());
        }

        // Merge the states information from the transition
        mergeStates(transition);
    }

    /**
     * Merges the states information from the transition
     * to their counterparts currently stored in the index.
     *
     * @param transition the transition
     */
    private void mergeStates(StateTransition transition) {
        findAndMerge(transition.getOrigin());
        findAndMerge(transition.getTarget());
    }

    /**
     * Merge the current state if found
     * in the index.
     *
     * @param state the state to merge
     */
    private void findAndMerge(State state) {
        transitionMap.keySet().stream()
                .filter(s -> s.equals(state))
                .findFirst().ifPresent(s -> s.merge(state));
    }

    /**
     * Removes an existing Transition from the state machine.
     * In case the state does not exists is it ignored
     * silently.
     *
     * @param transition the transition to remove
     */
    @Override
    public void remove(StateTransition transition) {
        // Update the current transitions mapping
        validateAndExecute(transition,TransitionValidator.Operation.REMOVE,
                t -> Optional.ofNullable(transitionMap.get(transition.getOrigin())).ifPresent(m -> m.remove(transition.getMessage())));
    }

    /**
     * Performs a validation of the transition before executing the function associated
     * to the given operation.
     *
     * @param transition the transition to validate
     * @param operation the operation to execute
     * @param transitionFunction the function associated with the operation
     */
    private void validateAndExecute(StateTransition transition, TransitionValidator.Operation operation, Consumer<StateTransition> transitionFunction) {
        ValidationResult result = transitionValidator.validate(transition, this, operation);
        if ((result.isValid()) || (!result.isValid() && TransitionValidator.FailurePolicy.PROCEED.equals(transitionValidator.getFailurePolicy()))) {
            transitionFunction.accept(transition);
        } else if (!result.isValid() && TransitionValidator.FailurePolicy.THROW_ON_FAILURE.equals(transitionValidator.getFailurePolicy())) {
            // Throws the exception on failure
            throw (result.getException() != null) ? result.getException() : new ValidationException(result.getDescription());
        }
    }

    /**
     * Creates a transition exception for the given state.
     *
     * @param stateName the state name
     * @return the transition exception
     */
    private Supplier<NullStateException> getNullTransitionException(@NonNull String stateName) {
        return () -> new NullStateException("State [" + stateName + "] not found");
    }

    /**
     * Removes the current state from the machine
     * including defined transitions.
     *
     * @param state the state to remove
     */
    @Override
    public void remove(@NonNull State state) {
        // Remove transition mappings
        Map<Message, State> messageStateMap = Optional.ofNullable(transitionMap.get(state))
                .orElseThrow(getNullTransitionException(state.getName()));
        messageStateMap.clear();

        // Remove state
        transitionMap.remove(state);

        // Remove transitions using the state as target
        transitionMap.values().forEach(m -> m.values().removeIf(s -> s.getName().equals(state.getName())));
    }

    /**
     * Removes the current state from the machine
     * including defined transitions.
     *
     * @param stateName the state to remove
     */
    @Override
    public void remove(@NonNull String stateName) {
        State state = find(stateName).orElseThrow(getNullTransitionException(stateName));
        remove(state);
    }

    /**
     * Clear all transitions from the map
     */
    @Override
    public void removeAllTransitions() {
        this.transitionMap.forEach((state, messageStateMap) -> messageStateMap.clear());
    }

    /**
     * Clear all states and transitions from the map
     */
    @Override
    public void clear() {
        this.transitionMap.clear();
    }

    /**
     * Retrieves the next state in the transition from
     * source state upon message m reception
     * @param source the origin state
     * @param m the received message
     *
     * @return the next state if found or empty otherwise
     */
    @Override
    public Optional<State> getNext(@NonNull State source, @NonNull Message m) {
        return Optional.ofNullable(transitionMap.get(source)).map(messageStateMap -> messageStateMap.get(m));
    }

    /**
     * Retrieves the previous node upon reception of the
     * message on the given source node.
     *
     * @param source the origin node
     * @param m the message
     * @return the next node if found or empty otherwise
     */
    @Override
    public Optional<State> getPrevious(State source, Message m) {
        return transitionMap.keySet().stream().filter(state -> source.equals(transitionMap.get(state).get(m))).findFirst();
    }

    /**
     * Finds the given state by its name.
     *
     * @param stateName the name of the state
     *
     * @return the state if found or empty otherwise
     */
    @Override
    public Optional<State> find(String stateName) {
        return transitionMap.keySet().stream().filter(state -> state.getName().compareTo(stateName) == 0).findFirst();
    }

    /**
     * Checks the presence of the given state in
     * the index.
     *
     * @param state the state to find
     * @return true if state present, false otherwise
     */
    @Override
    public boolean contains(@NonNull State state) {
        return transitionMap.containsKey(state);
    }

    /**
     * Checks the presence of the given transition in
     * the index.
     *
     * @param transition the transition to find
     * @return true if transition present, false otherwise
     */
    @Override
    public boolean contains(@NonNull StateTransition transition) {
        Optional<State> target = getNext(transition.getOrigin(), transition.getMessage());
        return target.isPresent() && target.get().equals(transition.getTarget());
    }

    /**
     * Retrieves the first state in the transition map
     *
     * @return the first state in the transition map
     */
    @Override
    public Optional<State> getFirst() {
        return Optional.ofNullable(!transitionMap.isEmpty() ? transitionMap.keySet().iterator().next() : null);
    }

    /**
     * Retrieves the number of states in the transition map
     *
     * @return the number of states in the transition map
     */
    @Override
    public int size() {
        return transitionMap.size();
    }

    /**
     * Remove orphan states from the transition map
     * i.e. States with no transitions and not involved in other
     * state transitions.
     */
    @Override
    public List<State> prune() {
        List<State> orphanStates = new ArrayList<>();
        // Look for states with empty transitions
        // as potential orphans
        List<State> emptyStates = transitionMap.keySet().stream()
                .filter(state -> transitionMap.get(state).isEmpty())
                .collect(Collectors.toList());

        // Remove states that are not involved in any transition
        emptyStates.forEach(emptyState -> {
            if (transitionMap.keySet().stream().noneMatch(state -> transitionMap.get(state).containsValue(emptyState))) {
                transitionMap.remove(emptyState);
                orphanStates.add(emptyState);
            }
        });

        return orphanStates;
    }

    /**
     * Retrieves the transitions from the given state
     * or throws a {@link NullStateException}
     * if not found.
     * @param stateName the state's name
     *
     * @return the transitions
     */
    @Override
    public Collection<StateTransition> getTransitions(String stateName) {
        State state = find(stateName).orElseThrow(getNullTransitionException(stateName));
        return getTransitions(state);
    }

    /**
     * Retrieves the transitions from the given state
     * or throws a {@link NullStateException}
     * if not found.
     * @param state the state
     *
     * @return the transitions
     */
    @Override
    public Collection<StateTransition> getTransitions(State state) {

        Map<Message, State> messageStateMap = transitionMap.get(state);

        return messageStateMap.keySet().stream()
                     .map(message -> new StateTransition(state, message, messageStateMap.get(message)))
                     .collect(Collectors.toList());
    }

    /**
     * Retrieves the transitions as a map
     *
     * @return the transitions as a map
     */
    @Override
    public Map<State, Map<Message, State>> getTransitionsAsMap() {
        return transitionMap;
    }

    /**
     * Retrieve all the transitions stored in the transition map.
     *
     * @return the collection of transitions
     */
    @Override
    public Collection<StateTransition> getAllTransitions() {
        Collection<StateTransition> transitions = new ArrayList<>();
        transitionMap.keySet().forEach(state ->
                transitionMap.get(state).keySet().stream()
                        .map(message -> new StateTransition(state, message, transitionMap.get(state).get(message))
        ).forEachOrdered(transitions::add));
        return transitions;
    }

    /**
     * Adds all supplied transitions to the index.
     * @param transitions the transitions to add
     */
    @Override
    public void addAll(@NonNull Collection<StateTransition> transitions) {
        transitions.forEach(this::add);
    }

}
