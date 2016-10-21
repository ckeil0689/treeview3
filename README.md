TREEVIEW 3.0
============

TreeView is an open-source Java app for visualizing large data matrices. It can load a dataset, cluster it, browse it, customize its appearance and export it (or parts of it) into a figure.

![treeview_screenshot.png](https://bitbucket.org/repo/AXqk7r/images/101136785-treeview_screenshot.png)


DOWNLOAD
========

** [Alpha 3 (current release)](https://bitbucket.org/TreeView3Dev/treeview3/downloads/tv3_alpha03_release.jar) ** - released on July 5, 2016

More recent "bleeding edge" releases, as well as older stable versions, are available at the [Treeview Wiki page](https://bitbucket.org/TreeView3Dev/treeview3/wiki/Home).


INSTALL & RUN
=============

## Requirements ##

* Java 7 or higher - visit <http://java.com/en/download/installed.jsp> to verify your current Java version.
* If you tested an older version of Treeview 3.0, we recommend that you reset your preferences before you upgrade to the latest version (File > Preferences > Reset Preferences). 

## Windows ##

Place the downloaded JAR file anywhere on your computer and run it by double-clicking on the icon.

## Mac ##

Place the downloaded JAR file anywhere on your computer.

**First time**: right-click on the icon and choose `Open with` > `Jar Launcher.app`.

**Every time after that**: double-click on the icon.


WHAT'S NEW
==========

## In Treeview 3.0 ##

* We have completely re-designed the interface to include one main data panel, with zooming, scrolling and searching options.
* We have made clustering an integral part of TreeView (no need for Cluster 3.0).
* We have added more flexible color settings (multiple colors, varying ranges) and label settings.
* We have eliminated some of the features that we considered obsolete or in need of a major re-work.
* We have improved image export.

## Since Alpha 2 ##

* New window layout that optimizes screen space
* Improved navigation:
    * whizzing labels and trees
    * double-click to zoom
* New data export:
    * copy labels to clipboard
    * export matrix view to PDF, SVG, PNG, JPG and PPM
* Improved search
* Improved color selection and editing
* Other minor updates and bug fixes


DOCUMENTATION
=============

[Coming soon]


HOW TO CITE
===========

The manuscript describing Treeview 3.0 is currently in preparation. In the meantime, if you have used the latest pre-release version of the software (alpha 3) and wish to cite the source, please use the following reference:

> Keil C, Leach RW, Faizaan SM, Bezawada S, Parsons L, Baryshnikova A. (2016). Treeview 3.0 (alpha 3) - Visualization and analysis of large data matrices [Data set]. Zenodo. http://doi.org/10.5281/zenodo.160573

To cite older alpha versions of Treeview 3.0, contact us at <treeview@princeton.edu>.


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

DEPENDENCIES
============

* Java TreeView (https://sourceforge.net/projects/jtreeview/)
* FreeHep (http://java.freehep.org/license.html)
* MigLayout (http://www.miglayout.com/)
* SwingX - Autocompletion (http://www.java2s.com/Code/Jar/s/Downloadswingxcore162AutoCompletitionjar.htm)
* xml-apis/ Xerces (https://xerces.apache.org/xml-commons/)