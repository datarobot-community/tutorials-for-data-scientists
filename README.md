# Tutorials for Data Scientists

This repository contains various end to end use-case examples using the DataRobot API. Each use-case directory contains instructions for its own use.

## Usage

For each respective guide, follow the instructions in its own `.ipynb` or `.Rmd` file. 

**Please pay attention to the different DataRobot API Endpoints**

The API endpoint you specify for accessing DataRobot is dependent on the deployment environment, as follows:

- AI Platform Trial—https://app2.datarobot.com/api/v2
- US Managed AI Cloud—https://app.datarobot.com/api/v2
- EU Managed AI Cloud—https://app.eu.datarobot.com/api/v2
- On-Premise—https://{datarobot.example.com}/api/v2 
       (replacing {datarobot.example.com} with your specific deployment endpoint
       
The DataRobot API Endpoint is used to connect your IDE to DataRobot.

## Important Links

- To learn to use DataRobot, visit [DataRobot University](https://university.datarobot.com/)
- For General articles on DataRobot and news, visit [DataRobot Community](https://community.datarobot.com/)
- Simple example scripts [Examples for Data Scientists](https://github.com/datarobot-community/examples-for-data-scientists)

## Contents

### Classification 

- *Lead Scoring for selling online courses:* predict who is likely to become a customer using  binary classification strategy.  Create a custom feature list.  Get the ROC Curve, Feature Impact and Feature Effects. Plot them for analysis. Retrain your model and make predictions. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Classification/Python/lead_scoring/src/Lead%20Scoring%20Tutorial.ipynb)

- *Predict Hospital Readmissions:* predict which patients are likely to be readmitted within 30 days after being discharged using binary classification. Install the software, find your API token, choose the best model, get the evaluation metrics, and make predictions.  [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Classification/Python/predict_hospital_readmissions/src/readmissions_tutorial.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Classification/R/predict_hospital_readmissions/src/readmissions_tutorial.ipynb)

- *Predict COVID-19 at the County Level:*  predict high risk counties with a look-alike modeling strategy.  Build a binary classification model and rank each county by the probability of seeing cases.  Set up the project, get evaluation and interpretability metrics, plot results, and get prediction explanations.  [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Classification/Python/predicting_covid_at_county_level/src/Covid_blog.ipynb). 

- *Predict Medical Fraud:*  predict fraudulent medical claims with binary classification.   Connect to a SQL database, create a data store, write custom functions to build multiple projects, conduct anomaly detection and deploy the model using the prediction server. Save the results for a custom dashboard. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Classification/Python/predicting_fraud_medical_claims/src/Predicting%20Fraud%20Medical%20Claims.ipynb)

- *Lead Scoring Bank Marketing:* predict which customers are likely to purchase a product or service in response to a bank telemarketing campaign.  Upload data, create a project, get and plot the ROC curve and Feature Impact.  Get the holdout predictions. [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Classification/R/Lead%20Scoring%20Bank%20Marketing/Lead_Scoring.Rmd)

### DRU 

*API Training:* The DataRobot API Training is targeted at data scientists and motivated individuals with at least basic coding skills who want to take automation with DataRobot to the next level. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/DRU/API_Training/Python) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/DRU/API_Training/R)

Here you will be able to learn how to use the DataRobot API through a series of exercises that will challenge you, and teach you how to solve some of the most common problems that people run into.

Start by reading carefully the "API Training - Introductory Notebook" [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Python%20API%20Training%20-%20Introductory%20Notebook.ipynb) or [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/R%20API%20Training%20-%20Introductory%20Notebook.ipynb). This will help you learn the basics and provide a concrete overview for the API. Afterwards, go within the /exercises folder and start downloading and solving the exercises.

The list of exercises is as follows:

- Exercise 1 Feature Selection Curves [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Exercises/1.%20Python%20API%20Training%20-%20Feature%20Selection%20Curves%20%5BExercise%5D.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/Exercises/1.%20R%20API%20Training%20-%20Feature%20Selection%20Curves%20%5BSolution%5D.Rmd)

- Exercise 2. Advanced Feature Manipulation [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Exercises/2.%20Python%20API%20Training%20-%20Advanced%20Feature%20Manipulation%20%5BExercise%5D.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/Exercises/2.%20R%20API%20Training%20-%20Advanced%20Feature%20Manipulation%20%5BSolution%5D.Rmd)

- Exercise 3. Model Documentation [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Exercises/3.%20Python%20API%20Training%20-%20Model%20Documentation%20%5BExercise%5D.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/Exercises/3.%20R%20API%20Training%20-%20Model%20Documentation%20%5BSolution%5D.Rmd)

- Exercise 4. Beyond AutoPilot [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Exercises/4.%20Python%20API%20Training%20-%20Beyond%20AutoPilot%20%5BExercise%5D.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/Exercises/4.%20R%20API%20Training%20-%20Beyond%20AutoPilot%20%5BSolution%5D.Rmd)

- Exercise 5. Model Factory [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Exercises/5.%20Python%20API%20Training%20-%20Model%20Factory%20%5BExercise%5D.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/Exercises/5.%20R%20API%20Training%20-%20Model%20Factory%20%5BSolution%5D.Rmd)

- Exercise 6. Continuous Model Training [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Exercises/6.%20Python%20API%20Training%20-%20Continuous%20Model%20Training%20%5BExercise%5D.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/Exercises/6.%20R%20API%20Training%20-%20Continuous%20Model%20Training%20%5BSolution%5D.Rmd)

- Exercise 7. Using a Database [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/Python/Exercises/7.%20Python%20API%20Training%20-%20Using%20a%20Database%20%5BExercise%5D.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/DRU/API_Training/R/Exercises/7.%20R%20API%20Training%20-%20Using%20a%20Database%20%5BSolution%5D.Rmd)

### Model Factories

- *Classification Model Factory:*  create a model factory for a binary classification problem using our readmissions dataset.  Predict the likelihood of patient readmission.  Build a single project and find the best model.  Then build more projects based on admission id.  Find the best model for each sub-project.  Make this model ready for deployment. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Model%20Factories/Python/readmissions_model_factory/Model%20Factory%20with%20Diabetes%20Readmission%20Dataset.ipynb) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Model%20Factories/R/readmissions_model_factory/src/readmissions_model_factory.R)

- *Time Series Model Factory:* create a time series model factory using our store sales multiseries dataset.  Set up a time series multiseries project. Get the best model and it’s performance. Cluster the data and create plots over time. Create a project for each cluster and evaluate the results. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/Model%20Factories/Python/time_Series_store_sales_model_factory) [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Model%20Factories/R/time_series_model_factory/src/time_series_model_factory.R)

### Model Management

- *Automated Retraining and Replacement of Models:* automatically retrain and replace models. An automated continuous training pipeline. [Python/cURL](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/Model%20Management/Automated%20Retraining%20and%20Replacement)

- *Monitoring Drift and Replacing Models:* monitor your deployment for data drift and replace the model once a criteria is met.  Connect to a SQL server and create a data store. Create a project based on the data source. Deploy the recommended model and setup drift tracking settings.  Upload and make predictions on a dataset with drift. Check the drift results and replace the model.  [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Model%20Management/Monitoring%20Drift%20and%20Replacing%20Model/src/Model_monitoring_data_drift_detection.ipynb)

### Multiclass

- *Multiclass one-vs-rest Modeling:*  create a one-vs-rest model to do geophysical classification with 9 potential classes.  Preprocess the data and split up the dataset. Use a loop to build nine projects and put the result into a dataframe  Then get the predictions and plot them with an advanced visualization technique. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Multiclass%20Classification/one-vs-rest-with-datarobot/src/One%20vs%20Rest%20with%20DataRobot.ipynb)

- *Predicting Product Type Based on Customer Complaints* Use the **free text** from customer complaints to predict which product the customers are adressing. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/Multiclass%20Classification/Predict%20Product%20Type%20Based%20on%20Customer%20Complaints)

### Out of Time Validation (OTV)

- *Predict C02 levels of Mauna Loa:*  create an OTV project to predict C02 levels.  This project trains on older data and then validates on newer data.  This strategy is done because scientists in this case know that the data changes.  Import your data, create lagged features, define date-time partitioning, select a model and get Feature Impact. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Multiclass%20Classification/one-vs-rest-with-datarobot/src/One%20vs%20Rest%20with%20DataRobot.ipynb)

### Regression

- *Double Pendulum with Eureqa Models:* solve a regression problem using Eureqa blueprints. Eureqa makes no prior assumptions about the data set, instead fitting - models to the data dynamically. The models are presented as mathematical equations, so end users can seamlessly understand results and recommendations. Setup a manual mode project and select Eureqa blueprints from the repository. Advance tune the default model and print the mathematical expression. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Regression/Python/double_pendulum_with_eureqa/src/Double%20Pendulum%20with%20Eureqa%20Models.ipynb)

- *Analysing Residuals to build better models:* Use residuals created by DataRobot insights to evaluate your models and make them better. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/Regression/Python/analysing_residuals_to_build_better_models)

### Time Series

- *Forecasting US COVID-19 Cases Using Time-Series:*  Create an AutoTS model on historical data taken from the US, France and Spain.  Clean and prepare the data. Create the time series project and build models. Forecast 10 days ahead for each country an write the results to a .csv file. [R](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/Time%20Series/COVID%20Time%20Series%20Forecasting%20in%20R/COVID%20Time%20Series%20Forecasting%20With%20R/COVID_TS.Rmd)

### VisualAI 
- *VisualAI Heartbeats:* Create a VisualAI project to classify images of sound.  Heartbeats of people with normal and atypical heart conditions were recorded onto .wav files.  This code shows you how to create spectrograms from the images and import them into DataRobot for VisualAI classification.  [Python](https://github.com/datarobot-community/tutorials-for-data-scientists-wip/blob/visualAI_changes/VisualAI/Python/VisualAI%20Heartbeats/heartbeat_visual_AI.ipynb)

- *Detecting Droids with DataRobot:* Create a VisualAI project to classify images of droids and create a custom shiny application.  Build file paths to images and set up folders for VisualAI.  Import that data in the platform and create image classification models.  Get evaluation metrics and plot them with ggplot.  Create a deployment using the prediction server.  Make a shiny app that hits the deployment. [R](https://github.com/datarobot-community/tutorials-for-data-scientists-wip/blob/visualAI_changes/VisualAI/R/Detecting%20Droids/src/Droids%20Demo/Droids_R.Rmd)

### Anomaly Detection (Unsupervised Learning)

- *Anti Money Laundering with Outlier Detection:* Create an unsupervised model that can predict money laundering related transactions. Use a small set of labeled data to evaluate how the different models can perform. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/Anomaly%20Detection%20(Unsupervised%20Learning))

### Integrations

- *Amazon Web Services (AWS):*  This repository has a number of solutions for using DataRobot with AWS. Use DataRobot prime and scoring code models with AWS Lambda. Also use DataRobot scoring code models with AWS Sagemaker. [cURL](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/integrations/AWS%20(Amazon%20Web%20Services)/readme.md)

- *Database Connections and Writebacks:* DataRobot provides a “self-service” JDBC product for database connectivity setup. Once configured, you can read data from production databases for model building and predictions. This allows you to quickly train and retrain models on that data, and avoids the unnecessary step of exporting data from your enterprise database to a CSV for ingest to DataRobot. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/blob/master/integrations/Database%20Connections%20and%20Writebacks/databases_and_deployment.ipynb)

### Feature Discovery

- *Feature Discovery  with Instacart Dataset:* An example of how to use Feature Discovery through the Python API. [Python](https://github.com/datarobot-community/tutorials-for-data-scientists/tree/master/Feature%20Discovery/Feature%20Discovery%20%20with%20Instacart%20Dataset)

## Development and Contributing

If you'd like to report an issue or bug, suggest improvements, or contribute code to this project, please refer to [CONTRIBUTING.md](CONTRIBUTING.md).

# Code of Conduct

This project has adopted the Contributor Covenant for its Code of Conduct. 
See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) to read it in full.

# License

Licensed under the Apache License 2.0. 
See [LICENSE](LICENSE) to read it in full.


