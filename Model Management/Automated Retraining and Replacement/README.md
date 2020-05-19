# auto_retrain

This repository is intended to be used as a starting template to setup a basic pipeline to automatically replace models.

The dataset used is sourced from the John Hopkins University COVID-19 repository [here](https://github.com/CSSEGISandData/COVID-19).
A number of example timeseries models are built on each of the Australian states, and deployments are created for each.
A text file is used to store the deployment ids and corresponding state (in reality this would likely be stored in a table or a keystore).

This project was set up in April 2020, as daily case numbers were being updated. On an ongoing basis, cron jobs can be set up to:

* pull the latest data, and upload as actuals to the respective deployments
* compare accuracy of models against actuals, and
    * retrain and redeploy if recent performance exceeds a threshold
* make predictions for the next day

## Installation

Prerequisites: python installed, DataRobot account, cron

Update your API token and install location
```
<editor of choice> drconfig.yaml
```

```
pip install -r requirements.txt
```

## Running

This template uses a number of python scripts to interact with the DataRobot API.
These scripts utilize a yaml config file `usecase_config.yaml`,
and are intended to be use with timeseries modelling.

For initial setup run
```
<full_path>/run_data_extract.sh
cd py
python create_initial_deployments.py
```

Note that this creates a number of autopilot projects, and so will take some time to run -
you can restrict the number of series to speed things up in the config.

Once initial models are setup, there are four main components that can be scheduled to run on a regular basis.
We recommend editing these to suit your use case, and once done, add these lines to your user crontab:
```
# refresh data every night at 20:00
0 20 * * * <full_path>/run_data_extract.sh
# update actuals for past predictions
5 20 * * * <full_path>/run_actuals_upload.sh
# update deployments if necessary
10 20 * * * <full_path>/run_accuracy_check.sh
# make predictions for tomorrow
0 21 * * * <full_path>/run_predictions.sh
```

These scripts all source your bash profile before running, to emulate your user environment.
Change as necessary.
