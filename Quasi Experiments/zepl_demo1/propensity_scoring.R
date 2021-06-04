# This R file contains the R code from this Zepl notebook: 
#    https://app.zepl.com/ODFHKV0LJ/notebooks/e1be0bcb11264260bede11649f0795ec
# 
# This is the 2nd part of the PSM process which began in main.py


# Inspect Model Performance
install.packages("caret")
install.packages("e1071")
install.packages("Matching")

require(caret)
require(data.table)
require(e1071)
require(Matching)

data   <- fread("marketing_promotional_campaign.csv")
scores <- fread("scored_results.csv")
df     <- cbind(data, scores)

caret::confusionMatrix(as.factor(df$received_promotional_credit_PREDICTION), as.factor(df$received_promotional_credit))

# Conduct the Propensity Score Matching and Estimate Average Effect of Treatment on the Treated
df$Tr <- ifelse(df$received_promotional_credit == T, 1, 0)

# PSM with 1-to-1 matching, allowing ties, replacement, and estimating ATT
effect       <- Match(Y             = df$spend,
                      Tr            = df$Tr,
                      X             = df$received_promotional_credit_True_PREDICTION, # Propensity Score from DR
                      estimand      = "ATT",
                      M             = 2, # Controls 1-to-1 vs many-to-1
                      ties          = T,
                      replace       = T,
                      caliper       = 0.01,
                      CommonSupport = T
)

summary(effect) # True treatment effect is 10


# Covariate Balance Before and After Matching & Pruning
MatchBalance(as.formula(Tr ~ age + gender + region), data=df, nboots=250, match.out = effect)
