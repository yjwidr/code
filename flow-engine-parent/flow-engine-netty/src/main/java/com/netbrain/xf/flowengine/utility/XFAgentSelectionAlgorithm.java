package com.netbrain.xf.flowengine.utility;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.xfcommon.XFCommon;

import java.util.Comparator;

public class XFAgentSelectionAlgorithm implements Comparator {

    private static int CMP_agent1IsBetter = -1;
    private static int CMP_theyAreTheSame = 0;
    private static int CMP_agent2IsBetter = 1;

    private int algorithmType = XFCommon.XFAgentSelectionAlgorithmType.ByLowerCPUAndHigherAvailablePhysicalMemoryInByte;
    private int taskPriority = XFCommon.TASK_PRIORITY_LOW;
    public XFAgentSelectionAlgorithm(int taskPriorityInt, int algorithmTypeInt)
    {
        this.algorithmType = algorithmTypeInt;
        this.taskPriority = taskPriorityInt;
    }

    @Override
    public int compare(Object agent1, Object agent2) {
        XFAgent xfagent1 = (XFAgent) agent1;
        XFAgent xfAgent2 = (XFAgent) agent2;
        int ret = CMP_theyAreTheSame;
        if (this.algorithmType == XFCommon.XFAgentSelectionAlgorithmType.ByLowerCPUAndHigherAvailablePhysicalMemoryInByte)
        {
            ret = this.compareByLowerCPUAndHigherAvailablePhysicalMemory(xfagent1, xfAgent2);
        }
        else if (this.algorithmType == XFCommon.XFAgentSelectionAlgorithmType.ByLowerCPUAndLowerAvailablePhysicalMemoryPercentage)
        {
            ret = this.compareByLowerCPUAndLowerAvailablePhysicalMemoryPercentage(xfagent1, xfAgent2);
        }

        return ret;
    }

    private int compareByPerPriorityAvailableByte(XFAgent agent1, XFAgent agent2)
    {
        long agent1_avai = 0;
        long agent2_avai = 0;
        if (taskPriority == XFCommon.TASK_PRIORITY_SUPER)
        {
            agent1_avai = agent1.getP1AvailableByte();
            agent2_avai = agent2.getP1AvailableByte();
        }
        else if (taskPriority == XFCommon.TASK_PRIORITY_HIGH)
        {
            agent1_avai = agent1.getP2AvailableByte();
            agent2_avai = agent2.getP2AvailableByte();
        }
        else // treated as if (taskPrio == XFCommon.TASK_PRIORITY_LOW)
        {
            agent1_avai = agent1.getP3AvailableByte();
            agent2_avai = agent2.getP3AvailableByte();
        }

        if (agent1_avai - agent2_avai > XFCommon.AllowedDiffError_VirtualMemory)
        {
            return CMP_agent1IsBetter;
        }
        else if (agent2_avai - agent1_avai > XFCommon.AllowedDiffError_VirtualMemory)
        {
            return CMP_agent2IsBetter;
        }
        else
        {
            return CMP_theyAreTheSame;
        }

    }
    private int compareByLowerCPUAndHigherAvailablePhysicalMemory(XFAgent agent1, XFAgent agent2)
    {

        int cmpRes = this.compareByPerPriorityAvailableByte(agent1, agent2);
        if (cmpRes != CMP_theyAreTheSame)
        {
            return cmpRes;
        }

        // Agent1 CPU is much lower, so return Agent1
        if ((agent2.getServerCpuPercent() - agent1.getServerCpuPercent()) > XFCommon.AllowedDiffError_CPU)
        {
            return CMP_agent1IsBetter;
        }
        else if ((agent1.getServerCpuPercent() - agent2.getServerCpuPercent()) > XFCommon.AllowedDiffError_CPU)
        {
            return CMP_agent2IsBetter;
        }
        // CPU are regarded as equal because the diff is less than allowed error
        if ((agent1.getServerPhysicalAvailableMemoryInByte() - agent2.getServerPhysicalAvailableMemoryInByte()) > XFCommon.AllowedDiffError_AvailablePhysicalMemoryInByte )
        {
            return CMP_agent1IsBetter;
        }
        else if ((agent2.getServerPhysicalAvailableMemoryInByte() - agent1.getServerPhysicalAvailableMemoryInByte()) > XFCommon.AllowedDiffError_AvailablePhysicalMemoryInByte )
        {
            return CMP_agent2IsBetter;
        }
        else
        {
            return CMP_theyAreTheSame;
        }

        //return CMP_theyAreTheSame;
    }

    private int compareByLowerCPUAndLowerAvailablePhysicalMemoryPercentage(XFAgent agent1, XFAgent agent2)
    {
        int agent1IsBetter = -1;
        int theyAreTheSame = 0;
        int agent2IsBetter = 1;

        return theyAreTheSame;
    }
}
