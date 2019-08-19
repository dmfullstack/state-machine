<p align="center">
    <img src="images/logo_new.png"/>
</p>

<p align="center">
    <a href="https://travis-ci.org/pnavais/state-machine">
        <img src="https://img.shields.io/travis/pnavais/state-machine.svg"
             alt="Build Status"/>
    </a>
    <a href="https://coveralls.io/github/pnavais/state-machine?branch=master">
        <img src="https://img.shields.io/coveralls/pnavais/state-machine.svg"
             alt="Coverage"/>
    </a>
     <a href="LICENSE">
       <img src="https://img.shields.io/github/license/pnavais/state-machine.svg"
            alt="License"/>         
    </a>
    <a href="https://sonarcloud.io/dashboard/index/org.payball:state-machine">
        <img src="https://sonarcloud.io/api/project_badges/measure?project=org.payball:state-machine&metric=alert_status"
             alt="Quality Gate"/>
    </a>
</p>

<p align="center"><sup><strong>Generic State Machine implementation for Java 8+</strong></sup></p>

## Basic usage

```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").on("1")
                .from("B").to("C").on("2")                
                .build();
 ```
 
Creates a new State Machine as per the following diagram : 

![alt text](images/simple_graph.png "Simple graph diagram")

When using the builder, the State Machine is automatically initialized using as current state the first node added (i.e "A" in the previous example).

A transition can be specified without a named message : 

```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").build();
 ```

which is a shorthand equivalent to : 
```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").on(Messages.EMPTY).build();
```
 
Transitions for any message can be specified using : 
 ```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").on(Messages.ANY).build();
 ```

### Traversal

Once initialized, the State Machine can be traversed by sending named messages :

```java
// A --- 1 ---> B --- 2 ---> C
State current = stateMachine.send("1").send("2").getCurrent(); 
System.out.println(current.getName()); // --> "C"
```

or empty messages : 

```java
// A ---> B
State current = stateMachine.next().getCurrent(); 
System.out.println(current.getName()); // --> "B"
```

Additionally wildcard messages can also be sent (if transitions supporting wildcards were added) : 

 ```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").on(Messages.ANY)
                .from("A").to("C").on("3").build();
 ```

Wildcard messages are used as fallback if the state does not support a direct transition for the given message i.e. : 

```java
stateMachine.getNext("3"); // --> C
stateMachinne.init();      // --> A again
stateMachine.getNext("4")  // --> B
```

In case the current state does not support the message sent, the latter will be silently ignored and thus no transition will be made.
Be aware that an **empty message is not similar to a wildcard message** (i.e. Messages.EMPTY != Messages.ANY) and thus a transition defined with no message is only triggered by an empty message.

The state can be initialized to any existing state at any time : 
```java
stateMachine.setCurrent("A");
State next = stateMachine.getNext("3"); // --> C
```

In case the state is not recognized a NullStateException is raised.
 
 ## Advanced usage
   
 ### Initializiation using State Transitions
 
 State transitions can be used directly when building the machine :
 ```java
 StateMachine stateMachine = StateMachine.newBuilder().add(new StateTransition("A", "1", "B")).build();
 ```
 
  ### Initializiation without the Builder
  
  The State Machine can also be initialized directly without the builder fluent language this way : 
  
  ```java
  StateMachine stateMachine = new StateMachine();
  
  stateMachine.add(new StateTransition("a", "0.2", "b"));
  stateMachine.add(new StateTransition("a", "0.4", "c"));
  stateMachine.add(new StateTransition("c", "0.6", "b"));
  stateMachine.add(new StateTransition("c", "0.6", "e"));
  stateMachine.add(new StateTransition("e", "0.1", "e"));
  stateMachine.add(new StateTransition("e", "0.7", "b"));
  
  ```
  
  Which leads to the following diagram : 
  
  ![alt text](images/manual_graph.png "Manual State machine creation graph diagram")
  
Please notice that the current state machine after manual creation must be specified manually 
```java
stateMachine.init(); // --> Initializes to the first state added to the machine (i.e. a)
stateMachine.setCurrent("b");
```
  
 ### Self loops
 
 Transitions to the same state can be specified this way : 
 
 ```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").on("1")
                .from("B").to("C").on("3")
                .selfLoop("B").on("2")
                .build();
 ```
 
 Which is equivalent to the following state machine diagram : 
 
 ![alt text](images/graph_with_loops.png "Graph with loops")
 
 
### Initializiation using custom States

```java
State initialState = new State("A");
StateMachine stateMachine = StateMachine.newBuilder().from(initialState).to("B").build();
```
When adding states to the machine, the name is used to verify if the state is already in place. In that case no additional state is added but rather merged to the existing one (See [Merging states](#Merging-states) section for more information).

 
 ### Final states
 
 States can be flagged as final in order to avoid potential transitions from them : 
 
 ```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B")
                .from("B").to(State.from("C").isFinal(true).build())
                .build();
 ```
 
 In case a transition is later added from a final state an IllegalTransitionException is raised.
  
 
 ### Message filtering
 
Custom handlers can be specified globally or message-scoped to intercept transitions occurring in the State Machine which are in turn triggered by incoming messages. These handlers can be specified at either departure or arrival of the transition. 
 
 See the following examples to have a better understanding of the concept.
   
 #### Global filters
 
 Just add a ```"leaving"``` or ```"arriving"``` clause to the builder specifying the handler to be executed on departure/arrival to the states involved in the current transition.
 
 ```java
 // Adds a global handler to filter any depature from state A
  StateMachineBuilder stateMachineBuilder = StateMachine.newBuilder()
                .add(new StateTransition("A", "1","B"))
                .add(new StateTransition("A", "2","C"))
                .leaving("A").execute(context -> {
                    messages.add(String.format("Departing from [%s] to [%s] on [%s]", context.getSource(), context.getTarget(),context.getMessage()));
                    return Status.PROCEED;
                }).build();
 ```
 
In this case, the lambda function specified when leaving state A will be executed for any message received. The transition can be either accepted/rejected depending on the supplied Status (predefined PROCEED/ABORT or custom with a given valid status).
 
#### Message-scoped filters
 
The handlers can be also specified on a per message basis as described below :

```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").on("1").arriving(context -> {
                    return doSomeProcessing(); // Do wathever you want and return a Status
                })
```

In this case, the lambda function will only be executed when the "1" message is sent for a transition from A to B.
 
 ### Custom messages
 
State Machine supports by default an special implementation of the ```Message``` interface ```StringMessage``` which only contains a message identifier as payload but any special Message can be specied.

The following example specifies a Message with a custom payload : 

```java
AtomicInteger counter = new AtomicInteger(100);

Message customMessage = new Message() {
    @Override
    public UUID getMessageId() {
        return UUID.randomUUID();
    }

    @Override
    public Payload getPayload() {
        return () -> counter;
    }
};

StateMachine stateMachine = StateMachine.newBuilder()
        .from("A").to("B").on(customMessage)
        .from("A").to("C")
        .leaving("A").execute(context -> {
            System.out.println("Counter >> "+context.getMessage().getPayload().get());
            return Status.PROCEED;
        }).build();

stateMachine.init();
stateMachine.send(message).getCurrent(); // Counter >> 100 (The integer payload)

stateMachine.init();
System.out.println(stateMachine.next().getCurrent()); // Counter >> _ (Empty payload)
```
 
 ### Custom properties
 
State instances can optionally contain any arbitrary String property attached to them (this is specially useful when exporting the state machine to an output format.

```java
State state = new State("A");
state.addProperty("prop", "value"); // To add the property with the given value
state.removeProperty("prop");       // To remove it
```
  
 ### Merging states
 
As already mentioned previously, in case a new state to be added to the State Machine already exists, the information of both states (existing and new) is merged automatically. This implies preserving the final state value and copying/overriding properties and message filters (if any).
 
 ### Exporting to GraphViz DOT language format
 
 A very basic DOT exporter is also provided allowing to export a given State Machine to the DOT language : 
 
```java
StateMachine stateMachine = StateMachine.newBuilder()
                .from("A").to("B").on("3")
                .selfLoop("C").on("3")
                .from("B").to("C").on("4")
                .selfLoop("A").on(Messages.ANY)
                .from("B").to(State.from("D").isFinal(true).build())
                .build();

DOTExporter.builder().build().exportToFile(stateMachine, "graph.gv");
```

Which eventually can be later processed by the DOT tool to produce an image :

```
dot -Tpng graph.gv -o graph.png
```

 ![alt text](images/exported_graph.png "Exported DOT graph")
 
 <div><sup>Icon made by <a href="https://www.flaticon.com/authors/smashicons" title="Smashicons">Smashicons</a> from <a href="http://www.flaticon.com" title="Flaticon">www.flaticon.com</a></sup></div>

 
