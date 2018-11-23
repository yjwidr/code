package com.netbrain.kc.framework.token;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * JwtToken
 */
public class JwtToken {
    private static Logger logger = LogManager.getLogger(JwtToken.class.getName());
    private static final String SECRET = "netbrain-knowledge-cloud"; //Do not modify this value; it is the key
    private static ObjectMapper mapper = new ObjectMapper();

    private static Map<String, Object> createHead() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("typ", "JWT");
        map.put("alg", "HS256");
        return map;
    }

    public static <T> String sign(T obj, long maxAge) throws UnsupportedEncodingException, JsonProcessingException {
        JWTCreator.Builder builder = JWT.create();

        builder.withHeader(createHead())// header
               .withSubject(mapper.writeValueAsString(obj)); // payload

        if (maxAge >= 0) {
            long expMillis = System.currentTimeMillis() + maxAge;
            Date exp = new Date(expMillis);
            builder.withExpiresAt(exp);
        }

        return builder.sign(Algorithm.HMAC256(SECRET));
    }

    public static <T> T unsign(String token, Class<T> classT) throws IOException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
        DecodedJWT jwt = verifier.verify(token);
        
        Date exp = jwt.getExpiresAt();
        if (exp != null && exp.after(new Date())) {
            String subject = jwt.getSubject();
            return mapper.readValue(subject, classT);
        }
        
        return null;
    }
    
    public static String getPayload(String token) throws IOException{
    	DecodedJWT jwt = JWT.decode(token);
		return jwt.getSubject();
    }

}
