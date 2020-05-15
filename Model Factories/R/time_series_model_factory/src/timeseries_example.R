source("DataRobot_timeseries_model_factory.R")

dataset = "../data/Sales.csv"

df 		<- read.csv(dataset)
targets 	<- c("product_A", "product_B") 
date_field	<- "date"
project_name 	<- "Sales_Models" 
result_dir 	<- "results"
metrics 	<- c("MAE", "MAPE", "RMSE", "MASE")

datarobot_timeseries_model_factory(df, date_field, targets, project_name, result_dir, metrics)

