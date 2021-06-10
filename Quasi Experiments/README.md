# PSM for Quasi-Experimentation with DataRobot

This repository contains copies of two Zepl notebooks used to demonstrate quasi-experimentation with DataRobot using Python and R. Propensity Score Matching (PSM) is the approach leveraged to conduct the quasi-experiments. This allows us to estimate treatment effects (i.e., causal inference) with less bias and greater accuracy than traditional regression modeling.

For more information about PSM and the notebooks provided here, see the [accompanying article](https://community.datarobot.com/t5/blog/welcome-to-conducting-quasi-experiments-with-datarobot/ba-p/11757) in the DataRobot Community.

## Usage

Make sure the required libraries are installed in your Zepl image (or in Python and R, if running directly). Python requires datarobot and pandas. R requires caret, e1071, and Matching. Code to install the R libraries are included in the R script and Zepl notebook; Python libraries can be installed with pip.

## Repository Contents

This repo contains two subdirectories corresponding to two Zepl notebooks (PSM demos). Each subdirectory contains the Zepl notebook as a .pdf, .zpln, and .json file. Additionally, Python and R code has been transcribed to .py and .R files for those who do not wish to use Zepl.

## Development and Contributing

If you'd like to report an issue or bug, suggest improvements, or contribute code to this project, please refer to [CONTRIBUTING.md](CONTRIBUTING.md).


# Code of Conduct

This project has adopted the Contributor Covenant for its Code of Conduct.
See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) to read it in full.

# License

Licensed under the Apache License 2.0.
See [LICENSE](LICENSE) to read it in full.
