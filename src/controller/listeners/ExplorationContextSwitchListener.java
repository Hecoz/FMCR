package controller.listeners;

import controller.exploration.Scheduler;
import controller.scheduling.ThreadInfo;
import controller.scheduling.events.EventDesc;
import controller.scheduling.events.LocationDesc;

import java.util.ArrayList;
import java.util.List;


public class ExplorationContextSwitchListener extends ExplorationListenerAdapter {
    
    private ThreadInfo previousThreadInfo;
    private LocationDesc previousThreadLocation;
    private List<String> contextSwitchInfo;
    
    //开始新的调度 ， 记录线程相关信息，记录 previousThreadInfo、contextSwitchInfo
    @Override
    public void startingSchedule() {
        previousThreadInfo = null;
        contextSwitchInfo = new ArrayList<String>();
    }
    
    //没当有线程切换的时候调用，记录线程切换信息
    @Override
    public void choiceMade(Object choice) {
        
        if (choice instanceof ThreadInfo) {
            ThreadInfo currentChoice = (ThreadInfo) choice;
            if (currentChoice != previousThreadInfo) {
                if (previousThreadInfo != null && previousThreadLocation != null) {
                    String switchFrom = "Switched after " + previousThreadInfo.getThread().getName() + " at "
                            + previousThreadLocation.getClassName() + " "  
                            + previousThreadLocation.getToLineNumber();
                    contextSwitchInfo.add(switchFrom);
                }
                String switchTo = "To " + 
                        currentChoice.getThread().getName()
                        //+ " at " + 
                        //currentChoice.getLocationDesc().getClassName() + " " + 
                        //currentChoice.getLocationDesc().getToLineNumber()
                        ;
                contextSwitchInfo.add(switchTo);
                previousThreadInfo = currentChoice;
               
            }
        }
    }
    
    //调度完成   将 previousThreadInfo 、 previousThreadLocation 置为空
    @Override
    public void completedSchedule(List<Integer> choicesMade) {
        previousThreadInfo = null;
        previousThreadLocation = null;
    }
    
    //检测到错误  输出线程交换信息
    @Override
    public void failureDetected(String errorMsg, List<Integer> choicesMade) {
        System.out.println("============================== CONTEXT SWITCH INFO ==============================");
        for (String s : contextSwitchInfo) {
            System.out.println(s);
        }
        System.out.println("============================================================");
    }
    
    // 有新的事件，更新 previousThreadLocation
    @Override
    public void afterEvent(EventDesc eventDesc) {
        ThreadInfo ti = Scheduler.getLiveThreadInfos().get(Thread.currentThread());
        previousThreadLocation = ti.getLocationDesc();
    }

}
