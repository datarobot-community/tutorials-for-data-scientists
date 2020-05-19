"""
Make predictions using deployed DataRobot models.
Uses a reference file to identify what predictions need to be made, and the
associated deployments.
"""

import pandas as pd
import drutils as du
import yaml

# setup
cf = du.load_config('../usecase_config.yaml')

# credentials
token = du.load_config('../drconfig.yaml')['token']
url, pred_key = du.get_default_pred_server_info()

################################################################################

# dataframes
df = pd.read_csv(cf['dataset'])
reference = pd.read_csv(cf['ref_file'])
try:
    output = pd.read_csv(cf['out_file']).drop_duplicates()
except:
    output = pd.DataFrame()

# iterate through deployments
for idx, row in reference.iterrows():
    # generate prediction dataset with association id
    pred_data = df[df[cf['series']] == row['use_case']] \
            .sort_values(cf['timecol']) \
            .tail(int(cf['history']))
    predict_date = max(pred_data[cf['timecol']])
    future_dates = pd.date_range(predict_date,
                                 periods=int(cf['horizon']) + 1,
                                 freq='D',
                                 closed='right')
    pred_data = pred_data.append(
            pd.DataFrame({
                cf['timecol']: future_dates.strftime('%Y-%m-%d'),
                cf['series']: row['use_case']
            }), sort=False) \
            .assign(id = lambda x: x[cf['series']] + ' ' + x[cf['timecol']])
    # call deployment
    response = du.make_timeseries_prediction(pred_data.to_csv(index=False),
                                             url, row['deployment_id'], token,
                                             pred_key)
    # parse json
    preds = du.parse_dr_predictions(response, timeseries=True) \
            .assign(series = row['use_case'])
    output = output.append(preds)

# write scores
output.round(2).drop_duplicates().to_csv(cf['out_file'], index=False)
