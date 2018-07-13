package controller.scheduling.filtering;

import java.util.SortedSet;

import controller.scheduling.ChoiceType;

/**
 * Default {@link SchedulingFilter} that does not perform any filtering.
 */
public class DefaultFilter implements SchedulingFilter {

    @Override
    public SortedSet<? extends Object> filterChoices(SortedSet<? extends Object> choices, ChoiceType choiceType) {
        return choices;
    }

}
