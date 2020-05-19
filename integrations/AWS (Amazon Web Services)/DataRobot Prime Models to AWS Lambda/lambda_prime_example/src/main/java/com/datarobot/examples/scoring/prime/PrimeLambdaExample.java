package com.datarobot.examples.scoring.prime;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.*;

public class PrimeLambdaExample implements RequestHandler<LinkedHashMap<String,String>, Map<String,String>> {

    public static Prediction primePrediction;

        static {
            try {
                //Do some init here
                primePrediction = new Prediction();
            } catch (Exception e) {
                //TODO: handle exception here
                e.printStackTrace();
            }
        }

        @Override
        public Map<String,String> handleRequest(LinkedHashMap<String,String> event, Context context) {

            List<Map> recordsList = new ArrayList<>();
            recordsList.add(event);

            Map<String, String> results = new LinkedHashMap<>();
            //OPTIONAL: You can add row attributes to result row:
            //results.putAll(event);

            try {
                //Call Prime model for one-record prediction
                results.put("prediction", String.valueOf(primePrediction.score(recordsList.iterator()).next()));
                return results;

            } catch (Exception e) {
                //TODO: handle exception here
                e.printStackTrace();
                results.put("error", e.getMessage());
                return results;
            }
        }
}
