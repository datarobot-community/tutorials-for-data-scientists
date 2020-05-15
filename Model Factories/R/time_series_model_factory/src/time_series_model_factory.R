# Author: John Hawkins

library(datarobot)
library(data.table)

# ####################################################################
# Generate a range of timeseries models from the specified data
# df            - (Dataframe) A dataframe containing all data
# date_col      - (String) The time differntiaing column
# targets       - (Array) The list of target columns to build independent models
# project_name  - (String) Prefix for project name in datarobot
# result_dir    - (String) Path to write the results
# metrics       - (Array:String) The metrics to report on
#
# NOTE: There is no error checking on the parameters. 
#       Any reference to columns that do not exist will result in a fatal error.
# ####################################################################

base_url="YOUR_HOSTNAME"

datarobot_timeseries_model_factory <- function(df, date_col, targets, project_name, result_dir, metrics) {

	# WE WRITE OUT THE MODEL RESULTS INTO A TABLE
        rez_file_name = paste(project_name, "_model_list.tsv", sep='')
        resultsFile = paste(result_dir, rez_file_name, sep='/')

        page_file_name = paste(project_name, "_model_list.html", sep='')
	resultsPage = paste(result_dir, page_file_name, sep='/')

	# Write a header to the results files before we begin 
	result = tryCatch({
		rez <- file(resultsFile, "w")
                metric_str = paste(metrics, collapse = '\t')
		writeLines( paste("target\tdatarobot_project_id\tdatarobot_model_id\t", metric_str, sep=""), con=rez,sep="\n")

		page <- file(resultsPage, "w")
		writeLines( 
                   paste( "<html> <head> 
                           <title>DataRobot Models for", project_name, "</title> 
                           <link rel='stylesheet' href='style.css'>
                           </head> <body>", sep=""), 
                   con=page, sep="\n" 
                )

                metric_str = paste(metrics, collapse = "</div><div class='rTableHead'>")
                header = paste("<div class='rTable'>
                 <div class='rTableRow'>
                 <div class='rTableHead'>Model</div>
                 <div class='rTableHead'>Model Type</div>
                 <div class='rTableHead'>", metric_str, "</div>
                 </div>", sep="")
                writeLines( header, con=page, sep="\n")

	}, warning = function(w) {
    		message('Potential problem with writing your results file.')
		message(w)
		return(0)
	}, error = function(e) {
	        message('Problem with your results file. Please check the path')
                message(e)
                return(0)
	}, finally = {
    		# CLEAN UP
	})

	# Force data frame to be a data table
	dt		<- data.table(df)

        dt_partition = CreateDatetimePartitionSpecification(
		datetimePartitionColumn = date_col,
		useTimeSeries = TRUE
 	)
	
	# iterate over the keyset
	for(target in targets) { 
		# Subset the data and create the project for this key
		temp.data	<- dt
		projName 	<- paste(project_name, target, sep='_')
		temp.proj	<- SetupProject( dataSource=temp.data, projectName=projName )

		SetTarget(project=temp.proj, target=target, partition = dt_partition)
		
                UpdateProject(project = temp.proj$projectId, workerCount = -1)

                print("Auto-Pilot Started... Halting until completion")
		WaitForAutopilot(project = temp.proj)

		# Once Autopilot has finished we retrieve the most accurate model details 
                best.model	<- GetRecommendedModel(temp.proj, type = RecommendedModelType$MostAccurate)
		model.type 	<- best.model$modelType
		model.id	<- best.model$modelId
		my.metrics	<- character(length(metrics))
		for (i in 1:length(metrics)) {

		my.metrics[i]	<- tryCatch({	
					best.model$metrics[[ metrics[i] ]]$validation
				}, error = function(e) {
                			"-"
        			})
		}	
                url 		<- paste( base_url, "projects/", temp.proj$projectId, "/models/", model.id, "/accuracy-over-time", sep="")
		metric_str 	<- paste( my.metrics, collapse = "</div><div class='rTableCell'>")
                entry		<- paste( "<div class='rTableRow'>
                                          <div class='rTableCell'><a href='", url ,"'>", target, "</a></div>
                                          <div class='rTableCell'>", model.type, "</div>
                                          <div class='rTableCell'>", metric_str, "</div>
                                          </div>", sep="")
                writeLines( entry, con=page, sep="\n")
		metric_str = paste( my.metrics, collapse = '\t')
		writeLines(paste(target, temp.proj$projectId, model.id, metric_str, sep='\t'), con=rez, sep="\n")		
	}
        # FINISH THE TABLE IN THE HTML OUTPUT

        footer = paste("</body></html>")
        writeLines( footer, con=page, sep="\n")
	# CLOSE THE RESULTS FILE 
	close(page)
	close(rez)
	return(1)
} 


