# Appdate

[![Build Status](https://travis-ci.com/binarynoise/Appdate.svg?branch=master)](https://travis-ci.com/binarynoise/Appdate)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=binarynoise/Appdate)](https://dependabot.com)

Appdate looks for updates of open-source apps 
by scanning the website where the updates are released.

Updates are fetched when you open the app and every 4 hours
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
 - [x] F-droid repository (beta, not completely tested, if available prefer github releases)
 - [ ] Xposed repository (not supported due to different version counting)
 - [ ] AndroidFileHost 

_Note_: please double-check if the version found by Appdate is the same as you'd expect because
some developers use a different versioning (like xposed) in their filenames than in their app.  
Example: version in app manifest `v2.5.3`, version in filename `253`

## License
see [`License.md`](LICENSE.md) for license details
