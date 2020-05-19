package com.datarobot.examples.scoring.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CodeGenLambdaTesting {

    public static void main(String[] args) {

        String testJson = "{" +
                "    \"race\": \"Caucasian\",\n" +
                "    \"gender\": \"Female\",\n" +
                "    \"age\": \"[50-60)\",\n" +
                "    \"weight\": \"?\",\n" +
                "    \"admission_type_id\": \"Elective\",\n" +
                "    \"discharge_disposition_id\": \"Discharged to home\",\n" +
                "    \"admission_source_id\": \"Physician Referral\",\n" +
                "    \"time_in_hospital\": 1,\n" +
                "    \"payer_code\": \"CP\",\n" +
                "    \"medical_specialty\": \"Surgery-Neuro\",\n" +
                "    \"num_lab_procedures\": 35,\n" +
                "    \"num_procedures\": 4,\n" +
                "    \"num_medications\": 21,\n" +
                "    \"number_outpatient\": 0,\n" +
                "    \"number_emergency\": 0,\n" +
                "    \"number_inpatient\": 0,\n" +
                "    \"diag_1\": 723,\n" +
                "    \"diag_2\": 723,\n" +
                "    \"diag_3\": 719,\n" +
                "    \"number_diagnoses\": 9,\n" +
                "    \"max_glu_serum\": \"None\",\n" +
                "    \"A1Cresult\": \"None\",\n" +
                "    \"metformin\": \"No\",\n" +
                "    \"repaglinide\": \"No\",\n" +
                "    \"nateglinide\": \"No\",\n" +
                "    \"chlorpropamide\": \"No\",\n" +
                "    \"glimepiride\": \"No\",\n" +
                "    \"acetohexamide\": \"No\",\n" +
                "    \"glipizide\": \"No\",\n" +
                "    \"glyburide\": \"No\",\n" +
                "    \"tolbutamide\": \"No\",\n" +
                "    \"pioglitazone\": \"No\",\n" +
                "    \"rosiglitazone\": \"No\",\n" +
                "    \"acarbose\": \"No\",\n" +
                "    \"miglitol\": \"No\",\n" +
                "    \"troglitazone\": \"No\",\n" +
                "    \"tolazamide\": \"No\",\n" +
                "    \"examide\": \"No\",\n" +
                "    \"citoglipton\": \"No\",\n" +
                "    \"insulin\": \"No\",\n" +
                "    \"glyburide.metformin\": \"No\",\n" +
                "    \"glipizide.metformin\": \"No\",\n" +
                "    \"glimepiride.pioglitazone\": \"No\",\n" +
                "    \"metformin.rosiglitazone\": \"No\",\n" +
                "    \"metformin.pioglitazone\": \"No\",\n" +
                "    \"change\": \"No\",\n" +
                "    \"diabetesMed\": \"No\",\n" +
                "    \"readmitted\": \"FALSE\",\n" +
                "    \"diag_1_desc\": \"Spinal stenosis in cervical region\",\n" +
                "    \"diag_2_desc\": \"Spinal stenosis in cervical region\",\n" +
                "    \"diag_3_desc\": \"Effusion of joint, site unspecified\"\n" +
                "  }";

        ObjectMapper objectMapper = new ObjectMapper();


        CodeGenLambdaExample test = new CodeGenLambdaExample();
        try {
            LinkedHashMap<String, String> jsonMap = objectMapper.readValue(testJson, new TypeReference<LinkedHashMap<String,String>>(){});
            test.handleRequest(jsonMap, null);
            Map<String,Object> result = test.handleRequest(jsonMap, null);

            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
