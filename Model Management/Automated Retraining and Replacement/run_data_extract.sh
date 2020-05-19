#!/bin/bash

# environment setup
source $HOME/.bash_profile
cd $(dirname "$0")/data

# pull and clean data
wget -N https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv
python clean_data.py
