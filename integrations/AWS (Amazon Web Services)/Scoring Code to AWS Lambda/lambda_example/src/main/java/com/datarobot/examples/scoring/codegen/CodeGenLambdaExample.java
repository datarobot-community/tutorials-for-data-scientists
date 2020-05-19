package com.datarobot.examples.scoring.codegen;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.datarobot.prediction.IClassificationPredictor;
import com.datarobot.prediction.IRegressionPredictor;
import com.datarobot.prediction.Predictors;

import java.util.LinkedHashMap;
import java.util.Map;

public class CodeGenLambdaExample implements RequestHandler<LinkedHashMap<String,String>, Map<String,Object>> {


    public static String modelId = "<Put exported CodeGen model_id here>";
    public static IClassificationPredictor model;

    static {
            try {
                //Do some init here
                model = Predictors.getPredictor(modelId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Map<String,Object> handleRequest(LinkedHashMap<String,String> event, Context context) {

            Map<String, Object> results = new LinkedHashMap<>();

            try {
                //Call GodeGen model for predictions
                Map<String, ?> prediction = model.score(event);

                results.putAll(event);
                results.putAll(prediction);

                return results;

            } catch (Exception e) {

                //TODO: handle exception here
                e.printStackTrace();
                results.put("error", e.getMessage());

                return results;
            }
        }
}
