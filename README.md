[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

sm-support-frontend
================================

How to run
==========

```sbtshell
sbt run
```

This will start the application on port **1024**

You also need to provide the path to config using a -D arg
```sbtshell
sbt -DsmPath=/path/to/sm/config -DgithubOrg=your-github-org-name run
```
How to test
===========
```sbtshell
sbt clean coverage test coverageReport
```

Features
========
- See currently running services based your teams sm profile
- Search through all available profiles
- Search through all available services
- See which ports are available to use
- See conflicting port usages
- Catalogue of test routes for services
- See currently available versions of assets frontend
- Generate config
