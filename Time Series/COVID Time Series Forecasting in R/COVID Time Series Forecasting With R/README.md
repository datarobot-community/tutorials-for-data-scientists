# Performing Far Forecast with Narrow Time Window, using R DataRobot Library

In early March I wanted to forecast the spread of COVID-19. However, at that time the number of days since the first US case of COVID-19 was really too short to perform any reliable long-term forecasting; at most, I could forecast only two days ahead. While this may appear to be an issue, it can be solved by combining the new forecast with the historical time series to predict another new forecast. The code here shows you how to do this by using the Time Series R DataRobot package.

See the [article](hhttps://community.datarobot.com/t5/ai-ml-knowledge-base/tkb-p/AI-ML-Knowledge-Base) in the DataRobot Community that accompanies my research and my code.

### Getting Started

These instructions will get a copy of my project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

* Packages needed:  **datarobot**, **readr**, **dplyr**, **tidyr**, **quantmod**, and **purrr**.
* You need to have an API Key for DataRobot. Under your profile in the DataRobot UI, select **Developer Tools** and copy an existing API key or create a new one. See [here](https://api-docs.datarobot.com/docs/api-access-guide) if you need more details.
* Also make sure to set the path to the file
=======
You will need to install:  datarobot, and to make sure you have readr, dplyr, tidyr, quantmod, and purrr You will also need your toke from DataRobot, which you request from going to Developer Tools under the human logo

Make sure to set up the path to the file

I've provided you the original file containing the COVID-19 global confirmed cases (time_series_covid19_confirmed_global_narrow0504.csv), but you can download the most reacent one from here:
https://data.humdata.org/dataset/novel-coronavirus-2019-ncov-cases

### Problem Type

Time Series Regression

In this repo, you will find the original file containing the global-wide confirmed cases of COVID-19 (`time_series_covid19_confirmed_global_narrow0504.csv`), but you can download the most recent one from [here](https://data.humdata.org/dataset/novel-coronavirus-2019-ncov-cases).
