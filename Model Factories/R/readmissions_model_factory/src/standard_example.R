source("DataRobot_multitarget_model_factory.R")

dataset = "data/Hospital_Readmission.csv"

df 		<- read.csv(dataset)
targets 	<- c("readmitted", "readmission_count", "length_of_stay") 
project_name 	<- "Hospital_Models" 
result_dir 	<- "results"
metric 		<- "RMSE"

datarobot_multitarget_model_factory(df, targets, project_name, result_dir, metric)


