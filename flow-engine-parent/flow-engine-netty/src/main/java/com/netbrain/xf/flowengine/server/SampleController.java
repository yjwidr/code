package com.netbrain.xf.flowengine.server;

import com.netbrain.ngsystem.model.FSCInfo;
import com.netbrain.xf.flowengine.metric.Metrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.netbrain.ngsystem.model.FrontServerController;
import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.background.StaleDTGChecker;
import com.netbrain.xf.flowengine.fscclient.FSCRepository;
import com.netbrain.xf.flowengine.fscclient.FSCRequest;
import com.netbrain.xf.flowengine.fscclient.NetlibClient;
import com.netbrain.xf.flowengine.scheduler.services.ISchedulerServices;

import java.util.List;
import java.util.ArrayList;

@Controller
@Configuration
public class SampleController {
	private static Logger logger = LogManager.getLogger(SampleController.class.getSimpleName());

	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private ISchedulerServices schedulerServices;
	
	@Autowired
	private Metrics metrics;
	
    @Autowired
    FSCRepository fscRepository;

	@RequestMapping("/")
	@ResponseBody
	String home() {
	    return "Please start the engine of XF";
	}

	@RequestMapping("/rabbit")
	@ResponseBody
	String catchTheRabbitUnderTheTree() {
		AMQPClient amqpClient = applicationContext.getBean(AMQPClient.class);
		return amqpClient.isRabbitCaught();
	}

	@RequestMapping("/queryDtg")
	@ResponseBody
	String queryDataTaskGroupStatus(@RequestParam(value = "dtgId") String dtgId) {
	    FrontServerController fsc = fscRepository.findFSCByTenantId(dtgId);
//		FrontServerController fsc = new FrontServerController();
//		fsc.setUsername("chendezhi");
//		fsc.setPassword("chendezhi");
//		fsc.setIpOrHostname("10.10.0.29");
//		fsc.setPort(9095);
//		fsc.setUseSSL(true);
		NetlibClient netlibClient = new NetlibClient(fsc, metrics);
		try {
			return netlibClient.sendCommand(FSCRequest.CMD_fsTaskgroupStatusReq, dtgId);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "error";
	}

	@RequestMapping("/stopDtg")
	@ResponseBody
	String stopDataTaskGroup(@RequestParam(value = "dtgId") String dtgId) {
		FrontServerController fsc = new FrontServerController();
		FSCInfo fscInfo = new FSCInfo();
		fscInfo.setUniqueName("default");
		fscInfo.setIpOrHostname("127.0.0.1");
		fscInfo.setUsername("admin");
		fscInfo.setPassword("R7IYNqU=");
		fscInfo.setPort(9095);
		List<FSCInfo> fscInfos = new ArrayList<FSCInfo>();
		fscInfos.add(fscInfo);
		fsc.setFscInfo(fscInfos);
		fsc.setActiveFSC("default");
		fsc.setUseSSL(false);
		NetlibClient netlibClient = new NetlibClient(fsc, metrics);
		try {
			return netlibClient.sendCommand(FSCRequest.CMD_fsStopDTGReq, dtgId);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "error";
	}

	@RequestMapping("/checkDtg")
	@ResponseBody
	String checkStaleDtgs() {
		StaleDTGChecker staleDTGChecker = applicationContext.getBean(StaleDTGChecker.class);
		staleDTGChecker.checkDTGLastUpdatedBefore(10);
		return "";
	}
	@RequestMapping("/sch")
	@ResponseBody
	String scheduler() {
	    return schedulerServices.getLastTimeNextTime();
	}
}