# README #

This README would normally document whatever steps are necessary to get your application up and running.

## What is this repository for?

* Quick summary
* Version
* [Learn Markdown](https://bitbucket.org/tutorials/markdowndemo)

## How do I get set up?

* Summary of set up
* Configuration
* Dependencies
    1. Java 1.7+
* Database configuration
* How to run tests
* Deployment instructions

## Contribution guidelines

### General Git Flow

Our Git workflow follows the [Github Workflow](https://guides.github.com/introduction/flow/).

### Code Style & Conventions
[Google Java Style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html)

An Eclipse configuration XML file can be found in LinkedView/ 

### Developer Change Procedure

Tackling an issue:

1. Claim the issue
2. Check to make sure no one's working on a similar issue (possibly involving same code)
3. Switch to master branch
4. Sync repository (git pull)
5. Branch (git checkout -b branchname)
6. Implement code
7. JUnit test + user test on your own
8. Run a code clean up if using Eclipse (Project Explorer > Right-click > Source > Clean-up)
The XML-protocol is available for download in the download section of this repository (cleanup_prefs_alpha01.xml). Import this to Eclipse for use.
9. Commit. Push to/ create _remote feature branch_ only for work that should be looked at by others or be backed up (git is not a backup tool though).
10. Code review
    * Have at least 1 other permanent contributor look at the code and point out criticism.
11. **Do not proceed** beyond here if tests fail. -> Fix until all tests are green. Exception: user test issues after team discussion.
---------------------- 

Merging branch of resolved issue with _develop_ branch:

1. Switch to _develop_ branch (git checkout _develop_ -> no errors if all changes have been committed).
2. Sync with remote _develop_ again **before merge**!
3. Merge in your branch (on _develop_: git merge branchname)
4. Resolve possible merge conflicts. ONLY if you are 100% sure no other work of others (fixes, additions, changes) will be overwritten by your code go ahead and decide which version to use. Otherwise bring up the single merge conflicts to other developers! This is to avoid accidental change/ deletion of important code.
5. Push your locally merged _develop_ branch to remote _develop_ once all conflicts have been resolved.
6. Resolve the issue in Bitbucket issue log (can be done automatically via "resolved _issue_num_; [...]" in commit message). 

### JUnit - Writing tests
1. [Small intro to JUnit in Eclipse](https://courses.cs.washington.edu/courses/cse143/11wi/eclipse-tutorial/junit.shtml)
2. [Another good intro](http://www.vogella.com/tutorials/JUnit/article.html) 
3. **No debug code** in _master_ and _develop_ branches (debug log statements etc., see code criteria below).
4. JUnit supports all sorts of unit testing (correctness, timing, etc.).
5. Write a test for **every** class and **all** its methods. Tests ideally need to cover all possible cases. 
6. If a method needs debugging, it should **fail its JUnit test**.
7. GUI/ Swing code: [**Do not test the framework code!**](https://stackoverflow.com/questions/1480843/unit-testing-a-swing-component) Test the logic you implement to update/ change GUI components. E.g.:
    * Do not test whether JScrollBar.setValue(int val) works, but whether your custom scroll code will produce the expected value used as parameter in setValue().
    * Do not test ActionListeners, but **do** test the methods they execute in actionPerformed().
    * If code is to be tested, it is logic. Therefore it belongs in the Controller class, not the View class. -> [MVC pattern](https://programmers.stackexchange.com/questions/127624/what-is-mvc-really)

### Debugging
If it does not fail while being buggy, extend the test case for this method (boundary conditions etc.).
6. debugger with breakpoints. You may write or expand test cases for methods.

### Code criteria to be eligible for merging into master
In general, clean & concise & functional code. The goal is minimum effort required by other devs to understand code and what it is supposed to do.

1. Absolutely no debug code. If you need to retain debugging code beyond *ongoing* development for a specific issue (aka before merge into master) that means a test case for that part of the code should be written instead.
2. Reduce comments to an absolute minimum when the code itself (through flow and naming) is not sufficient to understand what is happening. 
3. BUT maintain documentation for all classes, functions (JavaDoc). Update JavaDoc when necessary.
4. Add author tag to every new function/ class/ interface etc. + JavaDoc.
5. Generally follows the [OOD principles](http://www.oodesign.com/design-principles.html) (e.g. "Single Responsibility Principle").
6. The shared Eclipse clean-up settings (via a downloadable XML-file loaded into Eclipse) has been run. This forces some code formatting & style to maintain continuity.
7. While there cannot be a hard limit for line count in methods/ classes, code should be organized in classes and methods so that responsibilities and knowledge are distributed as much as possible. To help others read and understand code, such organization is critical.

### User Testing Procedure

User testing is a part of the Developer Change Procedure above, but in addition, every 2 weeks, testing is performed by the Project Leader to test all recent change activity.  The following is the procedure for generating the project snapshot:

Once every 2 weeks:

1. Switch to master branch
2. Pull & Sync repository
3. Export a jar file
4. Upload jar file to the downloads page named with the short build number
5. Notify project leader

### Who do I talk to?

* Repo owner or admin
* Other community or team contact