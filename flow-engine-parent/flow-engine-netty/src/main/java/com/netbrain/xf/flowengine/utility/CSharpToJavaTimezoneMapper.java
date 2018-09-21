package com.netbrain.xf.flowengine.utility;

import com.netbrain.xf.flowengine.scheduler.services.SchedulerServicesImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Map a C# TimeZoneInfo.Id (From Microsoft Windows Time Zone Database) to
 * a Java ZoneId (defined by IANA/Olson, a.k.a. TZDB)
 * https://stackoverflow.com/questions/5996320/net-timezoneinfo-from-olson-time-zone
 */
@Component
public class CSharpToJavaTimezoneMapper {
    private Map<String, ZoneId> timezoneMap;

    private static Logger logger = LogManager.getLogger(SchedulerServicesImpl.class.getSimpleName());

    @PostConstruct
    public void loadTimeZoneMap() {
        Map<String, ZoneId> map = new HashMap<>();

        try {
            File file = new File("conf/MSZoneToTZDB.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] entry = line.split(",");
                if (entry.length == 2) {
                    try {
                        map.put(entry[1].trim(), ZoneId.of(entry[0].trim()));
                    } catch (DateTimeException dte) {
                        logger.warn("Failed to load timezone " + entry[1] + " from mapping file, plesae ignore");
                    }
                }
            }
            fileReader.close();
        } catch (Exception e) {
            logger.error("Failed to load timezone from mapping file", e);
        }

        timezoneMap = Collections.unmodifiableMap(map);
    }

    public ZoneId getZoneFromCSharpName(String csharpTimezoneName) {
        ZoneId zoneId = timezoneMap.get(csharpTimezoneName);
        if (zoneId != null) {
            return zoneId;
        } else {
            return ZoneId.systemDefault();
        }
    }
}
