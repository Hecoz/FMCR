package controller.listeners;

import controller.scheduling.ChoiceType;
import controller.scheduling.ThreadInfo;
import controller.scheduling.events.EventDesc;

import java.util.List;
import java.util.SortedSet;


public class ExplorationListenerAdapter implements ExplorationListener {

    @Override
    public void startingExploration(String name) {

    }

    @Override
    public void startingSchedule() {

    }

    @Override
    public void makingChoice(SortedSet<? extends Object> choices, ChoiceType choiceType) {

    }

    @Override
    public void choiceMade(Object choice) {

    }

    @Override
    public void completedSchedule(List<Integer> choicesMade) {

    }

    @Override
    public void completedExploration() {

    }

    @Override
    public void beforeForking(ThreadInfo childThread) {

    }

    @Override
    public void beforeEvent(EventDesc eventDesc) {

    }

    @Override
    public void afterEvent(EventDesc eventDesc) {

    }

    @Override
    public void failureDetected(String errorMsg, List<Integer> choicesMade) {

    }

}
