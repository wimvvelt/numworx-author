# numworx-author

Installable Java application to run Numworx Author on your workstation.

## Introduction

Using the author_runner java application, it is easy to load and cache all necessary resources to run the Numworx Author application.
First the author_runner application is downloaded and installed through [jdeploy.com](https://jdeploy.com) for Windows, MacOSX and Linux.
Then de application bootstraps an OSGi runtime to start de Numworx Author application from internet.

## Prerequisites

All sources are build using Maven with a Java 11 runtime.
To convert the main and supporting jars to an application, you may use jdeploy. Otherwise you may use de jpackage packager from Java-17
You may need a github and/or npm account.

## Contents 

### Folder structure

This project contains three maven modules. The provisioning module is a fork of the OSGi provisioning service implementation by Jeremias Maerki.
The MicroBoot module is provisioned and installs additional OSGi services and starts the remote bootstrap.
The author_runner module provides an Apache Felix OSGi environment and handles the lifecycle of Numworx Author bundles at https://app.dwo.nl

## Usage

This is a maven project, written in Java-11. To use:
1. mvn install 
1. cd author_runner
1. mvn exec:java 

When uploaded to github, the Github runner creates a installable application at [https://www.jdeploy.com/gh/wimvvelt/numworx-author](https://www.jdeploy.com/gh/wimvvelt/numworx-author)

## License

This work is licensed under the Apache 2.0 License.
The OSGI provisioning implementation copyright (2011) Jeremias Maerki, Switserland
Copyright 2024 Utrecht University, all rights reserved.

## Contact 

Contact [w.p.g.vanvelthoven@uu.nl](mailto:w.p.g.vanvelthoven@uu.nl)
Website: https://github.com/wimvvelt/numworx-author