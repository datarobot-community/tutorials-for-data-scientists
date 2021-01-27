<!--
*** Thanks for checking out the Best-README-Template. If you have a suggestion
*** that would make this better, please fork the repo and create a pull request
*** or simply open an issue with the tag "enhancement".
*** Thanks again! Now go create something AMAZING! :D
***
***
***
*** To avoid retyping too much info. Do a search and replace for the following:
*** github_username, repo_name, twitter_handle, email, project_title, project_description
-->



<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->








<!-- TABLE OF CONTENTS -->
<details open="open">
  <summary><h2 style="display: inline-block">Table of Contents</h2></summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a>
      <ul>
        <li><a href="Assumptions">Assumptions</a></li>
      </ul>
    </li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgements">Acknowledgements</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

To show how to partition dataset into training, validation, and testing sets, how to tune parameters and retrain a model, how to build a blender, and how to extract performance metrics in DataRobot Python API.
We are using fake_job_postings dataset from http://emscad.samos.aegean.gr/ to showcase how to control DataRobot, and expand ones ability to build complex models.  This dataset has a mixture of fake and legitimate job postings.  Our job is to build a model that can triage the fake postings from the legitimate job postings.  The dataset has many challenges:  extremely unbalanced, and has 5 text features.  The code shows how to partition this dataset, given its particularity, how to customize models, how to blend these models with other models, and how to extract performance metrics from DataRobot. 
The code is in ./src, the dataset and the performance metric table are in ./Data.  The dataset is compressed due to Github space limitation. Please expand the file before running the code.  





<!-- GETTING STARTED -->
## Getting Started

To get a local copy up and running follow these simple steps.
go to 
    ./Blender_Models/src
open fake_jobs_posting.py
set token_id to your DataRobot token
run
    python fake_jobs_posting.py 
### Prerequisites
requirements.txt has the needed packages.  In short, you need DataRobot, numpy, pandas, and regex


<!-- USAGE EXAMPLES -->
## Usage

You will need to put your datarobot in 
token_id = ""

You can set some parameters in  ts_setting variable:

ts_setting = {"project_name":"fake_job_posting_210123","filename":"../Data/fake_job_postings.csv",     \
              "project_id": "","model_id":"",     \
              "feature_list": "Informative Features","features":[],"set":"holdout" , \
              "AUC":"Weighted AUC", "LogLoss":"Weighted LogLoss",                                    \
              "downsampling": 36,"holdout_pct": 20,"validation_pct":16,"target":"fraudulent" }


### Assumptions 

There are at least 2 text features
Please check the code for the functions and their purpose  



<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE` for more information.



<!-- CONTACT -->
## Contact

Dalila Benachenhou - Dalila.benachenhou@datarobot.com

Project Link: [https://github.com/datarobot-community/tutorials-for-data-scientists-wip/new/DalilaB000-patch-1/Model%20Factories/Python/Blend_Models](https://github.com/datarobot-community/tutorials-for-data-scientists-wip/new/DalilaB000-patch-1/Model%20Factories/Python/Blend_Models)







<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/github_username/repo.svg?style=for-the-badge
[contributors-url]: https://github.com/github_username/repo/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/github_username/repo.svg?style=for-the-badge
[forks-url]: https://github.com/github_username/repo/network/members
[stars-shield]: https://img.shields.io/github/stars/github_username/repo.svg?style=for-the-badge
[stars-url]: https://github.com/github_username/repo/stargazers
[issues-shield]: https://img.shields.io/github/issues/github_username/repo.svg?style=for-the-badge
[issues-url]: https://github.com/github_username/repo/issues
[license-shield]: https://img.shields.io/github/license/github_username/repo.svg?style=for-the-badge
[license-url]: https://github.com/github_username/repo/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/github_username
