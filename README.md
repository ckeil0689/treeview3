# README #

This README would normally document whatever steps are necessary to get your application up and running.

### What is this repository for? ###

* Quick summary
* Version
* [Learn Markdown](https://bitbucket.org/tutorials/markdowndemo)

### How do I get set up? ###

* Summary of set up
* Configuration
* Dependencies
    1. Java 1.7+
* Database configuration
* How to run tests
* Deployment instructions

### Contribution guidelines ###

* Writing tests
    1. master and develop branches may never include "debugging code" (debug log statements etc., see code criteria below).
    2. If a method needs to be debugged, it should be done via JUnit tests + debugger with breakpoints. You may write or expand test cases for methods.
    3. Small intro to JUnit in Eclipse -> https://courses.cs.washington.edu/courses/cse143/11wi/eclipse-tutorial/junit.shtml

* Code review (**BEFORE** Merge)
    1. Have at least 1 other permanent contributor look at the code and point out criticism.
    2. Upload a test JAR/ Executable for at least 1 other contributor to thoroughly test for issues.
    3. Only merge into production branch if code / version have been reviewed and tested.

* Other guidelines
    1. Oracle Code Conventions: http://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html
    2. Google Java Style: https://google-styleguide.googlecode.com/svn/trunk/javaguide.html
    3. More extensive style guide: http://www.scribd.com/doc/15884743/Java-Coding-Style-by-Achut-Reddy

### Developer Change Procedure ###

Generally tackling an issue:

1. Claim the issue
2. Check to make sure no one's working on a similar issue (possibly involving same code)
3. Switch to master branch
4. Sync repository (git pull)
5. Branch (git checkout -b branchname)
6. Implement code
7. Test on your own
8. Run a code clean up if using Eclipse (Project Explorer > Right-click > Source > Clean-up)
The XML-protocol is available for download in the download section of this repository (cleanup_prefs_alpha01.xml). Import this to Eclipse for use.
9. Commit. Push to/ create remote branch only for work that should be looked at by others or be backed up (git is not a backup tool though).
10. Ask at least 1 other contributor to test
11. Switch to master branch (git checkout master -> no errors if all changes have been committed).
12. Sync with remote master again **before merge**!
12. Merge in your branch (on master: git merge branchname)
13. Resolve possible merge conflicts. ONLY if you are 100% sure no other work of others (fixes, additions, changes) will be overwritten by your code go ahead and decide which version to use. Otherwise bring up the single merge conflicts to other developers! This is to avoid accidental change/ deletion of important code.
13. Push the merged master branch to remote once all conflicts have been resolved.
14. Resolve the issue..

### Code criteria to be eligible for merging into master ###
In general, clean & concise & functional code. The goal is minimum effort required by other devs to understand code and what it is supposed to do.

1. Absolutely no debug code. If you need to retain debugging code beyond *ongoing* development for a specific issue (aka before merge into master) that means a test case for that part of the code should be written instead.
2. Reduce comments to an absolute minimum when the code itself (through flow and naming) is not sufficient to understand what is happening. 
3. BUT maintain documentation for all classes, functions (JavaDoc). Update JavaDoc when necessary.
4. Add author tag to every new function/ class/ interface etc. + JavaDoc.
5. Generally follows the [OOD principles](http://www.oodesign.com/design-principles.html) (e.g. "Single Responsibility Principle").
6. The shared Eclipse clean-up settings (via a downloadable XML-file loaded into Eclipse) has been run. This forces some code formatting & style to maintain continuity.
7. While there cannot be a hard limit for line count in methods/ classes, code should be organized in classes and methods so that responsibilities and knowledge are distributed as much as possible. To help others read and understand code, such organization is critical.

### General Git flow ### 

[Reference 1](http://nvie.com/posts/a-successful-git-branching-model/)

[Reference 2 (pics)](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)

Specific branches have specific roles.

1. Two branches are used to record the history of the project:
     * master: holds history of project releases. Incremented by release versions. Commits tagged with version number.
     * develop: integration history for features. Incremented by feature additions.
     ![Selection_020.png](https://bitbucket.org/repo/AXqk7r/images/3307300262-Selection_020.png)
     * develop used to be master (as of Aug 2015).

2. Feature branches are branched off and merged into develop.
     * No direct interaction with master!
    ![Selection_021.png](https://bitbucket.org/repo/AXqk7r/images/863398893-Selection_021.png)

3. Release branch
     * Branched off from develop.
     * Branch once release is feature ready.
     * naming: release-***
     * Does **only** include: documentation, bug fixes, release-oriented stuff
     * No feature work here!
     * When ready to ship: merge into master -> tag branch & commit with version number!
     * Also, after release and potential bug fixes etc., merge back into develop to retain important changes.

4. Maintenance
     * For hotfixes.
     * Only branch that may directly branch of master (since fixed master should be released right away).
     * Immediately merge back into master once the hotfix is done.

### Developer Testing Procedure ###

Testing is a part of the Developer Change Procedure above, but in addition, every 2 weeks, testing is performed by the Project Leader to test all recent change activity.  The following is the procedure for generating the project snapshot:

Once every 2 weeks:

1. Switch to master branch
2. Pull & Sync repository
3. Export a jar file
4. Upload jar file to the downloads page named with the short build number
5. Notify project leader

### Who do I talk to? ###

* Repo owner or admin
* Other community or team contact