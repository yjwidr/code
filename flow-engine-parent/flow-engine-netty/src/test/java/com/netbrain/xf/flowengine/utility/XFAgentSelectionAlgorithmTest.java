package com.netbrain.xf.flowengine.utility;

import org.junit.Assert;
import org.junit.Test;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.xfcommon.XFCommon;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;

public class XFAgentSelectionAlgorithmTest {

    @Test
    public void test_XFAgentSelectionAlgorithm_Stability() throws Exception
    {
        List<XFAgent> listAgents = new ArrayList<XFAgent>();
        List<Long> listCpu = new ArrayList<Long>();

        XFAgent xfagen1 = new XFAgent();
        XFAgent xfagen2 = new XFAgent();
        XFAgent xfagen3 = new XFAgent();
        XFAgent xfagen4 = new XFAgent();
        XFAgent xfagen5 = new XFAgent();
        long cpu1 = 7 + 1 * XFCommon.AllowedDiffError_CPU;
        long cpu2 = 7 + 2 * XFCommon.AllowedDiffError_CPU;
        long cpu3 = 7 + 3 * XFCommon.AllowedDiffError_CPU;
        long cpu4 = 7 + 2 * XFCommon.AllowedDiffError_CPU;
        long cpu5 = 7 + 1 * XFCommon.AllowedDiffError_CPU - 1; // intentionally make it out the scope of AllowedDiffError

        xfagen1.setServerCpuPercent(cpu1);
        xfagen2.setServerCpuPercent(cpu2);
        xfagen3.setServerCpuPercent(cpu3);
        xfagen4.setServerCpuPercent(cpu4);
        xfagen5.setServerCpuPercent(cpu5);

        listAgents.add(xfagen1);
        listAgents.add(xfagen2);
        listAgents.add(xfagen3);
        listAgents.add(xfagen4);
        listAgents.add(xfagen5);
        listCpu.add(cpu1);
        listCpu.add(cpu2);
        listCpu.add(cpu3);
        listCpu.add(cpu4);
        listCpu.add(cpu5);

        int N = listAgents.size();
        for (int i =0; i < N; i++)
        {
            Long expectedCPU = listCpu.get(i);
            XFAgent currXFAgent = listAgents.get(i);
            Long currCPU = currXFAgent.getServerCpuPercent();
            Assert.assertEquals(expectedCPU, currCPU);
        }

        // Sort the list
        XFAgentSelectionAlgorithm algor = new XFAgentSelectionAlgorithm(XFCommon.TASK_PRIORITY_LOW, XFCommon.XFAgentSelectionAlgorithmType.ByLowerCPUAndHigherAvailablePhysicalMemoryInByte);
        // Before, it is 12, 17, 22, 17, 11
        // After, it is 12, 11, 17, 22, 17
        listAgents.sort(algor);
        for (int i =1; i < N; i++)
        {
            Long preCpu = listAgents.get((i-1)).getServerCpuPercent();
            Long currCPU = listAgents.get(i).getServerCpuPercent();

            Boolean sortIsStable = ((preCpu <= currCPU )|| (preCpu - currCPU <= XFCommon.AllowedDiffError_CPU));

            Assert.assertEquals(true, sortIsStable);
        }


    }

}
