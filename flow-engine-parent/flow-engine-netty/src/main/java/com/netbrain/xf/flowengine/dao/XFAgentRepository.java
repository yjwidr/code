package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFAgent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface XFAgentRepository extends MongoRepository<XFAgent, String>
{

}
