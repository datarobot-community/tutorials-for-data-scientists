package com.datarobot.examples.scoring.prime;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.commons.collections.IteratorUtils;

import java.util.*;

public class PrimeBatchLambdaExample implements RequestHandler<ArrayList<LinkedHashMap<String, String>>, Map<String,List>> {

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
        public Map<String,List> handleRequest(ArrayList<LinkedHashMap<String, String>> event, Context context) {


            List<Map> recordsList = new ArrayList<>();

            //OPTIONAL: You can add row attributes to result row:
            recordsList.addAll(event);

            Map<String, List> results = new LinkedHashMap<>();
            //results.putAll(event);

            try {
                //Call Prime model for multi-record prediction
                List predictions = IteratorUtils.toList(primePrediction.score(recordsList.iterator()));

                results.put("predictions", predictions);
                return results;

            } catch (Exception e) {
                //TODO: handle exception here
                e.printStackTrace();
                //results.put("error", e.getMessage());
                return results;
            }
        }
}
