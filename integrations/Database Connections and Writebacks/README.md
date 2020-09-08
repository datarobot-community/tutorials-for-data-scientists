# Database Connections and Writebacks in DataRobot

DataRobot provides a “self-service” JDBC product for database connectivity setup. Once configured, you can read data from production databases for model building and predictions. This allows you to quickly train and retrain models on that data, and avoids the unnecessary step of exporting data from your enterprise database to a CSV for ingest to DataRobot.

This notebook also goes over the prediction API and `BatchPredictionJob` utility in the `datarobot` Python sdk for setting up large-scale scoring jobs with database inputs and outputs. 