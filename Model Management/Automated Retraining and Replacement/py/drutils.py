import datarobot as dr
import pandas as pd
import yaml
import json
import requests
import time
import sys


def load_config(filename):
    """Open and read a yaml config"""
    with open(filename, 'r') as of:
        config = yaml.load(of, Loader=yaml.BaseLoader)
    return config


def get_existing_project(proj_name):
    built_projects = dr.Project.list()
    built_names = [p.project_name for p in built_projects]
    # new project name
    if proj_name in built_names:
        return built_projects[built_names.index(proj_name)]
    else:
        return None


def get_parent_model(model):
    """Get parent model if one exists"""
    try:
        parent_id = dr.FrozenModel.get(model.project_id,
                                       model.id).parent_model_id
        parent = dr.Model.get(model.project_id, parent_id)
        return parent
    except:
        return model


def get_default_pred_server_info():
    """Get details for default prediction server"""
    try:
        prediction_server = dr.PredictionServer.list()[0]
        pred_key = prediction_server.datarobot_key
        pred_endpoint = prediction_server.url
        return (pred_endpoint, pred_key)
    except:
        raise Exception("No prediction server found")


class DataRobotPredictionError(Exception):
    """Raised if there are issues getting predictions from DataRobot"""


def make_timeseries_prediction(data,
                               url,
                               deployment_id,
                               token,
                               key,
                               forecast_point=None):
    """
    Make predictions on data provided using DataRobot deployment_id provided.
    See docs for details:
         https://app.datarobot.com/docs/users-guide/predictions/api/new-prediction-api.html
 
    Parameters
    ----------
    data : str
        Feature1,Feature2
        numeric_value,string
    deployment_id : str
        The ID of the deployment to make predictions with.
    forecast_point : str, optional
        The as of timestamp in ISO format
 
    Returns
    -------
    Response schema:
        https://app.datarobot.com/docs/users-guide/predictions/api/new-prediction-api.html#response-schema
 
    Raises
    ------
    DataRobotPredictionError if there are issues getting predictions from DataRobot
    """
    # Set HTTP headers. The charset should match the contents of the file.
    headers = {
        'Content-Type': 'text/plain; charset=UTF-8',
        'datarobot-key': key,
        'Authorization': 'Token {}'.format(token)
    }
    url = '{url}/predApi/v1.0/deployments/{deployment_id}/timeSeriesPredictions'.format(
        url=url, deployment_id=deployment_id)
    params = {'forecastPoint': forecast_point}
    # Make API request for predictions
    predictions_response = requests.post(url,
                                         data=data,
                                         headers=headers,
                                         params=params)
    _raise_dataroboterror_for_status(predictions_response)
    # Return a Python dict following the schema in the documentation
    return predictions_response.json()


def _raise_dataroboterror_for_status(response):
    """Raise DataRobotPredictionError if the request fails along with the response returned"""
    try:
        response.raise_for_status()
    except requests.exceptions.HTTPError:
        err_msg = '{code} Error: {msg}'.format(code=response.status_code,
                                               msg=response.text)
        raise DataRobotPredictionError(err_msg)


def parse_dr_predictions(raw, timeseries=False, passthrough=False):
    """Convert json to pandas dataframe"""
    preds = raw['data']
    keep_cols = []
    if passthrough:
        keep_cols.append('passthroughValue')
    if timeseries:
        keep_cols = keep_cols + ['forecastPoint', 'timestamp', 'series']
    else:
        keep_cols.append('rowId')
    return pd.json_normalize(preds,
                             'predictionValues',
                             keep_cols,
                             errors='ignore')


def setup_basic_time_spec(cf):
    """
    Basic spec for timeseries, using a config.
    Assumes daily data, and no gap to prediction window.
    """
    spec = dr.DatetimePartitioningSpecification(
        cf['timecol'], use_time_series=True, default_to_known_in_advance=False)
    # disable holdout
    spec.disable_holdout = True
    # backtest options
    spec.number_of_backtests = int(cf['backtests'])
    spec.validation_duration = dr.partitioning_methods.construct_duration_string(
        days=int(cf['backtest_length']))
    # windows
    spec.feature_derivation_window_start = int(cf['fdw'])
    spec.feature_derivation_window_end = 0
    spec.forecast_window_start = 1
    spec.forecast_window_end = int(cf['horizon'])
    return spec


def get_feature_list(project, old_project, model):
    """Get or recreate a featurelist from another project"""
    flists = project.get_modeling_featurelists()
    match = [f for f in flists if model.featurelist_name == f.name]
    if len(match) > 0:
        f_id = match[0].id
    else:
        features = dr.Featurelist.get(old_project.id,
                                      model.featurelist_id).features
        match = [f for f in flists if features == f.features]
        if len(match) > 0:
            f_id = match[0].id
        else:
            f_id = project.create_modeling_featurelist(
                'p:' + old_project.id + ' m:' + model.id, features)
    return f_id


def retrain_models(project, old_project, models):
    """
    Retrain a list of models in a new project.
    Both projects must have been built from the same data otherwise errors will
    be raised when trying to match feature lists.
    """
    for m in models:
        featurelist = get_feature_list(project, old_project, m)
        try:
            job = project.train_datetime(m.blueprint_id,
                                         featurelist_id=featurelist,
                                         source_project_id=project.id)
        except:
            pass
    while len(project.get_model_jobs()) > 0:
        time.sleep(10)
    models = [
        dr.DatetimeModel.get(project.id, m.id) for m in project.get_models()
    ]
    try:
        jobs = [m.score_backtests() for m in models]
    except:
        pass
    while len(project.get_model_jobs()) > 0:
        time.sleep(10)
    return project.get_models()
