package controller.scheduling.filtering;

import java.util.SortedSet;

import controller.scheduling.ChoiceType;

/**
 * Interface that can be implemented to specify a scheduling filter that
 * restricts scheduling choices.
 */
public interface SchedulingFilter {

    /**
     * Computes and returns a filtered {@link SortedSet} of choices.
     * 
     * @param choices
     *            the original {@link SortedSet} of choices.
     * @param choiceType
     *            the {@link ChoiceType}.
     * @return a filtered {@link SortedSet} of choices.
     */
    public SortedSet<? extends Object> filterChoices(SortedSet<? extends Object> choices, ChoiceType choiceType);
}
