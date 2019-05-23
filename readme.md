# Appdate

[![Build Status](https://travis-ci.com/binarynoise/Appdate.svg?branch=master)](https://travis-ci.com/binarynoise/Appdate)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=binarynoise/Appdate)](https://dependabot.com)

Appdate looks for updates of other apps 
by scanning the website where the updates are released.

Updates are fetched when you open the app and every 8 hours
when connected to wifi. (No setting for that yet)

Appdate also synchronizes a list of templates, so if one user added an app, 
all the other users also get the app added, if it is installed.

## Example:  
 - Appdate is released at 
 `github.com/binarynoise/Appdate/releases`.

**Important**: you need to point to the site where you can
actually download the `.apk` as Appdate scans the `html` for 
links to these files. 

## currently supported
 - [x] github.com _public_ releases (and similar websites)
 - [x] F-droid (beta, not completely tested)
 - [ ] AndroidFileHost 
