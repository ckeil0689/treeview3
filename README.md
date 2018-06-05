TREEVIEW 3.0
============

TreeView is an open-source Java app for visualizing large data matrices. It can load a dataset, cluster it, browse it, customize its appearance and export it into a figure.

![treeview_screenshot.png](https://bitbucket.org/repo/AXqk7r/images/101136785-treeview_screenshot.png)


DOWNLOAD
========

** [Downloads page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home) **


INSTALL & RUN
=============

## Requirements ##

* Java 7 or higher - visit <http://java.com/en/download/installed.jsp> to verify your current Java version.

## Windows ##

1. Download the msi package from the [download page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home)
2. Double-click the downloaded msi package.
3. Follow the on-screen instructions in the TreeView3 Setup Wizard.
    * Click Next.
    * Click the checkbox to accept the license agreement.
    * Click Next.
    * If desired, change the install location & click Next.
    * Click Install.
    * Click Yes to allow installation of the app from an unknown publisher (bitbucket.org).
    * Click Finish.
4. Click the start menu in the lower left-hand corner
5. Scroll down until you see TreeView3
6. Click TreeView3
7. If you have used a previous version of TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).

## Mac ##

1. Download the dmg package from the [download page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home)
2. Double-click the downloaded dmg package.
3. Drag TreeView3.app to your Applications folder.
4. Double-click TreeView3
5. Security steps (if necessary)
    * If your security preferences prevent you from opening TreeView3, right-(or control-)click the TreeView3 app and click "Open"
    * If you are prompted to confirm open of an app downloaded from the internet from an unknown developer (Princeton University), click "Open".
6. If you have used a previous version of TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).

## Linux (Debian) ##

1. Download the deb package from the [download page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home)
2. Open a terminal.
3. cd to the location of the deb package.
4. Run the following commands.
    * `sudo apt install ./treeview3*.deb`
    * `treeview3`
5. If you have used a previous version of TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).

Note, the `./` in front of the deb package (or rather, the path to the package) is essential.  Otherwise, the installation will fail.  Alternatively, you can run `sudo apt install ./treeview3*.deb`.

## Linux (Redhat) ##

1. Open a terminal.
2. cd to the location of the rpm package.
3. Run the following commands.
    * `sudo rpm -ivh treeview3*.rpm`
    * `treeview3`
4. If you have used a previous version of TreeView3: Select File->Preferences->Reset Preferences & click reset.  You will have to restart TreeView3 afterwards.

OPTIONAL: If you would like to edit the default amount of memory to assign to TreeView3, refer to the [instructions here](https://bitbucket.org/TreeView3Dev/treeview3/wiki/OutOfMemory).



WHAT'S NEW
==========

## In Treeview 3.0 ##

* We have completely re-designed the interface to include one main data panel, with zooming, scrolling and searching options.
* We have made clustering an integral part of TreeView (no need for Cluster 3.0).
* We have added more flexible color settings (multiple colors, varying ranges) and label settings.
* We have eliminated some of the features that we considered obsolete or in need of a major re-work.
* We have improved image export.

## In the Beta 1 Release ##

* Added: Label export capabilities
* Added: Simplified installation for specific operating systems
* Fixed: File open bug
* Added: Data ticker improvements: visible data average & column/row hover averages
* Added: New label settings to control how live labels are displayed
* Fixed: "As seen on screen" export bug
* Added: Performance enhancements
* Other minor updates and bug fixes


## In the Alpha 3 Release ##

* Added: New window layout that optimizes screen space
* Added: Improved navigation:
    * always-visible "live" labels and trees
    * double-click to zoom
* Added: New data export:
    * copy labels to clipboard
    * export matrix view to PDF, SVG, PNG, JPG and PPM
* Added: Improved search
* Added: Improved color selection and editing
* Other minor updates and bug fixes


DOCUMENTATION
=============

TreeView3 is designed to be intuitive and easy to use. If anything is unclear, contact us at treeview@princeton.edu.


HOW TO CITE
===========

The manuscript describing Treeview 3.0 is currently in preparation. In the meantime, if you have used the latest pre-release version of the software (alpha 3) and wish to cite the source, please use the following reference:

> Keil C, Leach RW, Faizaan SM, Bezawada S, Parsons L, Baryshnikova A. (2016). Treeview 3.0 (alpha 3) - Visualization and analysis of large data matrices [Data set]. Zenodo. http://doi.org/10.5281/zenodo.160573


DEVELOPERS
==========

TreeView was first developed by Michael Eisen in 1998 (Treeview 1.0) and updated a few years later by Alok Saldanha (Treeview 2.0). The development team for Treeview 3.0 includes:

* Chris Keil - University of Hamburg
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
