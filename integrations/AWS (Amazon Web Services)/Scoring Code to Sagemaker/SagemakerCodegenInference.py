import time
import random
import pandas as pd
import json
import boto3
from botocore.client import Config
import csv
import itertools
from datarobot.mlops.mlops import MLOps
import os
from io import StringIO

"""
This is sample code on how to make predictions against a Sagemaker deployed model.
"""

runtime_client = boto3.client('runtime.sagemaker')
endpoint_name = 'YOUR_ENDPOINT_NAME'
cur_dir = os.path.dirname(os.path.abspath(__file__))
dataset_filename = os.path.join(cur_dir, "/path/YOUR_FILE.csv")


def _feature_df(num_samples):
    df = pd.read_csv(dataset_filename)

    return pd.DataFrame.from_dict(df)


def _predictions_list(num_samples):
    with open(dataset_filename, 'rb') as f:
        payload = f.read()

    result = runtime_client.invoke_endpoint(
        EndpointName=endpoint_name,
        Body=payload,
        ContentType='text/csv',
        Accept='Accept'
    )

    str_predictions = result['Body'].read().decode()
    df_predictions = pd.read_csv(StringIO(str_predictions))
    list_predictions = df_predictions.values.tolist()
    print("number of predictions made are : ",len(list_predictions))
    return list_predictions

def main():

    start_time = time.time()
    predictions_array = _predictions_list(num_samples)
    print(len(predictions_array))
    end_time = time.time()


if __name__ == "__main__":
    main()