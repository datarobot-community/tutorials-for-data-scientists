"""
Uploads actual outcome values for past predictions.
"""

import datarobot as dr
import pandas as pd
import drutils as du

# setup
cf = du.load_config('../usecase_config.yaml')
dr.Client(config_path='../drconfig.yaml')

################################################################################

scores = pd.read_csv(cf['out_file'])
times = pd.to_datetime(scores['timestamp']).dt.strftime('%Y-%m-%d').unique()
reference = pd.read_csv(cf['ref_file'])

df = pd.read_csv(cf['dataset'])[lambda x: x[cf['timecol']].isin(times)]

# if no outcomes exist, terminate
if len(df) == 0:
    print('no actuals available for past predictions')
    exit()

df = df.assign(id = lambda x: x[cf['series']] + ' ' + x[cf['timecol']]) \
        .loc[:, [cf['series'], cf['target'], 'id']] \
        .rename(columns={'cases': 'actual_value', 'id': 'association_id'})

for idx, row in reference.iterrows():
    actuals = df[df[cf['series']] == row['use_case']]
    deployment = dr.Deployment.get(deployment_id=row['deployment_id'])
    deployment.submit_actuals(actuals.drop(columns=cf['series']))
