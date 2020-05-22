#!/bin/bash

# environment setup
source $HOME/.bash_profile
cd $(dirname "$0")/py

# make predictions
python make_predictions.py
