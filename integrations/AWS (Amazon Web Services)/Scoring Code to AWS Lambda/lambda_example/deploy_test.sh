#!/usr/bin/env bash
cd /lambdatest/
mvn clean
mvn package
export AWS_DEFAULT_PROFILE=ADD_AWS_PROFILE_HERE
aws lambda update-function-code --function-name <add lambda name here> --zip-file fileb://target/lambdatest-1.0-SNAPSHOT.jar --region <add AWS region here>