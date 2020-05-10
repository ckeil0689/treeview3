# CONTRIBUTING #

This document is a summary of how to get involved in contributing to the TreeView 3 project.

## Dependencies

1. Java 1.7+

## Contribution guidelines

### General Workflow

Our workflow follows a modified version of the [Github Workflow](https://guides.github.com/introduction/flow/).
We've added release branches to make it easier to cherry-pick merge trivial
bugfixes from the cutting-edge master branch.

### Commit Messages
Short one liner as header + 1 blank line + body to explain details.
Reference: http://chris.beams.io/posts/git-commit/

### Code Style & Conventions
[Google Java Style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html)

Eclipse configuration XML files can be found here:

LinkedView/doc/softeng/eclipse_java_codestyle_preferences

To install the configuration files:

1. Open the Eclipse Preferences and expand the categories in the left pane to:
   Java->Code Style
2. Click "Clean up" in the left pane
3. Click the "Import..." button & select the
   princeton_bioinf_eclipse_cleanup_prifile.xml file
4. Click "Code Template" in the left pane
5. Click the "Import..." button & select the
   princeton_bioinf_eclipse_codetemplate_prifile.xml file
6. Click "Formatter" in the left pane
7. Click the "Import..." button & select the
   princeton_bioinf_eclipse_formatter_prifile.xml file

### Developer Change Procedure

For all change pocedures, please refer to section 3.2 (CONFIGURATION CONTROL) of
the Software Configuration Management Plan in the plan document located here:

doc/softeng/SCMP_TreeView3.doc

### JUnit - Writing tests
1. [Small intro to JUnit in Eclipse](https://courses.cs.washington.edu/courses/cse143/11wi/eclipse-tutorial/junit.shtml)
2. [Another good intro](http://www.vogella.com/tutorials/JUnit/article.html) 
3. JUnit supports all sorts of unit testing (correctness, timing, etc.).
4. JUnit testing for all classes and methods is highly encouraged.  Pull
   requests without JUnit tests may be rejected.

### Recommended Coding Practices
In general, clean & concise & functional code. The goal is minimum effort
required by other devs to understand code and what it is supposed to do.

1. Debug code that can be set to a specific feature type is encouraged, but must
   be able to be turned off easily.  When a problem is encountered, selecting a
   debug mode appropriate to the issue can be used to figure out a problem
   quickly.  Eventually a user-level debug mode may utilize the debug code so
   that users can debug issues themselves, or the debug output could be pasted
   in an email to help developers diagnose a problem.  A user-accessible debug
   mode however will only remain until a stable non-beta has been released, at
   which point much of the debug code may be pulled out.
2. Comments are highly encouraged and represent a significant portion of well-
   written code (http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=6613836),
   but the selection of method and class names which are self-explanatory are a
   preferred method of conveying function and intent.  Anywhere in the code
   where a simple function name is insufficient to capture concisely what the
   code underneath is doing and why it is doing it should be well commented.
3. Documentation for all classes, functions are to be done with JavaDoc.
4. Follow the [OOD principles](http://www.oodesign.com/design-principles.html) (e.g. "Single Responsibility Principle").
6. Run the Eclipse clean-up settings (see Code Style & Conventions) before
   merging.
7. While there cannot be a hard limit for line count in methods/ classes, code
   should be organized in classes and methods so that responsibilities and
   knowledge are distributed as much as possible. To help others read and
   understand code, such organization is critical.

### Testing

Testing is a part of the Developer Change Procedure (section 3.2 (CONFIGURATION
CONTROL) of the SCMP), but in addition, the following is the procedure for
generating a project snapshot:

1. Switch to master branch
2. Pull & Sync repository
3. Export a jar file
4. Upload jar file to the downloads page named with the short build number
5. Notify project testers

### Building / Releasing
To share builds between developers for testing, we use gradle.  A simple jar file build is sufficient for testing.  To create a jar, simply run:

- `gradle`

To release a build, a package must be created for 4 systems and the package must successfully install on the target system:

#### Mac
- `gradle createDmg`
- Double-click to mount & drag the app to /Applications
- Run by double-clicking the app (confirm icon)

#### Windows
- `gradle msi`
- Double-click to mount & follow instructions
- Run via start menu entry (confirm icon)

#### Debian (can also be built in RedHat)
- `gradle buildDeb`
- `sudo dpkg -i *.deb`
- `treeview3`
- Also run via start menu entry (confirm icon)

#### RedHat (can also be built in Debian)
- `gradle buildRpm`
- `sudo dpkg -i *.rpm`
- `treeview3`
- Also run via start menu entry (confirm icon)

### Who do I talk to?

* Repo owner or admin
* Other community or team contact