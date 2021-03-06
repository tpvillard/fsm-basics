package com.alu.oamp.fsm;

import java.util.HashSet;
import java.util.Set;

import static com.alu.oamp.fsm.States.newState;
import static com.alu.oamp.fsm.Timeout.newTimeout;

/**
 * A lift door with a heart beat state
 * <p>
 * States are [OPENED; CLOSE; OPENED_AND_RINGING] WITH the following events:
 * OPEN to open the door.
 * CLOSE to close the door
 * PRESENCE to notify the door that someone/something blocks the door. (coming from some kind of sensor)
 * ABSENCE to notify the door that nothing blocks the door
 * </p>
 * <p>On OPEN the door moves to the OPENED state</p>
 * <p>The door closes itself after 500 ms if no presence is detected</p>
 * <p>The door rings if it stays opened for more than 1000 ms/p>
 * Several scenario are tested.
 */
public class LiftDoorWithHeartBeat {

    private SimpleStateMachine fsm;
    private volatile boolean closeable = true;
    private volatile boolean ringing = false;

    public void fireEvent(Cmd cmd) {
        fsm.fireEvent(cmd);
    }

    enum Cmd implements EventId {
        OPEN,
        CLOSE,
        PRESENCE,
        ABSENCE
    }

    enum State implements StateId {

        OPENED,
        OPENED_AND_RINGING,
        CLOSED
    }

    static LiftDoorWithHeartBeat newDoor(SimpleStateListener listener) {
        LiftDoorWithHeartBeat liftDoor = new LiftDoorWithHeartBeat();
        liftDoor.initWithHeartBeat(listener);
        return liftDoor;
    }

    private SimpleStateMachine initWithHeartBeat(SimpleStateListener listener) {

        Set<com.alu.oamp.fsm.State> states = new HashSet<>();

        com.alu.oamp.fsm.State state =
                newState(State.OPENED)
                        .heartbeat(Heartbeat.newHeartbeat()
                                .period(1000)
                                .action(() -> fireEvent(Cmd.CLOSE))
                                .build())
                        .timeout(newTimeout()
                                .timeout(6000)
                                .target(State.OPENED_AND_RINGING)
                                .build())
                        .build();
        states.add(state);

        // Door is opened and ringing, when door closes stop the bell
        state = newState(State.OPENED_AND_RINGING)
                .onEntry(() -> ringing = true)
                .heartbeat(Heartbeat.newHeartbeat()
                        .period(50)
                        .action(() -> fireEvent(Cmd.CLOSE))
                        .build())
                .onExit(() -> ringing = false)
                .build();
        states.add(state);

        state = newState(State.CLOSED).build();
        com.alu.oamp.fsm.State initial = state;
        states.add(state);

        // Transition to open the door
        Set<Transition> transitions = new HashSet<>();
        Transition transition = Transition.newTransition(states)
                .from(State.CLOSED)
                .event(Cmd.OPEN)
                .to(State.OPENED).build();
        transitions.add(transition);

        // Transition for presence detection on opened state
        transition =
                Transition.newTransition(states)
                        .from(State.OPENED)
                        .event(Cmd.PRESENCE)
                        .action(() -> closeable = false).build();
        transitions.add(transition);

        // Transition for absence detection on opened state
        transition =
                Transition.newTransition(states)
                        .from(State.OPENED)
                        .event(Cmd.ABSENCE)
                        .action(() -> closeable = true).build();
        transitions.add(transition);

        // Transition to close the door if door is closeable
        transition =
                Transition.newTransition(states)
                        .from(State.OPENED)
                        .event(Cmd.CLOSE)
                        .when(() -> closeable)
                        .to(State.CLOSED)
                        .build();
        transitions.add(transition);

        // Transition to close the door if door is closeable
        transition =
                Transition.newTransition(states)
                        .from(State.OPENED_AND_RINGING)
                        .event(Cmd.CLOSE)
                        .when(() -> closeable)
                        .to(State.CLOSED)
                        .build();
        transitions.add(transition);

        // Transition for presence detection on opened and ringing state state
        transition =
                Transition.newTransition(states)
                        .from(State.OPENED_AND_RINGING)
                        .event(Cmd.ABSENCE)
                        .action(() -> closeable = true).build();
        transitions.add(transition);

        fsm = new SimpleStateMachine(states, transitions, "Heart Beat Door", initial);
        fsm.addStateMachineListener(listener);
        return fsm;
    }


    public void shutdown() {
        fsm.shutdown();
    }

    public boolean isRinging() {
        return ringing;
    }

}
