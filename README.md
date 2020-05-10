TREEVIEW 3.0
============

TreeView is an open-source Java app for visualizing large data matrices. It can load a dataset, gui.cluster it, browse it, customize its appearance and model.export it into a figure.

![treeview_screenshot.png](https://bitbucket.org/repo/AXqk7r/images/101136785-treeview_screenshot.png)


DOWNLOAD
========

** [Downloads page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home) **


INSTALL & RUN
=============

## Requirements ##

* **Java 13** or higher - visit <https://www.java.com/en/download/help/version_manual.xml> to find out your current Java version.
* Download the Java Runtime (JRE), for example at [AdoptOpenJDK](https://adoptopenjdk.net/archive.html?variant=openjdk13&jvmVariant=hotspot).
* Alternatively, you can install Java via a package manager such as macOS homebrew: `brew cask install adoptopenjdk13`

## Windows ##

1. Download the [msi package](https://bitbucket.org/TreeView3Dev/treeview3/downloads/treeview3-beta1-win-bdc455da.msi) from the [download page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home)
2. Double-click the downloaded msi package.
3. Follow the on-screen instructions in the app.TreeView3 Setup Wizard.
    * Click Next.
    * Click the checkbox to accept the license agreement.
    * Click Next.
    * If desired, change the install location & click Next.
    * Click Install.
    * Click Yes to allow installation of the app from an unknown publisher (bitbucket.org).
    * Click Finish.
4. Click the start menu in the lower left-hand corner
5. Scroll down until you see app.TreeView3
6. Click app.TreeView3
7. If you have used a previous version of app.TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart app.TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to app.TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).

## Mac ##

1. Download the [dmg package](https://bitbucket.org/TreeView3Dev/treeview3/downloads/treeview3-beta1-osx-f4a69b16.dmg) from the [download page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home)
2. Double-click the downloaded dmg package.
3. Drag app.TreeView3.app to your Applications folder.
4. Double-click app.TreeView3
5. Security steps (if necessary)
    * If you are prompted to confirm open of an app downloaded from the internet, click "Open".
    * If your security preferences prevent you from opening app.TreeView3, dismiss the warning, then right-(or control-)click the app.TreeView3 app and click "Open".
6. If you have used a previous version of app.TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart app.TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to app.TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).

## Linux (Debian) ##

1. Download the [deb package](https://bitbucket.org/TreeView3Dev/treeview3/downloads/treeview3-beta1-debian-f4a69b16.deb) from the [download page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home)
2. Open a terminal.
3. cd to the location of the deb package.
4. Run the following commands.
    * `sudo apt install ./treeview3*.deb`
    * `treeview3`
5. If you have used a previous version of app.TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart app.TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to app.TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).

Note, the `./` in front of the deb package (or rather, the path to the package) is essential.  Otherwise, the installation will fail.  Alternatively, you can run `sudo apt install ./treeview3*.deb`.

## Linux (Redhat) ##

1. Download the [rpm package](https://bitbucket.org/TreeView3Dev/treeview3/downloads/treeview3-beta1-redhat-f4a69b16.rpm) from the [download page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home)
2. Open a terminal.
3. cd to the location of the rpm package.
4. Run the following commands.
    * `sudo rpm -ivh treeview3*.rpm`
    * `treeview3`
5. If you have used a previous version of app.TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart app.TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to app.TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).



WHAT'S NEW
==========

## In Treeview 3.0 ##

* We have completely re-designed the interface to include one main data panel, with zooming, scrolling and searching options.
* We have made clustering an integral part of TreeView (no need for gui.cluster 3.0).
* We have added more flexible color settings (multiple colors, varying ranges) and label settings.
* We have eliminated some of the features that we considered obsolete or in need of a major re-work.
* We have improved image model.export.

## In the Beta 1 Release ##

* Added: Label model.export capabilities
* Added: Simplified installation for specific operating systems
* Fixed: File open bug
* Added: Data ticker improvements: visible data average & column/row hover averages
* Added: New label settings to control how live labels are displayed
* Fixed: "As seen on screen" model.export bug
* Added: Performance enhancements
* Other minor updates and bug fixes


## In the Alpha 3 Release ##

* Added: New window layout that optimizes screen space
* Added: Improved navigation:
    * always-visible "live" labels and trees
    * double-click to zoom
* Added: New data model.export:
    * copy labels to clipboard
    * model.export matrix view to PDF, SVG, PNG, JPG and PPM
* Added: Improved search
* Added: Improved color selection and editing
* Other minor updates and bug fixes


DOCUMENTATION
=============

app.TreeView3 is designed to be intuitive and easy to use. If anything is unclear, contact us at treeview@princeton.edu.


HOW TO CITE
===========

The manuscript describing Treeview 3.0 is currently in preparation. In the meantime, please use the following reference:

> Keil C, Leach RW, Faizaan SM, Bezawada S, Parsons L, Baryshnikova A. (2016). Treeview 3.0 (beta 1) - Visualization and analysis of large data matrices [Data set]. Zenodo. http://doi.org/10.5281/zenodo.1303402


DEVELOPERS
==========

TreeView was first developed by Michael Eisen in 1998 (Treeview 1.0) and updated a few years later by Alok Saldanha (Treeview 2.0). The development team for Treeview 3.0 includes:

* Christopher Keil
* Robert Leach - Princeton University
* Srikanth Bezawada
* Faizaan Shaik
* Lance Parsons - Princeton University
* Anastasia Baryshnikova - Princeton University - <http://www.baryshnikova-lab.org>


FEEDBACK
========

If you are interested in providing feedback, you can watch this repository, create new issues or contact us at <treeview@princeton.edu>.

Also, you can subscribe to the (very low-traffic) Treeview users' mailing list at <http://eepurl.com/A2Xzf>. You will receive an email when new software versions are available.


LICENSE
=======

Treeview 3.0 is released under the MIT license, which can be found [here](https://bitbucket.org/TreeView3Dev/treeview3/src/85ca08ccd77f32f80d1f219aaf8ec23898a29828/LICENSE?at=master&fileviewer=file-view-default).
