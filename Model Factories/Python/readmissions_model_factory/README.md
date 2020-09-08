# Readmissions Model Factory

This project contains scripts for building large numbers of models
using DataRobot, where each project uses a different target column 
from the same datset. 

We call this a *Multi-Target Model Factory*.

### Getting Started

Use the Notebook accompanying this folder to start.

### Requirements

This project assumes you have a valid DataRobot account and that you
have set up your account credentials in the 
[drconfig.yaml](https://datarobot-public-api-client.readthedocs-hosted.com/en/v2.19.0/setup/configuration.html) 
file so that you can use the API.
 
We assume that you have a single dataset that contains multiple target columns and a set of 
feature columns you want to reuse for each model.

Each model built will have a different target but use all non-target columns 
as features. The script provided currently assume that you want to run the full autopilot on each
target and choose the model at the top of the leaderboard. This logic is easily modified.

### Problem Type
Multi target model factory