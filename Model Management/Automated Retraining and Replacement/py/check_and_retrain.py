"""
Check accuracy of deployments, and recreate if necessary
"""

import datarobot as dr
import pandas as pd
import drutils as du

# setup
cf = du.load_config('../usecase_config.yaml')
dr.Client(config_path='../drconfig.yaml')


# you can define your own logic for accuracy,
# in reality this can be much more complex than this -
# what are the thresholds in terms of business acceptance?
# perhaps you want to iterate through all deployments first,
# return a list of errors and retrain only the worst ones
def is_accurate(deployment, start, end, baseline, err=True):
    """
    Compare accuracy of a deployment within start and end times to a baseline,
    return False if outside baseline error.
    If `err` is set to True, assumes an error metric (lower is better),
    otherwise the higher the performance the better.
    """
    accuracy = deployment.get_accuracy_over_time(metric=cf['metric'],
                                                 start_time=start,
                                                 end_time=end)
    # optionally add your own custom logic or thresholding
    # e.g. baseline = baseline * 1.2
    if accuracy.summary['value'] is None:
        return True
    if err:
        return accuracy.summary['value'] <= baseline
    else:
        return accuracy.summary['value'] >= baseline


################################################################################

reference = pd.read_csv(cf['ref_file'])

df = pd.read_csv(cf['dataset'])


def replace_timeseries_deployment(data, deployment, cf, latest_id):
    """
    Using a new data set, replace the model in a deployment.
    This pulls information from a config cf, and uses a previous project to
    potentially speed things up.
    """
    # new project name
    proj_name = 'auto retrain ' + row['use_case'] + ' ' + df[
        cf['timecol']].max()
    # if the deployment is already up to date, skip
    if deployment.label == proj_name:
        return None, None
    project = du.get_existing_project(proj_name)
    spec = du.setup_basic_time_spec(cf)
    use_auto = cf['run_autopilot']
    if project is None:
        # upload data and create project
        project = dr.Project.create(data, project_name=proj_name)
    if use_auto == 'True':
        # finalise project, and run autopilot with max workers
        try:
            project.set_target(cf['target'],
                               partitioning_method=spec,
                               metric=cf['metric'],
                               worker_count=-1)
            project.wait_for_autopilot()
        except:
            print('attempting to use previous autopilot')
        replacement = dr.ModelRecommendation.get(project.id).get_model()
    else:
        try:
            project.set_target(cf['target'],
                               mode=dr.enums.AUTOPILOT_MODE.MANUAL,
                               partitioning_method=spec,
                               metric=cf['metric'],
                               worker_count=-1)
        except:
            print('resuming from previous project')
        old_project = dr.Project.get(latest_id)
        # get best models from old projects
        n_models = int(cf['n_models'])
        # skip blenders and models without all backtests
        lb = [
            m for m in old_project.get_models()
            if m.metrics[cf['metric']]['crossValidation']
            if not 'Blender' in m.model_type
        ]
        lb.sort(key=lambda x: x.metrics[cf['metric']]['crossValidation'])
        best_models = lb[0:n_models]
        # retrain these in the new project
        lb = du.retrain_models(project, old_project, best_models)
        lb.sort(key=lambda x: x.metrics[cf['metric']]['crossValidation'])
        best_model = dr.DatetimeModel.get(project.id, lb[0].id)
        # retrain single model on latest data and deploy
        project.unlock_holdout()
        job = best_model.request_frozen_datetime_model()
        replacement = job.get_result_when_complete()
    # deploy
    print('replacing model')
    fi = replacement.get_or_request_feature_impact(120)
    deployment.replace_model(replacement.id, reason='ACCURACY')
    # update deployment name
    deployment.update(label=proj_name)
    # stats for reference
    parent = du.get_parent_model(replacement)
    return project.id, parent.metrics[cf['metric']]['crossValidation']


# search through deployment accuracy stats and replace as necessary
for idx, row in reference.iterrows():
    deployment = dr.Deployment.get(deployment_id=row['deployment_id'])
    # pull latest dates to check accuracy again
    dates = df[df[cf['series']] == row['use_case']] \
            .sort_values(cf['timecol']) \
            .loc[:, cf['timecol']] \
            .tail(int(cf['accuracy_days']))
    start = pd.to_datetime(min(dates))
    end = pd.Timestamp.now().ceil('D')
    accurate = is_accurate(deployment, start, end, row['error'])
    if not accurate:
        print('model for ' + row['use_case'] + ' is not accurate')
        data = df[df[cf['series']] == row['use_case']]
        project_id, error = replace_timeseries_deployment(
            data, deployment, cf, row['latest_project'])
        if project_id is not None:
            reference.loc[idx, 'latest_project'] = project_id
            reference.loc[idx, 'error'] = error
        else:
            print('found existing deployment - skipping')

reference.to_csv(cf['ref_file'], index=False)
