package controller.scheduling.strategy;

import controller.scheduling.ChoiceType;

import java.util.List;
import java.util.SortedSet;

public class FMCRStrategy extends SchedulingStrategy {


    @Override
    public void startingExploration() {

    }

    @Override
    public void startingScheduleExecution() {

    }

    @Override
    public void completedScheduleExecution() {

    }

    @Override
    public boolean canExecuteMoreSchedules() {
        return false;
    }

    @Override
    public Object choose(SortedSet<?> objectChoices, ChoiceType choiceType) {
        return null;
    }

    @Override
    public List<Integer> getChoicesMadeDuringThisSchedule() {
        return null;
    }
}
