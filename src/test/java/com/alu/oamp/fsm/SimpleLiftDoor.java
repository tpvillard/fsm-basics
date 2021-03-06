package com.alu.oamp.fsm;

import java.util.HashSet;
import java.util.Set;

import static com.alu.oamp.fsm.States.newState;
import static com.alu.oamp.fsm.Transition.newTransition;
import static com.alu.oamp.fsm.Timeout.newTimeout;

/**
 * This is a simple state machine simulating a lift door.
 * <p>
 * States are [OPENED; CLOSED]
 * and 1 command for the door: OPEN.
 * </p>
 * <p>On OPEN, the door transitions to the OPENED state</p>
 * <p>The OPENED state times out after 500 ms and the door returns to the CLOSED state</p>
 * Several scenario are tested.
 */
public class SimpleLiftDoor {

    private SimpleStateMachine fsm;

    enum Cmd implements EventId {
        OPEN,
        CLOSE
    }

    enum State implements StateId {

        OPENED,
        CLOSED
    }

    public void fireEvent(Cmd cmd) {
        fsm.fireEvent(cmd);
    }

    static SimpleLiftDoor newLiftDoor(SimpleStateListener listener) {
        SimpleLiftDoor liftDoor = new SimpleLiftDoor();
        liftDoor.init(listener);
        return liftDoor;
    }

    private SimpleStateMachine init(SimpleStateListener listener) {

        Set<com.alu.oamp.fsm.State> states = new HashSet<>();

        // The door stays opened for 500 ms and closes itself.
        com.alu.oamp.fsm.State state = newState(State.OPENED)
                .timeout(newTimeout().timeout(1000).target(State.CLOSED).build())
                .build();
        states.add(state);

        state = newState(State.CLOSED).build();
        com.alu.oamp.fsm.State initial = state;
        states.add(state);

        Set<Transition> transitions = new HashSet<>();

        // Transition to open the door
        Transition transition =
                newTransition(states).from(State.CLOSED)
                        .event(Cmd.OPEN).to(State.OPENED).build();

        transitions.add(transition);
        transition =
                newTransition(states).from(State.OPENED)
                        .event(Cmd.CLOSE).to(State.CLOSED).build();
        transitions.add(transition);

        fsm = new SimpleStateMachine(states, transitions, "Simple Lift Door", initial);
        fsm.addStateMachineListener(listener);
        return fsm;
    }


    public void shutdown() {
        fsm.shutdown();
    }
}
