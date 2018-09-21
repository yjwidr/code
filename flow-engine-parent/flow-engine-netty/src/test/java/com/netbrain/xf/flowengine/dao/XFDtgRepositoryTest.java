package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.xfcommon.XFCommon;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class XFDtgRepositoryTest {
    @Autowired
    private XFDtgRepository xfDtgRepository;
    @Before
    public void setUp() {
        xfDtgRepository.deleteAll();
    }

    @After
    public void after()
    {
        xfDtgRepository.deleteAll();
    }

    @Test
    public void testSave() throws Exception {
        xfDtgRepository.save(new XFDtg());
        Assert.assertEquals(1, xfDtgRepository.findAll().size());
    }

    @Test
    public void testFindById() throws Exception {
        String aUuid = UUID.randomUUID().toString();
        XFDtg xfdtg = new XFDtg();
        xfdtg.setId(aUuid);
        xfDtgRepository.save(xfdtg);
        Optional<XFDtg> foundXFDtg2 = xfDtgRepository.findById(aUuid);
        Assert.assertNotNull(foundXFDtg2.get());
    }


    @Test
    public void testfindRunningDtgsForDtgId() throws Exception {
        String dtgId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        xfDtg1.setId(dtgId);
        xfDtg1.setFinalTriggerReceived(false);

        xfDtgRepository.save(xfDtg1);
        List<XFDtg> foundXFDtgList = xfDtgRepository.findRunningDtgsForDtgId(dtgId);
        Assert.assertEquals(1, foundXFDtgList.size());

        xfDtg1.setFinalTriggerReceived(true);

        xfDtgRepository.save(xfDtg1);
        foundXFDtgList = xfDtgRepository.findRunningDtgsForDtgId(dtgId);
        Assert.assertEquals(0, foundXFDtgList.size());


    }

    @Test
    public void testfindRunningDtgsForTaskflowId_() throws Exception {
        String taskflowId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        xfDtg1.setTaskflowId(taskflowId);
        xfDtg1.setFinalTriggerReceived(false);

        xfDtgRepository.save(xfDtg1);
        List<XFDtg> foundXFDtgList = xfDtgRepository.findRunningDtgsForTaskflowId(taskflowId);
        Assert.assertEquals(1, foundXFDtgList.size());


        xfDtg1.setFinalTriggerReceived(true);

        xfDtgRepository.save(xfDtg1);
        foundXFDtgList = xfDtgRepository.findRunningDtgsForTaskflowId(taskflowId);
        Assert.assertEquals(0, foundXFDtgList.size());

    }



    @Test
    public void test_updateDtgStatusByJobIdOrTaskflowId_JobIdFlavor() throws Exception {
        String jobid = UUID.randomUUID().toString();
        String taskflowId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        String dtgid1 = UUID.randomUUID().toString();
        xfDtg1.setId(dtgid1);
        xfDtg1.setDtgStatus(XFCommon.DTGSTATUS_Running);
        xfDtg1.setTaskflowId(taskflowId);
        xfDtg1.setJobId(jobid);
        xfDtgRepository.save(xfDtg1);

        XFDtg xfDtg2 = new XFDtg();
        String dtgid2 = UUID.randomUUID().toString();
        xfDtg2.setId(dtgid2);
        xfDtg2.setDtgStatus(XFCommon.DTGSTATUS_Running);
        xfDtg2.setTaskflowId(taskflowId);
        xfDtg2.setJobId(jobid);
        xfDtgRepository.save(xfDtg2);

        List<XFDtg> foundXFDtgList = xfDtgRepository.findRunningDtgsForTaskflowId(taskflowId);
        Assert.assertEquals(2, foundXFDtgList.size());

        xfDtgRepository.updateDtgStatusByJobIdOrTaskflowId(jobid, XFCommon.DTGSTATUS_Canceled);

        Optional<XFDtg> dtgOpt1 = xfDtgRepository.findById(dtgid1);
        Assert.assertTrue(dtgOpt1.isPresent());

        XFDtg found_xfdtg1 = dtgOpt1.get();
        Assert.assertEquals(XFCommon.DTGSTATUS_Canceled, found_xfdtg1.getDtgStatus() );


        Optional<XFDtg> dtgOpt2 = xfDtgRepository.findById(dtgid1);
        Assert.assertTrue(dtgOpt2.isPresent());

        XFDtg found_xfdtg2 = dtgOpt2.get();
        Assert.assertEquals(XFCommon.DTGSTATUS_Canceled, found_xfdtg2.getDtgStatus() );

    }

    @Test
    public void test_updateDtgStatusByJobIdOrTaskflowId_TaskflowIdFlavor() throws Exception {
        String jobid = UUID.randomUUID().toString();
        String taskflowId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        String dtgid1 = UUID.randomUUID().toString();
        xfDtg1.setId(dtgid1);
        xfDtg1.setDtgStatus(XFCommon.DTGSTATUS_Running);
        xfDtg1.setTaskflowId(taskflowId);
        xfDtg1.setJobId(jobid);
        xfDtgRepository.save(xfDtg1);

        XFDtg xfDtg2 = new XFDtg();
        String dtgid2 = UUID.randomUUID().toString();
        xfDtg2.setId(dtgid2);
        xfDtg2.setDtgStatus(XFCommon.DTGSTATUS_Running);
        xfDtg2.setTaskflowId(taskflowId);
        xfDtg2.setJobId(jobid);
        xfDtgRepository.save(xfDtg2);

        List<XFDtg> foundXFDtgList = xfDtgRepository.findRunningDtgsForTaskflowId(taskflowId);
        Assert.assertEquals(2, foundXFDtgList.size());

        xfDtgRepository.updateDtgStatusByJobIdOrTaskflowId(taskflowId, XFCommon.DTGSTATUS_Canceled);

        Optional<XFDtg> dtgOpt1 = xfDtgRepository.findById(dtgid1);
        Assert.assertTrue(dtgOpt1.isPresent());

        XFDtg found_xfdtg1 = dtgOpt1.get();
        Assert.assertEquals(XFCommon.DTGSTATUS_Canceled, found_xfdtg1.getDtgStatus() );


        Optional<XFDtg> dtgOpt2 = xfDtgRepository.findById(dtgid1);
        Assert.assertTrue(dtgOpt2.isPresent());

        XFDtg found_xfdtg2 = dtgOpt2.get();
        Assert.assertEquals(XFCommon.DTGSTATUS_Canceled, found_xfdtg2.getDtgStatus() );

    }

    @Test
    public void testFindOverdueDtgsWithNullSubmitTime() throws Exception {
        String taskflowId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        String dtgid1 = UUID.randomUUID().toString();
        xfDtg1.setId(dtgid1);
        xfDtg1.setDtgStatus(XFCommon.DTGSTATUS_Running);
        xfDtg1.setTaskflowId(taskflowId);
        xfDtg1.setJobId(taskflowId);
        // without setting submitTime here
        xfDtgRepository.save(xfDtg1);

        List<XFDtg> foundXFDtgList = xfDtgRepository.findRunningDtgsSubmittedBefore(Instant.now(), new PageRequest(0, 100));
        Assert.assertEquals(0, foundXFDtgList.size());
    }

    @Test
    public void testFindOverdueDtgsWithRunningDtg() throws Exception {
        String taskflowId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        String dtgid1 = UUID.randomUUID().toString();
        xfDtg1.setId(dtgid1);
        xfDtg1.setDtgStatus(XFCommon.DTGSTATUS_Running);
        xfDtg1.setTaskflowId(taskflowId);
        xfDtg1.setJobId(taskflowId);

        Instant oneHourAgo = Instant.now();
        oneHourAgo.minusSeconds(3600);
        xfDtg1.setSubmitTime(oneHourAgo);
        xfDtgRepository.save(xfDtg1);

        List<XFDtg> foundXFDtgList = xfDtgRepository.findRunningDtgsSubmittedBefore(Instant.now(), new PageRequest(0, 100));
        Assert.assertEquals(1, foundXFDtgList.size());
        Assert.assertEquals(dtgid1, foundXFDtgList.get(0).getId());
    }

    @Test
    public void testFindOverdueDtgsWithCompletedDtg() throws Exception {
        String taskflowId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        String dtgid1 = UUID.randomUUID().toString();
        xfDtg1.setId(dtgid1);
        xfDtg1.setDtgStatus(XFCommon.DTGSTATUS_CompletedNormally);
        xfDtg1.setTaskflowId(taskflowId);
        xfDtg1.setJobId(taskflowId);
        xfDtgRepository.save(xfDtg1);

        List<XFDtg> foundXFDtgList = xfDtgRepository.findRunningDtgsSubmittedBefore(Instant.now(), new PageRequest(0, 100));
        Assert.assertEquals(0, foundXFDtgList.size());
    }

    @Test
    public void testUpdateFinalTriggerReceived() throws Exception {
        String dtgid1 = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        xfDtg1.setId(dtgid1);
        xfDtgRepository.save(xfDtg1);

        xfDtgRepository.updateFinalTriggerReceived(xfDtg1, true);
        XFDtg refetchedDtg = xfDtgRepository.findById(dtgid1).get();
        Assert.assertTrue(refetchedDtg.isFinalTriggerReceived());
        Assert.assertTrue(refetchedDtg.getDtgStatus() == XFCommon.DTGSTATUS_CompletedNormally);
    }

    @Test
    public void test_dropcollection() throws Exception {

        String taskflowId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        String dtgid1 = UUID.randomUUID().toString();
        xfDtg1.setId(dtgid1);
        xfDtg1.setDtgStatus(XFCommon.DTGSTATUS_CompletedNormally);
        xfDtg1.setTaskflowId(taskflowId);
        xfDtg1.setJobId(taskflowId);
        xfDtgRepository.save(xfDtg1);

        xfDtgRepository.dropCollection();

    }

}
