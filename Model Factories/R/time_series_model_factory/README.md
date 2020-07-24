Time Series Model Factory 
===============================================

This project contains sample code of how to initiate a time series project using the API. This can be useful when you want to build a single model for each seperate time series (in a multi-series project) to squeeze more accuracy out.

### Assumptions

This project assumes you have a valid DataRobot account and that you
have set up your account credentials in the 
[drconfig.yaml](https://datarobot-public-api-client.readthedocs-hosted.com/en/v2.19.0/setup/configuration.html) 
file so that you can use the API.
 
We assume that you have R installed with the [DataRobot R Package](https://cran.r-project.org/web/packages/datarobot/index.html).


### Instructions

The script [time_series_model_factory.R](src/timeseries_example.R) shows how to do it for a time series project.
