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
* Code review (**BEFORE** Merge)
    1. Have at least 1 other permanent contributor look at the code and point out criticism.
    2. Upload a test JAR/ Executable for at least 1 other contributor to thoroughly test for issues.
    3. Only merge into production branch if code / version have been reviewed and tested.

* Other guidelines
    1. Oracle Code Conventions: http://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html
    2. Google Java Style: https://google-styleguide.googlecode.com/svn/trunk/javaguide.html
    3. More extensive style guide: http://www.scribd.com/doc/15884743/Java-Coding-Style-by-Achut-Reddy

### Developer Change Procedure ###

When you tackle an issue:

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
14. Resolve the issue.

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