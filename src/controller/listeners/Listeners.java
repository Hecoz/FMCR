package controller.listeners;

import controller.MCRProperties;
import controller.scheduling.ChoiceType;
import controller.scheduling.ThreadInfo;
import controller.scheduling.events.EventDesc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class Listeners {

    private static final String DEBUG_EXPLORATION_DEFAULT = "false";

    private static final Set<ExplorationListener> explorationListeners;

    public static final boolean debugExploration;
    
    public static final boolean printContextSwitch = true;

    //static 代码块随着类的加载，只加载一次。作用是初始化类。
    static {
        
        MCRProperties mcrProps = MCRProperties.getInstance();
        explorationListeners = new HashSet<ExplorationListener>();

        if (debugExploration = Boolean.parseBoolean(mcrProps.getProperty(MCRProperties.EXPLORATION_DEBUG_KEY, DEBUG_EXPLORATION_DEFAULT))) {
            explorationListeners.add(new ExplorationDebugListener());
        }
        
        // 添加 ExplorationContextSwitchListener
        if (printContextSwitch) {
            explorationListeners.add(new ExplorationContextSwitchListener());
        }

        /* Add the other listeners to be used */
        //添加其他要使用的Listeners
        String listenerClassNames = mcrProps.getProperty(MCRProperties.LISTENERS_KEY, ExplorationStatsListener.class.getName());
        //添加 PaperStatsListener
        if (listenerClassNames != null) {
            for (String listenerClassName : listenerClassNames.split(",")) {
                try {
                    ExplorationListener listener = (ExplorationListener) Class.forName(listenerClassName).newInstance();
                    explorationListeners.add(listener);
                } catch (Exception e) {
                    System.err.println("Unable to obtain instance of: " + listenerClassName);
                    e.printStackTrace();
                }
            }
        }
    }

    public static void addListener(ExplorationListener listener) {
        explorationListeners.add(listener);
    }

    public static void removeListener(ExplorationListener listener) {
        explorationListeners.remove(listener);
    }

    public static void fireStartingExploration(String name) {
        for (ExplorationListener listener : explorationListeners) {
            listener.startingExploration(name);
        }
    }

    public static void fireStartingSchedule() {
        for (ExplorationListener listener : explorationListeners) {
            listener.startingSchedule();
        }
    }

    public static void fireBeforeForking(ThreadInfo childThread) {
        for (ExplorationListener listener : explorationListeners) {
            listener.beforeForking(childThread);
        }
    }

    public static void fireBeforeEvent(EventDesc eventDesc) {
        for (ExplorationListener listener : explorationListeners) {
            listener.beforeEvent(eventDesc);
        }
    }

    public static void fireAfterEvent(EventDesc eventDesc) {
        for (ExplorationListener listener : explorationListeners) {
            listener.afterEvent(eventDesc);
        }
    }

    public static void fireMakingChoice(SortedSet<? extends Object> objectChoices, ChoiceType choiceType) {
        for (ExplorationListener listener : explorationListeners) {
            listener.makingChoice(objectChoices, choiceType);
        }
    }

    public static void fireChoiceMade(Object choice) {
        for (ExplorationListener listener : explorationListeners) {
            listener.choiceMade(choice);
        }
    }

    public static void fireCompletedSchedule(List<Integer> choicesMade) {
        for (ExplorationListener listener : explorationListeners) {
            listener.completedSchedule(choicesMade);
        }
    }

    public static void fireCompletedExploration() {
        for (ExplorationListener listener : explorationListeners) {
            listener.completedExploration();
        }
    }

    public static void fireFailureDetected(String errorMsg, List<Integer> choicesMade) {
        for (ExplorationListener listener : explorationListeners) {
            listener.failureDetected(errorMsg, choicesMade);
        }
    }

}
