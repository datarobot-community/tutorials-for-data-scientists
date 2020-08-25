# Performing Far Forecast when time-series are short With R DataRobot Library

In Early march, the number of days since the first US case was extremely short to perform any reliable long-term forecasting.  The most I could forecast was 2 days ahead.  While this may appear an issue.  It can be solved by combining the new forecast with the historical time-series to predict another new forecast.  The code here shows you how to do so using Time-Series R DataRobot lobrary.

### Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

You will need to install:  datarobot, and to make sure you have readr, dplyr, tidyr, quantmod, and purrr You will also need your toke from DataRobot, which you request from going to Developer Tools under the human logo

Make sure to set up the path to the file

I've provided you the original file containing the COVID-19 global confirmed cases (time_series_covid19_confirmed_global_narrow0504.csv), but you can download the most reacent one from here:
https://data.humdata.org/dataset/novel-coronavirus-2019-ncov-cases

### Problem Type

Time Series Regression


