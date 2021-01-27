#!/usr/bin/env python
# coding: utf-8

'''


'''
import time
import pandas as pd
import datarobot as dr
from datarobot.models.modeljob import wait_for_async_model_creation
import numpy as np
import re
import os
from datarobot.errors import JobAlreadyRequested

token_id = ""
ts_setting = {"project_name":"fake_job_posting_210123","filename":"../Data/fake_job_postings.csv",     \
              "project_id": "60089b3d23aace3eea1810d0","model_id":"",     \
              "feature_list": "Informative Features","features":[],"set":"validation" , \
              "AUC":"Weighted AUC", "LogLoss":"Weighted LogLoss",                                    \
              "downsampling": 36,"holdout_pct": 20,"validation_pct":16,"target":"fraudulent" }

parameter_name = ['stop_words','stemmer','num_ngram',"use_idf","pos_tagging"]
value = [1,"porter",[1,2,3,4],1,1]
param_df = pd.DataFrame(list(zip(parameter_name, value)),
               columns =['parameter_name', 'value'])

dr.Client(token=token_id, endpoint='https://app.datarobot.com/api/v2')



def check_if_number(st):
    tp = re.search("\d+",st)
    if tp:
        return int(tp.group())
    else:
        return np.nan

def get_min_max_salary (text):
    '''
    Get the min and max from the salary_range
    :param text: string
    :return: the min and max of a salary_range
    '''
    if type(text) == str:
        if re.search("\-",text):
            tp = text.split("-")
            min_salary = check_if_number(tp[0].strip())
            max_salary = check_if_number(tp[1].strip())
            return min_salary,max_salary
        else:
            return np.nan,np.nan
    else:
        return np.nan, np.nan


def cleaned_location(text):
    '''
    Extract country, and country_and state from location
    :param text: string with country, state, city
    :return:
    '''
    country_state = ""
    st = str(text)
    if type(st) is str:
        tp = re.search("[a-zA-Z]{2,}\s?\,(\s*[a-zA-Z0-9]+|\s)",st)
        if tp:
            country_state = tp.group().strip()
            country = st.strip()[0:2]
        else:
            return "",""
        return country,country_state
    else:
        return "",""
def create_binary_cat_for_education(text):
    if pd.isnull(text) or pd.isna(text):
        return "no"
    elif text == "unspecified":
        return "no"
    else:
        return "yes"
def PrepareDataSet():
    '''
    Prepare the dataset for fake_job_postings by adding new features.
    :return: enriched original dataset with new features
    '''
    fake_jobs_df = pd.read_csv(ts_setting["filename"])
    fake_jobs_df.min_salary = np.nan
    fake_jobs_df.max_salary = np.nan
    fake_jobs_df.salary_diff = np.nan
    fake_jobs_df["min_salary"],fake_jobs_df["max_salary"] = zip(*fake_jobs_df["salary_range"].apply(get_min_max_salary))
    fake_jobs_df["min_salary"] = pd.to_numeric(fake_jobs_df["min_salary"])
    fake_jobs_df["max_salary"] = pd.to_numeric(fake_jobs_df["max_salary"])
    fake_jobs_df["education_flag"] = [create_binary_cat_for_education(x) for x in fake_jobs_df["required_education"]]
    fake_jobs_df["salary_range"] = fake_jobs_df.max_salary - fake_jobs_df.min_salary
    fake_jobs_df["salary_diff"] = fake_jobs_df["salary_range"]/fake_jobs_df["min_salary"]
    return fake_jobs_df


def start_project_with_settings(fake_jobs_df):
    '''
    Run a project for fake_jobs_df
    :param fake_jobs_df: already enriched dataset
    :return: project
    '''
    global ts_setting
    advanced_options = dr.AdvancedOptions(
        response_cap=0.7,
        blueprint_threshold=2,
        smart_downsampled=True, majority_downsampling_rate=ts_setting["downsampling"])
    partition = dr.StratifiedTVH(ts_setting["holdout_pct"],ts_setting["validation_pct"], seed=0)
    pandas_dataset = dr.Dataset.create_from_in_memory_data(data_frame=fake_jobs_df.drop(columns = ["job_id"]))
    project = pandas_dataset.create_project(project_name = ts_setting["project_name"])
    project.set_target(target= ts_setting["target"],mode = dr.enums.AUTOPILOT_MODE.QUICK, 
                       partitioning_method=partition, 
                       advanced_options = advanced_options,
                       worker_count = -1)
    project.unlock_holdout()
    project.wait_for_autopilot(verbosity=dr.VERBOSITY_LEVEL.SILENT)
    return project
'''
From the project, find features, DataRobot set as text features
'''

def get_text_features(project):
    '''
    get text features
    :param project: DataRobot Project
    :return: list of features of type text
    '''
    raw = [feat_list for feat_list in project.get_featurelists()\
           if feat_list.name == ts_setting["feature_list"]][0]
    text_features = [
        feat
        for feat in raw.features if dr.Feature.get(project.id, feat).feature_type == "Text"
    ]
    return text_features

#Get all the models for a given text field
def get_1_model_performance(model_p,text_feature,num_modified):
    '''
    Extract a model metrics
    :param model_p: model of interest
    :param text_feature: list of features of type text
    :param num_modified: number of parameters modified
    :return: performance of type dict
    '''
    global ts_setting
    performance = {}
    try:
        roc = model_p.get_roc_curve(ts_setting["set"])
        threshold = roc.get_best_f1_threshold()
        metrics = roc.estimate_threshold(threshold)
        performance = {"model_id":model_p.id,"text_feature":text_feature,"AUC":model_p.metrics[ts_setting["AUC"]][ts_setting["set"]],   \
                           "sample_pct":model_p.sample_pct,
                           "LogLoss":model_p.metrics[ts_setting["LogLoss"]][ts_setting["set"]],
                           'f1_score':metrics['f1_score'],"sample_pct":model_p.sample_pct,\
                           'true_negative_rate': metrics['true_negative_rate'],
                           'false_positive_rate':metrics['false_positive_rate'],
                           'true_positive_rate':metrics['true_positive_rate'],\
                           'positive_predictive_value':metrics['positive_predictive_value'],\
                           'negative_predictive_value':metrics['negative_predictive_value'],\
                           'threshold':metrics['threshold'],'parameters_modified': num_modified}
        return performance
    except:
        performance = {"model_id": model_p.id, "text_feature": text_feature,
                       "AUC": 0, \
                       "sample_pct": model_p.sample_pct,
                       "LogLoss": 1,
                       'f1_score': 0, "sample_pct": model_p.sample_pct, \
                       'true_negative_rate': 0,
                       'false_positive_rate': 0,
                       'true_positive_rate': 0, \
                       'positive_predictive_value': 0, \
                       'negative_predictive_value': 0, \
                       'threshold': 0, 'parameters_modified': num_modified}
        return performance
#Get all the models for a given text field
#This function will have 2 uses:  First, it will be used to find the best AutoTuned model for the 
#text features, and then it will be used to compare the best model before the pre-processing and 
#after the pre-processing.  Keep only models that used less than 100 of dataset 
def models_performance_for_text(text_feature,project):
    '''
    extract all models built only for text features
    :param text_feature: list of features of type text
    :param project: DataRobot project
    :return: all models trained on less than 100% and trained on only the text features (Auto-Tuned Word N-gram )
    '''
    models_desc =project.get_models(
    search_params={
        'name': text_feature
    })
    df= pd.DataFrame()
    for model_p in models_desc:
        tmp_df = get_1_model_performance(model_p,text_feature,0)
        if tmp_df:
            if tmp_df["sample_pct"] < 100.00:
                df = df.append(tmp_df, ignore_index=True)
            
    return df

def get_best_models_before_text(project):
    '''
    get the best models for each text features.  This function calls get_text_features, and models_performance_for_text
    :param project: DataRobot project
    :return: best models id and logloss metric
    '''
    text_features= get_text_features(project)
    models_df = pd.DataFrame()
    performance_df = pd.DataFrame()
    for text in text_features:
        print(text)
        performance_df = models_performance_for_text(text, project)
        index_max = min(range(len(performance_df.LogLoss)), key=performance_df.LogLoss.__getitem__)
        best_model_before = performance_df.iloc[index_max, :]
        models_df = models_df.append({"text_feature": text, "model_id": best_model_before.model_id,\
                                        "sample_pct":best_model_before.sample_pct,
                                        "LogLoss": best_model_before.LogLoss,"parameters_modified":0}, ignore_index=True)
    return models_df

'''
Set a list of models for each text word
Get the models to modify and start tunning
'''
def prepare_model_for_tuning(model_id,project):
    '''
    set a model for parameter tuning
    :param model_id: DataRobot model id
            project: DataRobot project
    :return: tune
    '''
    model = dr.Model.get(project=project.id,
                         model_id=model_id)
    tune = model.start_advanced_tuning_session()
    return model,tune

# Get available task names,
# and available parameter names for a task name that exists on this model
def set_parameters(param_2_set,tune):
    '''
    set an already trained model parameters
    :param param_2_set: number of parameters to set from list of potential parameters in
    param_df
    :param tune: an object with a model tunable parameters
    :return: the tuned model parameters
    '''
    global param_df
    task_name = tune.get_task_names()[0]
    #tune.get_parameter_names()
    for index, row in param_df[0:param_2_set].iterrows():
        tune.set_parameter(
            task_name=task_name,
            parameter_name= row["parameter_name"] ,
            value= row["value"])
    return tune



def run_auto_tuned_with_different_settings(models_df,project):
    '''
    Run models with new set parameters
    :param models_df: list of models to re-run with modified parameters
    :return: a pandas dataframe with updated models_df with the new models appended to it
    '''
    tmp_df = pd.DataFrame()
    for index, model_p in models_df.iterrows():
        print("text feature",model_p.text_feature)
        text_feature = model_p.text_feature
        model_id = model_p.model_id
        model_1,tune = prepare_model_for_tuning(model_id,project)
        try:
            tune2 = set_parameters(3, tune)
            print("tune ", index)
            job = tune2.run()
            tmp_df= tmp_df.append({"model_id":job.model_id,'text_feature':text_feature,'parameters_modified':3}, \
                                      ignore_index= True)
            job.wait_for_completion()
        except JobAlreadyRequested:
            print("duplicate Job")
        model_1,tune = prepare_model_for_tuning(model_id,project)
        try:
            tune2 = set_parameters(6, tune)
            job2 = tune2.run()
            '''while job2.status != "COMPLETED":
                job2.refresh()'''
            tmp_df= tmp_df.append({"model_id":job2.model_id,'text_feature':text_feature,'parameters_modified':5},\
                                  ignore_index = True)
            job.wait_for_completion()
        except JobAlreadyRequested:
            print("duplicate Job")
    tmp_df.to_csv("../Data/ModelsBuiltForFakeJobs.csv")
    model_df = models_df.append(tmp_df, ignore_index=True, sort=True)
    model_df.to_csv("../Data/AllTextModels.csv")

    return model_df


def get_performance_of_tuned_models(models_tuned,project):
    '''
    get the metrics for all the text models tuned and their respective models before tuning
    :param models_tuned: pandas dataframe with model ids
    :param project: project
    :return: pandas dataframe with metrics
    '''
    result_df = pd.DataFrame()
    for index, row in models_tuned.iterrows():
        try:
            model1 = dr.Model.get(project=project.id, model_id=row.model_id)
        except:
            print("Error")
        model_tmp_df = get_1_model_performance(model1, row.text_feature, row.parameters_modified)
        result_df = result_df.append(model_tmp_df, ignore_index=True)
    return result_df


def create_new_featurelist(project):
    '''
    create a feature list from all non text features
    :param project: DataRobot project
    :return: a new DataRobot feature list
    '''
    #check if feature test3 is featurelist
    featurelist = [feat_list for feat_list in project.get_featurelists()\
           if feat_list.name == 'test3']
    if featurelist:
        featurelist = featurelist[0]
    else:
        raw = [feat_list for feat_list in project.get_featurelists()\
               if feat_list.name == 'Informative Features'][0]
        features_to_keep = [
            feat
            for feat in raw.features if dr.Feature.get(project.id, feat).feature_type != "Text"
        ]
        featurelist = project.create_featurelist('test3', features_to_keep)
    return featurelist

def run_best_model_with_new_featurelist(project,bestModels,sample_size_train,featurelist):
    '''
    1.  run the best recommended model with new feature list, and with sample size equal to sample_size_train
    2.  build a blender with the new trained model, and the best text features models
    :param bestModels: a list of models id for text features models
    :param sample_size_train: the highest sample size, but less than 100%, in all models
    :param featurelist: a new created feature list
    :return: blended model
    '''
    recommendation = dr.ModelRecommendation.get(project.id)
    recommended_model = recommendation.get_model()
    model_job_id =recommended_model.train(sample_pct = sample_size_train, featurelist_id =featurelist.id )
    new_model = wait_for_async_model_creation(
        project_id=project.id,
        model_job_id=model_job_id,
        max_wait = 1200000
    )
    bestModels.append(new_model.id)
    model_Blender = project.blend(bestModels, dr.enums.BLENDER_METHOD.GLM)
    blended_model = model_Blender.get_result_when_complete()
    return blended_model


def get_All_model_performance(model_p):
    '''
    given a model_p get its performance metrics
    :param model_p: a model
    :return: a dictionary with performance
    '''
    roc = model_p.get_roc_curve(ts_setting["set"])
    threshold = roc.get_best_f1_threshold()
    metrics = roc.estimate_threshold(threshold)
    performance = {"model_id": model_p.id, "model_type": model_p.model_type,
                   "AUC": model_p.metrics[ts_setting["AUC"]][ts_setting["set"]], "sample_pct": model_p.sample_pct,
                   "LogLoss": model_p.metrics[ts_setting["LogLoss"]][ts_setting["set"]],
                   'f1_score': metrics['f1_score'], \
                   'true_negative_rate': metrics['true_negative_rate'],
                   'false_positive_rate': metrics['false_positive_rate'],
                   'true_positive_rate': metrics['true_positive_rate'], \
                   'positive_predictive_value': metrics['positive_predictive_value'], \
                   'negative_predictive_value': metrics['negative_predictive_value'],\
                   'threshold':threshold}
    return performance



def models_performance_for_All(project):
    '''
    get the performance of all the models
    :param project: DataRobot project
    :return: a dataframe with all models perfomances
    '''
    models_desc = project.get_models()
    df = pd.DataFrame()
    for model_p in models_desc:
        tmp_df = get_All_model_performance(model_p)
        df = df.append(tmp_df, ignore_index=True)

    return df
def extract_best_models(result_df):
    '''
    For each text features, extract the best Auto-Tune Word N-gram model
    :param result_df: all the text auto-tuned models
    :return: a dataframe with the best model or each text, and the highest sample size in
    result_df
    '''
    sample_size_train = result_df.sample_pct.max()
    # Get best models for each text: They will be used in blender
    groups = result_df.groupby(by=['text_feature'])
    bestModels = groups.apply(lambda g: g[g['LogLoss'] == g['LogLoss'].min()]).model_id
    bestModels = list(bestModels)
    return bestModels,sample_size_train

def rerun_text_models_with_new_param(project):
    '''
    rerun text models with different settings, and get their performance
    :param project: the DataRobot project
    :return: a dataframe with the tuned models performance
    '''
    # Tune the best models with different text preprocessing approaches
    models_df = get_best_models_before_text(project)
    models_df.to_csv("../Data/bestModels.csv")
    model_tuned_df = run_auto_tuned_with_different_settings(models_df, project)
    while project.get_all_jobs():
        time.sleep(5)
    result_df = get_performance_of_tuned_models(model_tuned_df, project)
    result_df.to_csv("../Data/AllResultsWithMetrics.csv")
    return result_df

def create_df_with_performance_of_all_models(project,result_df,bestModels):
    '''
    get the performance of all the models, and add the number of tuning each text model went through.
    If you don't care knowing which best model for each text feature was chosen, you just need to run
     all_performance = models_performance_for_All(project)
     all_performance = all_performance.sort_values('LogLoss')
     all_performance["project_id"]= project.id
    :param project: DataRobot project
    :param result_df: all text models performance
    :param bestModels: the best models for each text feature
    :return: a dataframe with all the metrics and a column with the number of parameters tuned
    '''
    all_performance = models_performance_for_All(project)
    all_performance = all_performance.sort_values('LogLoss')
    model_modified_param = result_df[result_df.model_id.isin(bestModels)][["model_id", "parameters_modified"]]
    all_performance = all_performance.merge(model_modified_param, on='model_id', how='left')
    all_performance["project_id"]= project.id
    return all_performance


'''
START Run

'''
if __name__ == "__main__":

    fake_jobs_df = PrepareDataSet()
    fake_jobs_df.to_csv("../Data/FakeDataCleaned.csv")
    # run project using the cleaned dataset
    project = start_project_with_settings(fake_jobs_df)
    ts_setting["project_id"] = project.id
    # rerun text models with parameters modified
    #project = dr.Project("60089b3d23aace3eea1810d0")
    result_df = rerun_text_models_with_new_param(project)
    # Extract best models for each text models
    bestModels, sample_size_train = extract_best_models(result_df)
    # Create a new feature list with out text features
    featureList = create_new_featurelist(project)
    # train the best model in project with non-text features, and blend this model with the
    # models trained with modified parameters
    model_blender = run_best_model_with_new_featurelist(project, bestModels, sample_size_train, featureList)
    # put all the results in a dataframe and save the result in /Data as a csv file
    all_performance_df = create_df_with_performance_of_all_models(project, result_df, bestModels)
    all_performance_df = all_performance_df.to_csv("../Data/All_Models_Complete.csv")

