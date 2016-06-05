TREEVIEW 3.0
============

TreeView is an open-source Java app for visualizing large data matrices. It can load a data matrix, cluster it, browse it, customize its appearance and export it (or parts of it) into a figure.

It was first developed by Michael Eisen in 1998 (Treeview 1.0) and updated a few years later by Alok Saldanha (Treeview 2.0). We have now completely redesigned the app and released a new & improved TreeView 3.0 that is faster, cleaner and easier to use.

If you are interested in providing feedback, please watch this repository, create new issues or contact us at <treeview@princeton.edu>.



DOWNLOAD
========

The Alpha 2 version of TreeView 3.0 can be downloaded from:

<https://bitbucket.org/TreeView3Dev/treeview3/downloads/tv3_alpha02_final.jar>

The Alpha 3 version of Treeview 3.0: coming soon.



INSTALL
=======

## Requirements ##

* Java 7 or higher - go to <http://java.com/en/download/> to find out what version you currently have.
* If you tested an older alpha or beta version of Treeview 3.0, we recommend that you reset your preferences before you upgrade to the latest version (File > Preferences > Reset Preferences). 

## Windows ##

Place the downloaded JAR file anywhere on your computer and run it by double-clicking on the icon.

## Mac ##

Place the downloaded JAR file anywhere on your computer.

**First time**: right-click on the icon and choose `Open with` > `Jar Launcher.app`.

**Every time after that**: double-click on the icon.


WHAT'S NEW
==========

## In Alpha 3 ##

* Updated window layout to optimize the use of screen space
* Improved navigation:
    * whizzing labels and trees
    * double-click to zoom
* Added data export functions:
    * copy labels and/or matrix data to clipboard
    * export matrix view to PDF, SVG, PNG, JPG, PPM
* Improved search
* Improved color selection and editing
* Other minor updates and bug fixes

## In Treeview 3.0 overall ##

* We have completely re-worked the interface to include one main data panel, with zooming, scrolling and searching options.
* We have made clustering an integral part of TreeView (no need for Cluster 3.0).
* We have added more flexible color settings (multiple colors, varying ranges)
* We have eliminated some of the features that we considered obsolete or in need of a major re-work.
* We have added a much improved option for image export.


DOCUMENTATION
=============

[Add link].


DEVELOPERS
==========

* Chris Keil - University of Hamburg
* Robert Leach - Princeton University
* Lance Parsons - Princeton University
* Anastasia Baryshnikova - Princeton University - <http://www.baryshnikova-lab.org>


LICENSE
=======

Treeview 3.0 - visualization and analysis of large data matrices

The MIT License (MIT)

Original work Copyright (c) 2001-2003 Alok Saldanha
Modified work Copyright (c) 2013-2016 Princeton University

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal in 
the Software without restriction, including without limitation the rights to 
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
the Software, and to permit persons to whom the Software is furnished to do so, 
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

DEPENDENCIES
=======

* Java TreeView (https://sourceforge.net/projects/jtreeview/)
* FreeHep (http://java.freehep.org/license.html)
* MigLayout (http://www.miglayout.com/)
* SwingX - Autocompletion (http://www.java2s.com/Code/Jar/s/Downloadswingxcore162AutoCompletitionjar.htm)
* xml-apis/ Xerces (https://xerces.apache.org/xml-commons/)