package controller.scheduling.strategy;

import controller.Instrumentor.RVGlobalStateForInstrumentation;
import controller.scheduling.ChoiceType;

import java.util.List;
import java.util.SortedSet;

public class RVProfileStrategy extends SchedulingStrategy {

    private boolean notYetExecutedFirstSchedule;

	@Override
	public void startingExploration() {
	    this.notYetExecutedFirstSchedule = true;
	}

	@Override
	public void startingScheduleExecution() {
	    
      
	}

	@Override
	public void completedScheduleExecution() {

        this.notYetExecutedFirstSchedule = false;

	}

	@Override
	public boolean canExecuteMoreSchedules() {
	    if(!this.notYetExecutedFirstSchedule)
	    {
	        RVGlobalStateForInstrumentation.instance.saveMetaData();
	    }
        return this.notYetExecutedFirstSchedule;
	}

    @Override
    public Object choose(SortedSet<? extends Object> objectChoices, ChoiceType choiceType) {
        // TODO Auto-generated method stub
        return getChosenObject(0, objectChoices);
    }

    @Override
    public List<Integer> getChoicesMadeDuringThisSchedule() {
        // TODO Auto-generated method stub
        return null;
    }
}
