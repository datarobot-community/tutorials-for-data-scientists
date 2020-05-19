#!/usr/bin/env bash
cd /lambda_prime_test/
mvn clean
mvn package
export AWS_DEFAULT_PROFILE=ADD_AWS_PROFILE_HERE
aws lambda update-function-code --function-name testLambda --zip-file fileb://target/lambdatest-1.0-SNAPSHOT.jar --region us-east-1