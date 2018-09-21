package com.netbrain.xf.flowengine.scheduler.services;

import static com.mongodb.client.model.Filters.eq;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.netbrain.xf.flowengine.scheduler.TaskJob;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.flowengine.utility.CSharpToJavaTimezoneMapper;

@Service
public class SchedulerServicesImpl implements ISchedulerServices {
 
    private static Logger logger = LogManager.getLogger(SchedulerServicesImpl.class.getName());
    private List<String> preJobIds=new ArrayList<String>();
    public static final String COLLECTION_NAME="JobDef";
    private final String BY_WEEK="byWeek";
    private final String BY_DAY="byDay";
    private final String BY_MONTH="byMonth";
    private final String BY_HOUR="byHour";
    private final String RUN_ONCE="runOnce";

    private Lock lock = new ReentrantLock();

    @Resource(name="ngsystem")
    private MongoTemplate ngsystemTemplate;
    
    private MongoDatabase mongoDatabase;
    
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private CSharpToJavaTimezoneMapper timezoneMapper;
    
    @Autowired
    TaskController taskController;

    @PostConstruct
    public void setDb() {
        mongoDatabase = ngsystemTemplate.getMongoDbFactory().getDb();
        logger.debug("dbName="+mongoDatabase.getName());
    }

    public List<JobDetail> getJobDetails(String jobId) {
        List<JobDetail> jobDetails = new ArrayList<>();
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobId));
            for (JobKey jobKey : jobKeys) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                if (jobDetail != null) {
                    jobDetails.add(jobDetail);
                }
            }
        } catch (SchedulerException e) {
            logger.error("Failed to retrieve jobDetail from quartz", e);
        }
        return jobDetails;
    }

    protected Map<String, JobDataMap> getJobDataMapByJobId(String jobId) {
        Map<String, JobDataMap> map = new ConcurrentHashMap<String,JobDataMap>();
        try {
        FindIterable<Document> iteratble = mongoDatabase.getCollection(COLLECTION_NAME).find(eq("job.jobId", jobId));
        setJobDataMap(map, iteratble);
        }catch (Exception e) {
            logger.error("Failed to getJobDataMapByJobId by jobId: "+jobId, e);
        }
        return map;
    }

    @Override
    public Map<String, JobDataMap> getJobDataMap() {
    	Map<String, JobDataMap> map = new ConcurrentHashMap<String,JobDataMap>();
        try {
        FindIterable<Document> iteratble = mongoDatabase.getCollection(COLLECTION_NAME).find();
        setJobDataMap(map, iteratble);
        }catch(Exception e) {
            logger.error("Failed to getJobDataMap: ", e);
        }
        return map;
    }

    private void setJobDataMap(Map<String,JobDataMap> map, FindIterable<Document> iteratble) {
        if(!ObjectUtils.isEmpty(iteratble)) {
            for (Document document : iteratble) {
                JobDataMap jobDataMap = new JobDataMap();
                setJobParameter(document, jobDataMap);
                if(!ObjectUtils.isEmpty(document) && document.containsKey("job")) {
                    Document job = document.get("job", Document.class);
                    if(!ObjectUtils.isEmpty(job)) {
                        if(job.containsKey("scheduleTime")) {
                            Document scheduleTime = job.get("scheduleTime", Document.class);
                            if(!ObjectUtils.isEmpty(scheduleTime)) {
                                Document frequency=scheduleTime.get("frequency", Document.class);
                                int type = frequency.getInteger("type");
                                if(type==0) {
                                    setJobScheduleData(job, scheduleTime, frequency, jobDataMap, RUN_ONCE);
                                }else if(type==2){
                                    setJobScheduleData(job, scheduleTime, frequency, jobDataMap, BY_HOUR);
                                }else if(type==3){
                                    setJobScheduleData(job, scheduleTime, frequency, jobDataMap, BY_DAY);
                                }else if(type==4){
                                    setJobScheduleData(job, scheduleTime, frequency, jobDataMap, BY_WEEK);
                                }else if(type==5){
                                    setJobScheduleData(job, scheduleTime, frequency, jobDataMap, BY_MONTH);
                                }
                            }
                        }
                    }
                    map.put(job.getString("jobId"), jobDataMap);
                }
            }
        }
    }

    private void setJobParameter(Document document, JobDataMap jobDataMap) {
        if(document.containsKey("jobId")) {
            jobDataMap.put("jobId", document.getString("jobId"));
        }
        if(document.containsKey("jobType")) {
            jobDataMap.put("jobType", document.getString("jobType"));
        }
        if(document.containsKey("ShortDescription")) {
            jobDataMap.put("ShortDescription", document.getString("ShortDescription"));
        }
        if(document.containsKey("WorkerRestartTimes")) {
            jobDataMap.put("WorkerRestartTimes", document.getInteger("WorkerRestartTimes"));
        }
        if(document.containsKey("needBroadCallbackToAllApiServer")) {
            jobDataMap.put("needBroadCallbackToAllApiServer", document.getBoolean("needBroadCallbackToAllApiServer",false));
        }
        if(document.containsKey("jobRunCategory")) {
            jobDataMap.put("jobRunCategory", document.getString("jobRunCategory"));
        }
        if(document.containsKey("userName")) {
            jobDataMap.put("userName", document.getString("userName"));
        }
        if(document.containsKey("userIPAddress")) {
            jobDataMap.put("userIPAddress", document.getString("userIPAddress"));
        }
        if(document.containsKey("domainId")) {
            jobDataMap.put("domainId", document.getString("domainId"));
        }
        if(document.containsKey("domainDbName")) {
            jobDataMap.put("domainDbName", document.getString("domainDbName"));
        }
        if(document.containsKey("tenantId")) {
            jobDataMap.put("tenantId", document.getString("tenantId"));
        }
        if(document.containsKey("tenantDbName")) {
            jobDataMap.put("tenantDbName", document.getString("tenantDbName"));
        }
        if(document.containsKey("priority")) {
            jobDataMap.put("priority", document.getInteger("priority"));
        }
        if(document.containsKey("callbackqueue")) {
            jobDataMap.put("callbackqueue", document.getString("callbackqueue"));
        }
        if(document.containsKey("start")) {
            jobDataMap.put("start", document.getDate("start"));
        }
        if(document.containsKey("parameters")) {
            jobDataMap.put("parameters", document.getString("parameters"));
        }
        if(document.containsKey("active")) {
            jobDataMap.put("active", document.getBoolean("active"));
        }
        if(document.containsKey("stopOldTaskAtNextRun")) {
            jobDataMap.put("stopOldTaskAtNextRun", document.getBoolean("stopOldTaskAtNextRun"));
        }
        if(document.containsKey("timeZone")) {
            jobDataMap.put("timeZone", document.getString("timeZone"));
        }
        if(document.containsKey("enableEnd")) {
            jobDataMap.put("enableEnd", document.getBoolean("enableEnd", false));
            if(document.containsKey("end")) {
                jobDataMap.put("end", document.getDate("end")); 
            }
        }
    }

    private void setJobScheduleData(Document job, Document scheduleTime, Document frequency, JobDataMap jobDataMap, String type) {
        List<Integer> dayList = null;
        Integer interval = 0;
        Integer startTime = 0;
        Integer[] days = new Integer[]{};
        Integer[] startTimesArray = new Integer[]{};
        Integer[] endTimesArray = new Integer[]{};
        List<Integer> startTimesList = new ArrayList<Integer>();
        List<Integer> endTimesList = new ArrayList<Integer>();
        if(!ObjectUtils.isEmpty(frequency) && frequency.containsKey(type)) {
            frequency = frequency.get(type, Document.class);
            setJobParameter(job, jobDataMap);
            setJobParameter(scheduleTime, jobDataMap);
            jobDataMap.put("frequency", type);
            if(!ObjectUtils.isEmpty(frequency) ) {
                // Parse the timeRange value for monthly, weekly and daily schedule
                if(type.equals(BY_WEEK) || type.equals(BY_MONTH) || type.equals(BY_DAY)) {
                    if(frequency.containsKey("timeRange")) {
                        List<Document> timeRangeList = frequency.get("timeRange",ArrayList.class);
                        if(!ObjectUtils.isEmpty(timeRangeList)) {
                            for(Document timeRange: timeRangeList) {
                                startTimesList.add(timeRange.getInteger("startTime"));
                                endTimesList.add(timeRange.getInteger("endTime"));
                            }
                        }
                        startTimesArray = startTimesList.toArray(startTimesArray);
                        endTimesArray = endTimesList.toArray(endTimesArray);
                        jobDataMap.put("startTime", startTimesArray);
                        jobDataMap.put("endTime", endTimesArray);
                        startTimesList.clear();
                    }
                }

                // Parse day and month values from weekly and monthly schedule
                // i.e. 2 means 2nd day of a week
                if(type.equals(BY_WEEK) || type.equals(BY_MONTH)) {
                    if(type.equals(BY_WEEK) && frequency.containsKey("day")) {
                        dayList = frequency.get("day",ArrayList.class);
                    }else if(type.equals(BY_MONTH) && frequency.containsKey("month")) {
                        dayList = frequency.get("month",ArrayList.class);
                    }
                    days = dayList.toArray(days);
                    if(type.equals(BY_WEEK)) {
                        jobDataMap.put("day", days);
                    } else if(type.equals(BY_MONTH)) {
                        jobDataMap.put("month", days);
                    }
                }

                if(type.equals(BY_WEEK) || type.equals(BY_MONTH) || type.equals(BY_DAY) || type.equals(BY_HOUR) || type.equals(RUN_ONCE)) {
                    if(type.equals(BY_HOUR) || type.equals(RUN_ONCE)) {
                        startTime = frequency.containsKey("startTime") ? frequency.getInteger("startTime") : 0;
                        jobDataMap.put("startTime", startTime);
                    }
                    if(type.equals(BY_WEEK) || type.equals(BY_DAY) || type.equals(BY_HOUR)) {
                        interval = frequency.containsKey("interval") ? frequency.getInteger("interval") : 0;
                        jobDataMap.put("interval", interval);
                    }

                    // The day value in a monthly schedule is a singular value, user can only select one day of
                    // a month to run a task
                    if(type.equals(BY_MONTH)) {
                        Integer dayInMonth = frequency.containsKey("day") ? frequency.getInteger("day") : 0;
                        jobDataMap.put("day", dayInMonth);
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteJob(String jobId) {
        boolean bl = false;
        try {
            Set<JobKey> set=scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobId));
            List<JobKey> listJobKeys = new ArrayList<JobKey>(set);
            bl = scheduler.deleteJobs(listJobKeys);
            logger.debug("deleteJob for jobId={}",jobId);
        } catch (SchedulerException e) {
            logger.error("error: delete by groupname=" + jobId, e);
        }
        return bl;
    }

    @Override
    public void makeScheduler(JobDataMap jobDataMap) {
        try {
            JobDataMap oldJobDataMap = null;
            Integer startTime = null;
            Integer oldStartTime = null;
            Integer[] startTimes = null;
            Integer[] oldStartTimes = null;
            Date oldEnd = null;
            LocalDate oldStartL = null;  
            Object oOldStartTime;
            Integer oldInterval = null;
            String oldFrequency = null;
            Date oldStart;
            String mtl = "-";
            StringBuilder sb = new  StringBuilder();
            String jobId = jobDataMap.getString("jobId");
            String frequency = jobDataMap.getString("frequency");
            Date start = (Date)jobDataMap.get("start");
            Date end = (Date)jobDataMap.get("end");
            Integer interval = null;
            if(jobDataMap.containsKey("interval")) {
                interval = jobDataMap.getInt("interval");
            }
            Object oStartTime = jobDataMap.get("startTime");
            boolean enableEnd = jobDataMap.getBoolean("enableEnd");
            boolean oldEnableEnd = false;
            boolean result = false;
            if(oStartTime instanceof Integer) {
                startTime = (Integer) oStartTime;
            }else if(oStartTime instanceof Integer[]) {
                startTimes = (Integer[])oStartTime;
            }
            Set<JobKey> setJobKey = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobId));
            List<JobKey> listJobKey = new CopyOnWriteArrayList<JobKey>(setJobKey);
            for (JobKey jobKey : listJobKey) {
                if(jobKey.getName().contains("nodelete")) {
                    listJobKey.remove(jobKey);
                }
            }
            ZoneId timeZoneId = timezoneMapper.getZoneFromCSharpName(jobDataMap.getString("timeZone"));
            LocalDate startL = ZonedDateTime.ofInstant((start).toInstant(), timeZoneId).toLocalDate();
            if(listJobKey.size() != 0) {
                // all jobDataMaps from the same JobID have the same content, so using the first one here
                oldJobDataMap = scheduler.getJobDetail(listJobKey.get(0)).getJobDataMap();
                oldFrequency = oldJobDataMap.getString("frequency");
                oldStart = (Date)oldJobDataMap.get("start");
                oldEnd = (Date)oldJobDataMap.get("end");
                oldStartL = ZonedDateTime.ofInstant((oldStart).toInstant(), timeZoneId).toLocalDate();
                oOldStartTime = oldJobDataMap.get("startTime");
                oldEnableEnd = oldJobDataMap.getBoolean("enableEnd");
                if(oldJobDataMap.containsKey("interval")) {
                    oldInterval = oldJobDataMap.getInt("interval");
                }
                if(oOldStartTime instanceof Integer) {
                    oldStartTime = (Integer) oOldStartTime;
                }else if(oOldStartTime instanceof Integer[]) {
                    oldStartTimes = (Integer[])oOldStartTime;
                }
                if(!oldFrequency.equals(frequency)){
                    // delete the old quartz job if the type of a job is changed, i.e. from daily to weekly
                    result = deleteJobKeys(listJobKey);
                }
            }

            startTime= ObjectUtils.isEmpty(startTime) ? 0 : startTime;
            oldStartTime = ObjectUtils.isEmpty(oldStartTime) ? 0 : oldStartTime;
            interval = ObjectUtils.isEmpty(interval) ? 0 : interval;
            oldInterval = ObjectUtils.isEmpty(oldInterval) ? 0 : oldInterval;
            
            if(listJobKey.size()==0) {//new
                if(frequency.equals("runOnce")) {
                    runOnce(jobDataMap, startL,null);
                }else if(frequency.equals("byHour")) {
                    byHour(jobDataMap, startL,null);
                }else if(frequency.equals("byDay")) {
                    byDay(jobDataMap, startL,null);
                }else if(frequency.equals("byWeek")) {
                    byWeek(jobDataMap, startL,null);
                }else if(frequency.equals("byMonth")) {
                    byMonth(jobDataMap, startL,null);
                }
            }else if(listJobKey.size()>0){//edit
                if(frequency.equals("runOnce")) {
                    if(!oldStartL.isEqual(startL) || startTime.intValue()!=oldStartTime.intValue()) {
                        if(!result) {
                            deleteJobKeys(listJobKey);
                        }
                        runOnce(jobDataMap,startL,listJobKey);
                    }
                }else if(frequency.equals("byHour")) {
                    if (!oldStartL.isEqual(startL) || startTime.intValue() != oldStartTime.intValue() || enableEnd!=oldEnableEnd || enableEnd && oldEnableEnd && end.getTime() != oldEnd.getTime() || interval.intValue()!=oldInterval.intValue()) {
                        if(!result) {
                            deleteJobKeys(listJobKey);
                        }
                        byHour(jobDataMap,startL,listJobKey);
                    }
                }else if(frequency.equals("byDay")) {
                    if(!oldStartL.isEqual(startL) || enableEnd!=oldEnableEnd || enableEnd  && oldEnableEnd && end.getTime() != oldEnd.getTime() || interval.intValue()!=oldInterval.intValue()){
                        if(!result) {
                            deleteJobKeys(listJobKey);
                        }
                        byDay(jobDataMap,startL,null);
                    }else {
                        oldStartTimes = ObjectUtils.isEmpty(oldStartTimes)==true?new Integer[] {}:oldStartTimes;
                        startTimes = ObjectUtils.isEmpty(startTimes)==true?new Integer[] {}:startTimes;
                        List<Integer> increasedStartTimeList = compare(oldStartTimes,startTimes);
                        List<Integer> reducedStartTimeList = compare(startTimes,oldStartTimes);
                        byDay(jobDataMap, startL, increasedStartTimeList);
                        List<JobKey> listReducedJobKey=new ArrayList<JobKey>();
                        for(int starTime:reducedStartTimeList) {
                            sb.setLength(0);
                            sb.append(jobId).append(mtl).append(frequency).append(mtl).append(interval).append(mtl).append(startL.toString()).append(mtl).append(starTime);
                            setEndDate(sb, end, start, enableEnd, mtl);
                            listReducedJobKey.add(new JobKey(sb.toString(),jobId));
                        }
                        if(listReducedJobKey.size()>0) {
                            deleteJobKeys(listReducedJobKey);
                        }
                   }
                }else if(frequency.equals("byWeek")) {
                    Integer[] whatDays=(Integer[])jobDataMap.get("day");
                    whatDays=ObjectUtils.isEmpty(whatDays)==true?new Integer[] {}:whatDays;
                    if(oldFrequency.equals(frequency)) {
                        Integer[] oldWhatDays=(Integer[])oldJobDataMap.get("day");
                        oldWhatDays=ObjectUtils.isEmpty(oldWhatDays)==true?new Integer[] {}:oldWhatDays;
                        if(!oldStartL.isEqual(startL) || enableEnd!=oldEnableEnd || enableEnd  && oldEnableEnd && end.getTime() != oldEnd.getTime() || interval.intValue()!=oldInterval.intValue()){
                            if(!result) {
                                deleteJobKeys(listJobKey);
                            }
                            byWeek(jobDataMap,startL,null);
                        }else {
                            List<Integer> increasedWhatDayList=compare(oldWhatDays,whatDays);
                            List<Integer> reducedWhatDayList=compare(whatDays,oldWhatDays);
                            byWeek(jobDataMap,startL,increasedWhatDayList);
                            List<JobKey> listReducedJobKey=new ArrayList<JobKey>();
                            for(int whatDay:reducedWhatDayList) {
                                sb.setLength(0);
                                sb.append(jobId).append(mtl).append(frequency).append(mtl).append(interval).append(mtl).append(startL.toString()).append(mtl).append(whatDay).append(mtl).append(startTimes[0]);
                                setEndDate(sb ,end ,start,enableEnd,mtl);
                                listReducedJobKey.add(new JobKey(sb.toString(),jobId));
                            }
                            if(listReducedJobKey.size()>0) {
                                deleteJobKeys(listReducedJobKey);
                            }
                       }
                   }else {
                       byWeek(jobDataMap,startL,null);
                   }
                }else if(frequency.equals("byMonth")) {
                    Integer day=jobDataMap.getInt("day");
                    Integer [] months=(Integer[])jobDataMap.get("month");
                    months=ObjectUtils.isEmpty(months)==true?new Integer[] {}:months;
                    if(oldFrequency.equals(frequency)) {
                        Integer oldDay=oldJobDataMap.getInt("day");
                        Integer [] oldMonths=(Integer[])oldJobDataMap.get("month");
                        oldMonths=ObjectUtils.isEmpty(oldMonths)==true?new Integer[] {}:oldMonths;
                        if(!oldStartL.isEqual(startL) || enableEnd!=oldEnableEnd || enableEnd  && oldEnableEnd && end.getTime() != oldEnd.getTime() || day.intValue()!=oldDay.intValue() ){
                            if(!result) {
                                deleteJobKeys(listJobKey);
                            }
                            byMonth(jobDataMap,startL,null);
                        }else {
                            List<Integer> increasedMonthList=compare(oldMonths,months);
                            List<Integer> reducedMonthList=compare(months,oldMonths);
                            byMonth(jobDataMap,startL,increasedMonthList);
                            List<JobKey> listReducedJobKey=new ArrayList<JobKey>();
                            for(int whatMonth:reducedMonthList) {
                                sb.setLength(0);
                                sb.append(jobId).append(mtl).append(frequency).append(mtl).append(day).append(mtl).append(startL.toString()).append(mtl).append(whatMonth).append(mtl).append(startTimes[0]);
                                setEndDate(sb ,end ,start,enableEnd,mtl);
                                listReducedJobKey.add(new JobKey(sb.toString(),jobId));
                            }
                            if(listReducedJobKey.size()>0) {
                                deleteJobKeys(listReducedJobKey);
                            }
                        }
                    }else {
                        byMonth(jobDataMap,startL,null);
                    }
                }
            }
        } catch (SchedulerException e) {
            logger.error("Failed to convert to quartz schedule", e);
        }
    }

    private <T> List<T> compare(T[] arrayPre, T[] arrayNext) {    
        List<T> listPre = Arrays.asList(arrayPre);    
        List<T> listNext = new ArrayList<T>();    
        for (T t : arrayNext) {    
            if (!listPre.contains(t)) {    
                listNext.add(t);    
            }    
        }    
        return listNext;    
    }

    private Date startDate(Date start, int startTime, String csharpTimezone) {
        String mtl="-";
        String space=" ";
        String colon=":";
        StringBuffer cornBuilder=new StringBuffer();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        cornBuilder.append(calendar.get(Calendar.YEAR)).append(mtl).append(calendar.get(Calendar.MONTH)+1).append(mtl).append(calendar.get(Calendar.DAY_OF_MONTH)).append(space);
        LocalTime time=LocalTime.ofSecondOfDay(startTime);
        cornBuilder.append(time.getHour()).append(colon).append(time.getMinute()).append(colon).append(time.getSecond());
        return transferDate(cornBuilder.toString(), csharpTimezone);
    }

    private Date transferDate(String startData, String csharpTimezone) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-M-d H:m:s");
        LocalDateTime  localDateTime = LocalDateTime.parse(startData, f);
        ZoneId zoneId = timezoneMapper.getZoneFromCSharpName(csharpTimezone);
        ZonedDateTime zdt = localDateTime.atZone(zoneId);
        Date date = Date.from(zdt.toInstant());
        return date;
    }

    @Override
    public List<String> loopScheduler(Logger logger) {
        List<String> groupNames = null;
        try {
            groupNames=scheduler.getJobGroupNames();
            for (String groupName : groupNames) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                 String jobName = jobKey.getName();
                 List<Trigger> list =(List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                 Date previousFireTime = null;
                 Date nextFireTime = null;
                 Date startTime=null;
                 Date endTime=null;
                 if(!ObjectUtils.isEmpty(list)) {
                     for(Trigger trigger:list) {
                         startTime=trigger.getStartTime();
                         endTime=trigger.getEndTime();
                         previousFireTime =trigger.getPreviousFireTime();
                         nextFireTime =trigger.getNextFireTime();
                         logger.debug("jobName:{} previousFireTime:{} nextFireTime:{} startTime: {} endTime: {}", jobName,previousFireTime,nextFireTime,startTime,endTime);
                         }
                     }
                }
            }
        } catch (SchedulerException e) {
            logger.error("Failed to loop through scheduled jobs", e);
        }
        return groupNames==null?new ArrayList<String>():groupNames;
    }
 
    private boolean runNow(JobDataMap jobDataMap) {
        JobDetail jobDetail=null;
        String strJobKey=null;
        StringBuilder sb = new StringBuilder();
        String jobId=jobDataMap.getString("jobId");
        String frequency=jobDataMap.getString("frequency");
        String mtl="-";
        JobKey jobKey=null;
        try {
            sb.append(jobId).append(mtl).append(frequency);
            if(jobDataMap.containsKey("nodelete")) {
                sb.append(mtl).append("nodelete");
            }
            strJobKey=sb.toString();
            jobKey=new JobKey(strJobKey,jobId);
            jobDetail = JobBuilder.newJob(TaskJob.class).withIdentity(jobKey).storeDurably().usingJobData(jobDataMap).build();
            if(scheduler.checkExists(jobDetail.getKey())){
                if(!jobDataMap.containsKey("nodelete")) {
                    scheduler.deleteJob(jobDetail.getKey());
                }else {
                    logger.debug("jobkey is===={}",jobDetail.getKey().getName());
                    return false;
                }
            }
            scheduler.addJob(jobDetail, true);
            scheduler.triggerJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            logger.error("Failed to schedule jobid: " + jobId + ", jobKey: " + strJobKey, e);
            return false;
        }
    }
    
    private void runOnce(JobDataMap jobDataMap,LocalDate startL,List<JobKey> listJobKey) {
        Date start=(Date)jobDataMap.get("start");
        Integer startTime=jobDataMap.getInt("startTime");
        if(startDate(start, startTime, jobDataMap.getString("timeZone")).before(new Date())) {
            return; 
        }
        Trigger trigger=null;
        JobDetail jobDetail=null;
        String strJobKey=null;
        StringBuilder sb = new StringBuilder();
        String jobId=jobDataMap.getString("jobId");
        String frequency=jobDataMap.getString("frequency");
        TriggerBuilder<Trigger> triggerBuilder=null;
        Date end=(Date)jobDataMap.get("end");
        boolean enableEnd=jobDataMap.getBoolean("enableEnd");
        Date startLocal =startDate(start,startTime, jobDataMap.getString("timeZone"));
        String mtl="-";
        try {
            sb.append(jobId).append(mtl).append(frequency).append(mtl).append(startL.toString()).append(mtl).append(startTime);
            setEndDate(sb ,end ,start,enableEnd,mtl);
            strJobKey=sb.toString();
            jobDetail = JobBuilder.newJob(TaskJob.class).withIdentity(strJobKey,jobId).storeDurably().usingJobData(jobDataMap).build();
            triggerBuilder= TriggerBuilder.newTrigger().withIdentity(strJobKey,jobId).startAt(startLocal);
            triggerBuilder.withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount());
            trigger=triggerBuilder.build();                             
            Date date = scheduler.scheduleJob(jobDetail, trigger);
            date.getTime();
        } catch (SchedulerException e) {
            logger.error("Failed to schedule jobid: " + jobId + ", jobKey: " + strJobKey, e);
        }
    }

    private void byHour(JobDataMap jobDataMap,LocalDate startL,List<JobKey> listJobKey) {
        boolean enableEnd=jobDataMap.getBoolean("enableEnd");
        Date end=(Date)jobDataMap.get("end");
        if(enableEnd && end.before(new Date())) {
            return; 
        }
        String strJobKey=null;
        StringBuilder sb = new StringBuilder();
        Trigger trigger=null;
        JobDetail jobDetail=null;
        Integer startTime=jobDataMap.getInt("startTime");
        String jobId=jobDataMap.getString("jobId");
        String frequency=jobDataMap.getString("frequency");
        TriggerBuilder<Trigger> triggerBuilder=null;
        Date start=(Date)jobDataMap.get("start");
        Date startLocal =startDate(start, startTime, jobDataMap.getString("timeZone"));
        String mtl="-";
        Integer interval=jobDataMap.getInt("interval");
        try {
            sb.append(jobId).append(mtl).append(frequency).append(mtl).append(interval).append(mtl).append(startL.toString()).append(mtl).append(startTime);
            setEndDate(sb ,end ,start,enableEnd,mtl);
            strJobKey=sb.toString();
            jobDetail = JobBuilder.newJob(TaskJob.class).withIdentity(strJobKey,jobId).storeDurably().usingJobData(jobDataMap).build();
            triggerBuilder= TriggerBuilder.newTrigger().withIdentity(strJobKey,jobId).startAt(startLocal);
            triggerBuilder.withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionNextWithRemainingCount().withIntervalInHours(interval).repeatForever());
            setTriggerBuilderForEndDate(end ,start,enableEnd,triggerBuilder);
            trigger=triggerBuilder.build();                             
            Date date = scheduler.scheduleJob(jobDetail, trigger);
            date.getTime();
        } catch (SchedulerException e) {
            logger.error("Failed to schedule jobid: " + jobId + ", jobKey: " + strJobKey, e);
        }
    }

    private void byDay(JobDataMap jobDataMap,LocalDate startL,List<Integer> increasedStartTimeList) {
        boolean enableEnd=jobDataMap.getBoolean("enableEnd");
        Date end=(Date)jobDataMap.get("end");
        if(enableEnd && end.before(new Date())) {
            return; 
        }
        String strJobKey=null;
        Trigger trigger=null;
        JobDetail jobDetail=null;
        StringBuilder sb = new StringBuilder();
        Integer[] startTimes=(Integer[])jobDataMap.get("startTime");
        Integer[] endTimes=(Integer[])jobDataMap.get("endTime");
        Integer endTime=null;
        Integer startTime=null;
        Date curDateTime =null;
        Date endDateTime =null;
        Date startDateTime =null;
        String jobId=jobDataMap.getString("jobId");
        String frequency=jobDataMap.getString("frequency");
        TriggerBuilder<Trigger> triggerBuilder=null;
        Date start=(Date)jobDataMap.get("start");
        String mtl="-";
        Integer interval=jobDataMap.getInt("interval");
        if(!ObjectUtils.isEmpty(increasedStartTimeList)) {
            startTimes = new Integer[] {};
            startTimes=increasedStartTimeList.toArray(startTimes);
        }
        for(int starTime:startTimes) {
            sb.setLength(0);
            sb.append(jobId).append(mtl).append(frequency).append(mtl).append(interval).append(mtl).append(startL.toString()).append(mtl).append(starTime);
            Date startLocal =startDate(start, starTime, jobDataMap.getString("timeZone"));
            setEndDate(sb ,end ,start,enableEnd,mtl);
            strJobKey=sb.toString();
            jobDetail = JobBuilder.newJob(TaskJob.class).withIdentity(strJobKey,jobId).storeDurably().usingJobData(jobDataMap).build();
            triggerBuilder= TriggerBuilder.newTrigger().withIdentity(strJobKey,jobId);
            try {
                if(!taskController.hasAnyUnfinishedTaskflowsForJobId(jobId)) {
                    if (!isJobTriggeredFinished(jobDetail)) {
                        Date previousFireTime = getPreviousFireTime(jobDetail);
                        if (previousFireTime == null) {
                            if (interval.intValue() == 1 && startTimes.length == 1) {
                                if (!ObjectUtils.isEmpty(endTimes) && endTimes.length == 1) {
                                    endTime = endTimes[0];
                                    if (!ObjectUtils.isEmpty(endTime)) {
                                        curDateTime = new Date();
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.setTime(curDateTime);
                                        startTime = calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
                                        if (startTime >= startTimes[0]) {
                                            startDateTime = startDate(curDateTime, startTime, jobDataMap.getString("timeZone"));
                                        } else {
                                            startDateTime = startDate(curDateTime, startTimes[0], jobDataMap.getString("timeZone"));
                                        }
                                        endDateTime = startDate(curDateTime, endTime, jobDataMap.getString("timeZone"));
                                        ZoneId timeZoneId = timezoneMapper.getZoneFromCSharpName(jobDataMap.getString("timeZone"));
                                        LocalDate startLD = ZonedDateTime.ofInstant((startDateTime).toInstant(), timeZoneId).toLocalDate();
                                        if (startTimes[0] < endTime) {
                                            if (startDateTime.after(start) && endDateTime.before(end) && curDateTime.after(startDateTime) && curDateTime.before(endDateTime) && (startLD.isAfter(startL) || startLD.isEqual(startL))) {
                                                jobDataMap.put("nodelete", "no");
                                                runNow(jobDataMap);
                                            }
                                        } else {
                                            if (startTime >= startTimes[0]) {
                                                if (startDateTime.after(start) && endDateTime.before(end) && curDateTime.after(startDateTime) && (startLD.isAfter(startL) || startLD.isEqual(startL))) {
                                                    jobDataMap.put("nodelete", "no");
                                                    runNow(jobDataMap);
                                                }
                                            } else {
                                                if (startDateTime.after(start) && endDateTime.before(end) && curDateTime.before(endDateTime) && (startLD.isAfter(startL) || startLD.isEqual(startL))) {
                                                    jobDataMap.put("nodelete", "no");
                                                    runNow(jobDataMap);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.debug("jobId {} has been triggered by scheduler ", jobId);
                        }
                    } else {
                        logger.debug("jobId {} has been triggered completely by scheduler ", jobId);
                    }
                }else {
                    logger.debug("jobId {} is running " ,jobId);
                }
                if(!scheduler.checkExists(jobDetail.getKey())){
                    triggerBuilder.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInDays(interval).withMisfireHandlingInstructionDoNothing());
                    if(startLocal.before(start)) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(startLocal);
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        startLocal=calendar.getTime();
                    }
                    triggerBuilder.startAt(startLocal);
                    setTriggerBuilderForEndDate(end ,start,enableEnd,triggerBuilder);
                    trigger=triggerBuilder.build();
                    Date date = scheduler.scheduleJob(jobDetail, trigger);
                    date.getTime();
                }
            } catch (SchedulerException e) {
                logger.error("Failed to schedule jobid: " + jobId + ", jobKey: " + strJobKey, e);
            }
        }
    }

    private void byWeek(JobDataMap jobDataMap,LocalDate startL,List<Integer> increasedWhatDayList) {
        boolean enableEnd=jobDataMap.getBoolean("enableEnd");
        Date end=(Date)jobDataMap.get("end");
        if(enableEnd && end.before(new Date())) {
            return; 
        }
        String strJobKey=null;
        Trigger trigger=null;
        JobDetail jobDetail=null;
        StringBuilder sb = new StringBuilder();
        Integer[] startTimes=(Integer[])jobDataMap.get("startTime");
        String jobId=jobDataMap.getString("jobId");
        String frequency=jobDataMap.getString("frequency");
        TriggerBuilder<Trigger> triggerBuilder=null;
        Date start=(Date)jobDataMap.get("start");
        String mtl="-";
        Integer interval=jobDataMap.getInt("interval");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        int currentWhatDay= calendar.get(Calendar.DAY_OF_WEEK);
        Integer[] whatDays=(Integer[])jobDataMap.get("day");
        if(!ObjectUtils.isEmpty(increasedWhatDayList)) {
            whatDays = new Integer[] {};
            whatDays=increasedWhatDayList.toArray(whatDays);
        }
        for(int whatDay:whatDays){
            sb.setLength(0);
            sb.append(jobId).append(mtl).append(frequency).append(mtl).append(interval).append(mtl).append(startL.toString()).append(mtl).append(whatDay).append(mtl).append(startTimes[0]);
            setEndDate(sb ,end ,start,enableEnd,mtl);
            strJobKey=sb.toString();
            jobDetail = JobBuilder.newJob(TaskJob.class).withIdentity(strJobKey,jobId).storeDurably().usingJobData(jobDataMap).build();
            triggerBuilder= TriggerBuilder.newTrigger().withIdentity(strJobKey,jobId);
            try {
                if(!scheduler.checkExists(jobDetail.getKey())){
                    calendar.setTime(start);
                    if(currentWhatDay<=(whatDay+1)) { 
                        calendar.add(Calendar.DATE, whatDay+1-currentWhatDay);
                    }else { 
                        calendar.add(Calendar.DATE, whatDay+1-currentWhatDay+7);
                    }
                    triggerBuilder.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(interval).withMisfireHandlingInstructionDoNothing());
                    triggerBuilder.startAt(startDate(calendar.getTime(), startTimes[0], jobDataMap.getString("timeZone")));
                    setTriggerBuilderForEndDate(end ,start,enableEnd,triggerBuilder);
                    trigger=triggerBuilder.build();
                    Date date = scheduler.scheduleJob(jobDetail, trigger);
                    date.getTime();
                }
            } catch (SchedulerException e) {
                logger.error("Failed to schedule jobid: " + jobId + ", jobKey: " + strJobKey, e);
            }
        }
    }

    private void byMonth(JobDataMap jobDataMap,LocalDate startL,List<Integer> increasedMonthList) {
        boolean enableEnd=jobDataMap.getBoolean("enableEnd");
        Date end=(Date)jobDataMap.get("end");
        if(enableEnd && end.before(new Date())) {
            return; 
        }
        int year;
        int month;
        String yearMonthDayhms;
        StringBuilder sb = new StringBuilder();
        Integer day=jobDataMap.getInt("day");
        Integer [] months=(Integer[])jobDataMap.get("month");
        String strJobKey=null;
        Trigger trigger=null;
        JobDetail jobDetail=null;
        Integer[] startTimes=(Integer[])jobDataMap.get("startTime");
        String jobId=jobDataMap.getString("jobId");
        String frequency=jobDataMap.getString("frequency");
        TriggerBuilder<Trigger> triggerBuilder=null;
        Date start=(Date)jobDataMap.get("start");
        String mtl="-";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        int currentMonth= calendar.get(Calendar.MONTH);
        if(!ObjectUtils.isEmpty(increasedMonthList)) {
            months = new Integer[] {};
            months=increasedMonthList.toArray(months);
        }
        for(int whatMonth:months){
            sb.setLength(0);
            sb.append(jobId).append(mtl).append(frequency).append(mtl).append(day).append(mtl).append(startL.toString()).append(mtl).append(whatMonth).append(mtl).append(startTimes[0]);
            setEndDate(sb ,end ,start,enableEnd,mtl);
            strJobKey=sb.toString();
            jobDetail = JobBuilder.newJob(TaskJob.class).withIdentity(strJobKey,jobId).storeDurably().usingJobData(jobDataMap).build();
            triggerBuilder= TriggerBuilder.newTrigger().withIdentity(strJobKey,jobId);
            try {
                if(!scheduler.checkExists(jobDetail.getKey())){
                    calendar.setTime(start);
                    if(currentMonth+1<=whatMonth) { 
                        calendar.add(Calendar.MONTH, whatMonth-(currentMonth+1));
                    }else { 
                        calendar.add(Calendar.MONTH, 12-(currentMonth+1)+whatMonth);
                    }
                    year=calendar.get(Calendar.YEAR);
                    month=calendar.get(Calendar.MONTH)+1;
                    yearMonthDayhms=String.valueOf(year)+"-"+String.valueOf(month)+"-"+String.valueOf(day) +" 0:0:0";
                    triggerBuilder.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInYears(1).withMisfireHandlingInstructionDoNothing());
                    triggerBuilder.startAt(startDate(transferDate(yearMonthDayhms, jobDataMap.getString("timeZone")) ,startTimes[0], jobDataMap.getString("timeZone")));
                    setTriggerBuilderForEndDate(end ,start,enableEnd,triggerBuilder);
                    trigger=triggerBuilder.build();
                    Date date = scheduler.scheduleJob(jobDetail, trigger);
                    date.getTime();
                }
            } catch (SchedulerException e) {
                logger.error("Failed to schedule jobid: " + jobId + ", jobKey: " + strJobKey, e);
            }
        }
    }

    private void setTriggerBuilderForEndDate(Date end ,Date start,boolean enableEnd,TriggerBuilder<Trigger> triggerBuilder) {
        if(enableEnd) {
            if((end).after((start))) {
                triggerBuilder.endAt(end);
            }
        }
    }

    private String setEndDate(StringBuilder sb ,Date end ,Date start,boolean enableEnd,String mtl) {
        if(enableEnd) {
            if((end).after((start))) {
                sb.append(mtl).append(end.getTime());
            }
        }
        return sb.toString();
    }

    private static int tran(String str) {
        LocalTime localTime=LocalTime.parse(str);
        int s= localTime.getHour()*60*60+localTime.getMinute()*60+localTime.getSecond();
        return s;
    }

    private boolean deleteJobKeys(List<JobKey> listJobKey) {
        StringBuilder sb = new StringBuilder();
        boolean result = false;
        try {
            if(!ObjectUtils.isEmpty(listJobKey)) {
                Iterator<JobKey> it = listJobKey.iterator();  
                for (JobKey jobKey : listJobKey) {
                    String jobId=jobKey.getGroup();
                    if(jobKey.getName().contains("nodelete")) {
                        listJobKey.remove(jobKey);
                    }else {
                        sb.append("delete jobKeys by jobId: ").append(jobKey.getName()).append(",");
                    }
                }
                result = scheduler.deleteJobs(listJobKey);
                sb.append("delete result:").append(result);
                logger.debug(sb.toString());
                sb.setLength(0);
                return result;
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Remove quartz jobs when JobDef entries are deleted from database
     * @param thisJobIds
     */
    @Override
    public void cleanJobs(List<String> thisJobIds){
        if(this.preJobIds.size()==0 && thisJobIds.size()!=0) {
            preJobIds=thisJobIds;
        }else if(this.preJobIds.size()!=0 && thisJobIds.size()!=0) {
            String[] arrayThisJobId = new String[] {};
            String[] arrayPreJobId = new String[] {};
            arrayThisJobId = thisJobIds.toArray(arrayThisJobId);
            arrayPreJobId = preJobIds.toArray(arrayPreJobId);
            for(String groupName :compare(arrayThisJobId,arrayPreJobId)) {
                Set<JobKey> set;
                try {
                    set = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                    deleteJobKeys(new CopyOnWriteArrayList<JobKey>(set));
                } catch (SchedulerException e) {
                    logger.error("Failed to delete job by groupName " + groupName, e);
                }
            }
            preJobIds=thisJobIds; 
        }
        loopScheduler(logger);
    }

    @Override
    public boolean runNow(String jobId) {
        Map<String,JobDataMap> map = new ConcurrentHashMap<String,JobDataMap>();
        try {
        FindIterable<Document> iteratble=mongoDatabase.getCollection(COLLECTION_NAME).find(eq("job.jobId", jobId)).batchSize(1);
        setJobDataMap(map,iteratble);
        }catch (Exception e) {
            logger.error("Failed to runNow by jobId: "+jobId, e);
        }
        boolean bl=false;
        if(map.containsKey(jobId)) {
            JobDataMap jobDataMap =map.get(jobId);
            bl=runNow(jobDataMap);
        }
        return bl;
    }

    @Override
    public String getLastTimeNextTime() {
        Map<String, HashMap<String, Date>> map = new ConcurrentHashMap<String, HashMap<String, Date>>();
        HashMap<String, Date> mapDate = null;
        String gp = null, jobName = null;
        try {
            List<String> groupNames = scheduler.getJobGroupNames();
            for (String groupName : groupNames) {
                if (!groupName.equals("DEFAULT")) {
                    gp = groupName;
                    mapDate = new HashMap<String, Date>();
                    for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                        jobName = jobKey.getName();
                        List<Trigger> list = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                        Date previousFireTime = null;
                        Date nextFireTime = null;
                        if (!ObjectUtils.isEmpty(list)) {
                            for (int index = 0; index < list.size(); index++) {
                                Trigger trigger = list.get(index);
                                previousFireTime = trigger.getPreviousFireTime();
                                nextFireTime = trigger.getNextFireTime();
                                if (index == 0) {
                                    mapDate.put("lastRunTime", previousFireTime);
                                    mapDate.put("nextRunTime", nextFireTime);
                                } else {
                                    if (mapDate.get("nextRunTime").after(nextFireTime)) {
                                        mapDate.put("nextRunTime", nextFireTime);
                                        mapDate.put("lastRunTime", previousFireTime);
                                    }
                                }
                            }
                        }
                    }
                    map.put(groupName, mapDate);
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            String json = mapper.writeValueAsString(map);
            logger.debug("json {}", json);
            return json;
        } catch (SchedulerException | JsonProcessingException e) {
            logger.error("Failed to getLastTimeNextTime for group " + gp + ", jobKey: " + jobName, e);
            return null;
        }
    }

    public void scheduleAllJobs() {
        lock.lock();
        try {
            List<String> jobIds = new ArrayList<String>();
            Map<String, JobDataMap> map = getJobDataMap();
            for (Map.Entry<String, JobDataMap> entry : map.entrySet()) {
                JobDataMap jobDataMap = entry.getValue();
                this.makeScheduler(jobDataMap);
                jobIds.add(entry.getKey());
            }
            cleanJobs(jobIds);
        } finally {
            lock.unlock();
        }
    }

    public void addScheduledJob(String jobId) {
        lock.lock();
        try {
            Map<String, JobDataMap> map = getJobDataMapByJobId(jobId);
            for (Map.Entry<String, JobDataMap> entry : map.entrySet()) {
                JobDataMap jobDataMap = entry.getValue();
                this.makeScheduler(jobDataMap);
            }
        } finally {
            lock.unlock();
        }
    }
    public boolean sameDate(Date d1, Date d2) {
        LocalDate localDate1 = ZonedDateTime.ofInstant(d1.toInstant(), ZoneId.systemDefault()).toLocalDate();
        LocalDate localDate2 = ZonedDateTime.ofInstant(d2.toInstant(), ZoneId.systemDefault()).toLocalDate();
        return localDate1.isEqual(localDate2);
    }
    public Date getPreviousFireTime(JobDetail jobDetail) throws SchedulerException {
        JobKey jobKey=jobDetail.getKey();
        List<Trigger> list =(List<Trigger>) scheduler.getTriggersOfJob(jobKey);
        Date previousFireTime = null;
        if(!ObjectUtils.isEmpty(list)) {
            for(Trigger trigger1:list) {
                previousFireTime =trigger1.getPreviousFireTime();
            }
        }
        return previousFireTime;
    }

    public boolean isJobTriggeredFinished(JobDetail jobDetail) throws SchedulerException {
        JobKey jobKey = jobDetail.getKey();
        boolean bExist = scheduler.checkExists(jobKey);
        List<Trigger> list = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
        return bExist && (ObjectUtils.isEmpty(list) || list.size() == 0);
    }
}
