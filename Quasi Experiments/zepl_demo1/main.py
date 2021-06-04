# This .py contains the Python code used in the 
#    Zepl notebook https://app.zepl.com/ODFHKV0LJ/notebooks/e1be0bcb11264260bede11649f0795ec

# Matching for Quasi-Experimentation with DataRobot
#   Jason Miller, CFDS

# Load Libraries
import datarobot as dr
import seaborn as sns 
import pandas as pd
import matplotlib.pyplot as plt

# Connect to DR
## Note: if running this as a .py you should replace MAIN_TOKEN with your
##          .yaml configuration file
# Connect
dr.Client(token=MAIN_TOKEN , endpoint='https://app.datarobot.com/api/v2')

# Import Data Set
## This (wget) part is for Zepl only!
# !wget http://zdata/marketing_promotional_campaign.csv
## This part is for regular Python as well:
data = pd.read_csv('marketing_promotional_campaign.csv')
## drop the target
data.drop('spend', inplace=True, axis=1)
print(data)
## write a CSV of the subset of data
data.to_csv('marketing_promo_no_target.csv')
## send the new data set to AI Catalog
dataset = dr.Dataset.create_from_file(file_path='marketing_promo_no_target.csv')

# Create a Project
project = dr.Project.create_from_dataset(dataset.id, project_name='Zepl NB: Matching for Quasi Experimentation (v2.2.1)')

# Set Target to Treatment, Run Autopilot
project.set_target(target='received_promotional_credit', # this is our treatment vs control flag (received_promotional_credit)
                   metric='LogLoss',
                   mode=dr.AUTOPILOT_MODE.FULL_AUTO)
                   
featurelist = project.create_featurelist('Omit the Target for Matching', ['gender', 'age', 'region', 'received_promotional_credit'])
project.set_worker_count(-1)
project.start_autopilot(featurelist.id) # optional: add wait for autopilot to complete

# Cross-Validate All Models
lb = project.get_models()

for model in lb:
    try:
        model.cross_validate()
    except:
        pass

# Unlock Holdouts
project.unlock_holdout()

# Get the Best Model and Learn About It
model = dr.Model.get(project=project.id,
                     model_id="60aeb7c03d594b70a8a5fcfa") # update with your model Id!
feature_impact = model.get_or_request_feature_impact()
fi_df = pd.DataFrame(feature_impact)

# Create a Deployment
prediction_server = dr.PredictionServer.list()[0]

deployment = dr.Deployment.create_from_learning_model(
    model.id, label='Propensity Scoring Deployment', description='Deployment for Zepl NB Matching for Quasi-experimentation v2.2',
    default_prediction_server_id=prediction_server.id)
deployment

# Propensity Scoring
dr.BatchPredictionJob.score_to_file(
    deployment.id,
    'marketing_promo_no_target.csv',
    './scored_results.csv')

# The next step of this process uses the R file "propensity_scoring.R"
