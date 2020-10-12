# Anti Money Laundering using anomaly detection

`Anti Money Laundering` is the process of hiding illicitly obtained money. In this tutorial, we are going to use a historical money transactions dataset and train anomaly detection models that detect outliers. 

Keep in mind, that in this dataset, the actual frauds are already depicted in column `SAR` but we are not going to use that information on purpose as in most cases, money laundering goes by undetected and our only way to solve this problem would be anomaly detection. 

In the end, we are going to use a small subset of the data to evaluate how well the anomaly detection approach is working since we do have the labeled data.

### Getting Started

Follow the steps as outlined within the notebook file. There is also a training dataset you can review: `aml.csv`

### Problem Type
Anomaly detection - Outlier Detection