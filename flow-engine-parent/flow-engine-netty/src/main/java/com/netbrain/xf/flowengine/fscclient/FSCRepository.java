package com.netbrain.xf.flowengine.fscclient;

import com.mongodb.client.MongoDatabase;
import com.netbrain.ngsystem.model.FrontServerController;
import com.netbrain.xf.flowengine.scheduler.services.SchedulerServicesImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.swing.text.Document;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class FSCRepository {
    private static Logger logger = LogManager.getLogger(FSCRepository.class.getSimpleName());

    @Resource(name="ngsystem")
    private MongoTemplate ngsystemTemplate;

    /**
     * Find FrontServerController by a given tenantId
     * @param tenantId
     * @return null if not found, or the first matching FSC if there are multiple matching FSCs.
     */
    public FrontServerController findFSCByTenantId (String tenantId) {
        // The $all operator selects the documents where the value of a field is an array that contains all the specified elements.
        // here it returns a FrontServerController Document whose tenants field is an array and contains tenantId
        return ngsystemTemplate.findOne(Query.query(where("tenants").elemMatch(new Criteria("_id").is(tenantId))), FrontServerController.class);
    }

    public List<FrontServerController> findAll(int limit) {
        return ngsystemTemplate.find(Query.query(new Criteria()).limit(limit), FrontServerController.class);
    }

    /**
     * This is currently only used by unit tests to create testing data.
     * Please refactor this method if it is used in production
     * @param fsc
     */
    protected void addFSC(FrontServerController fsc) {
        ngsystemTemplate.insert(fsc);
    }

    /**
     * This is currently only used by unit tests to create testing data.
     * Please refactor this method if it is used in production
     * @param fsc
     */
    protected void removeFSC(FrontServerController fsc) {
        ngsystemTemplate.remove(fsc);
    }
}
