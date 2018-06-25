# How to distribute a macOS app outside the app store

Note, the current code signing certificate, created using the procedure described in this document was generated on 6/23/2018 and expires on 6/23/2023, at which time a new certificate will have to be generated.

## Purpose: Eliminate the "Unidentified Developer" error when users try to run our app downloaded from BitBucket

## Requirements

- gradle
- You're at an educational Institution that holds (a single/free) Apple Developer Program Account
- A Mac computer
- An Apple ID

## Scope

This procedure covers everything from beginning to end.  It does not go into the details of setting up an Apple ID or an Apple Developer ID, but mentions where to go to set up the latter.

1. Install xcode: In Terminal.app, enter: `xcode-select --install`
2. Obtain an Apple Developer ID from here: https://developer.apple.com/register/index.action
3. Determine who on your university campus controls the University Apple Developer Program account, supply them with your apple(/developer) ID, and request for them to invite you to the team (as a "member" - lowest privilege access)

4. Generate a "Mac Development" certificate, which we will use for testing the gradle script.  Note, this will not work out in the "wild".  It will only allow you to identify gradle config errors, if you have any.

4.1. Log into your Apple Developer account (https://developer.apple.com/account/) and do the following:
4.1.1. Click "Certificates, Identifiers & Profiles"
4.1.2. Select "macOS" from the drop-down menu at the top left
4.1.3. Under "Certificates", click "All"
4.1.4. Click the "+" at the top right
4.1.5. Click the "Mac Development" radio button (as it should be the only choice and will give us a certificate for testing purposes)
4.1.6. Click continue (and leave this window open)

4.2. In the Keychain Access app:
4.2.1. Select Keychain Access > Certificate Assistant > Request a Certificate from a Certificate Authority
4.2.2. Enter your: university email, full name, & saved to disk (leaving "ca email" empty)
4.2.3. Click continue
4.2.4. Save the CSR file in a memorable place
4.2.5. Click done

4.3. Submit your CSR to Apple.  Return to your account window we left open in step 4.1.6 and do the following:
4.3.1. Click continue on the website
4.3.2. Upload the CSR file and click continue on the following page
4.3.3. Click download to get the certificate and then click done.

4.4. Save the cert (.cer file) from the downloads window in a memorable place
4.5. Double-click the certificate (which opens in keychain access)
4.6. In the Keychain Access window that opens, select the login keychain & click add
4.7. Click the "Keys" or "Certificates" category to find the cert, which should be named something like "Mac Developer: <your name> (cert-ID-string)"
4.8. Click the triangle next to "<your name>" and copy the cert ID string/user ID

5. Edit your build.gradle script to add the cert string and codesign dependency:
5.1. Add 'certIdentity = "<cert ID string>"' to the macAppBundle block, where <cert ID string> is the string you copied on step 4.8.
5.2. Add 'createDmg.dependsOn(codeSign)' underneath/outside the macAppBundle block (so that `gradle createDmg` will execute the code-sign)

6. Test your certificate with a new gradle build
6.1. cd into your project directory
6.2. Execute `gradle createDmg`

At this point, if everything worked (i.e. the build succeeds and there's a message in the verbose output that says ":codeSign" without an error following it) and you're ready to release your app, you must obtain a "Developer ID Application" certificate, install it, and replace the cert string with that of the new certificate.  This is specifically for distributing your app outside of the mac app store *and* if you're not creating an installer, but rather a .app inside a dmg.  Here's how you do that:

7. Request from the team agent to create a certificate of type "Developer ID Application", which entails:
7.1. Log into your Apple Developer "agent" account (https://developer.apple.com/account/) to create a "Developer ID Application" certificate by doing the following:
7.1.1. Click "Certificates, Identifiers & Profiles"
7.1.2. Select "macOS" from the drop-down menu at the top left
7.1.3. Under "Certificates", click "All"
7.1.4. Click the "+" at the top right
7.1.5. Click the "Developer ID Application" radio button
7.1.6. Click continue (and leave this window open, to return to it on step 7.3)

7.2. In the Keychain Access app:
7.2.1. Select Keychain Access > Certificate Assistant > Request a Certificate from a Certificate Authority
7.2.2. Enter your: university email, full name, & saved to disk (leaving "ca email" empty)
7.2.3. Click continue
7.2.4. Save the CSR file in a memorable place
7.2.5. Click done

7.3. Go back to your apple developer account where you left off on step 7.1.6
7.3.1. Click continue on the website
7.3.2. Upload the CSR file and click continue on the following page
7.3.3. Click download to get the certificate and then click done.

7.4. Put the cert (.cer file) in a memorable place
7.5. Double-click the certificate (which opens in keychain access)
7.6. Select the login keychain & click add
7.7. In the Keychain Access window that opens up, click the "Certificates" category to find the cert, which should be named something like "Developer ID Application: <team name> (cert-ID-string)"
7.8. Expand the certificate by clicking the gray triangle next to it and highlight both the "Developer ID Application..." row and the next row, which is the "team agent name" private key
7.9. While both rows are highlighted, right click and select "Export 2 items" in order to share
7.10. You will be saving a p12 file to your machine, and you will need to create a password for it in order to share the file (you will need to share the password as well), so note it down
7.11. Email both the P12 file and the password to your team member (does not have to be an admin or agent - just "team member" - see https://developer.apple.com/account/#/people/)

8. Check your email to find the attached .p12 file and associated passphrase
9. Save the cert/key (.p12 file) in a memorable place
10. Double-click the p12 file (which opens in keychain access)
11. Select the login keychain & click add
12. Click the "Certificates" category to find the cert
13. Click the triangle next to the cert and copy the cert ID string/user ID

14. Edit your build.gradle script:
14.1. Change the certIdentity from step 20.1 to the copied cert string/user ID from step 29

15. Open Terminal.app and...
15.1. cd into your project directory where your build.gradle file is
15.2. Execute `gradle createDmg`

16. Test your code-signed app

17.1. Make sure the security & Privacy settings in your System Preferences are set to allow apps installed from known developers
17.1.1. Open system preferences
17.1.2. Click "Security & Privacy"
17.1.3. Click the lock in the lower left corner (if locked) and enter your system password
17.1.4. Under "Allow apps downloaded from", select "App Store and Identified Developers"
17.1.5. Click the lock and close the window

17.2. Try to generate the "Unidentified developer" error
17.2.1. Upload your dmg to bitbucket
17.2.2. Re-downloading the dmg
17.2.3. Double-click the dmg
17.2.4. Drag the app to /Applications
17.2.5. Double-click the app
17.2.6. If the app opens without an unidentified developer error, it worked!  Note, you will still get a warning that the app was downloaded from the internet, but will be prompted with an open button



## References

https://devreboot.wordpress.com/2014/07/04/distributing-an-app-on-mac-os-x-without-xcode-outside-the-mac-app-store/
https://github.com/crotwell/gradle-macappbundle/issues/6
https://stackoverflow.com/questions/29039462/which-certificate-should-i-use-to-sign-my-mac-os-x-application/29040068
https://www.ironpaper.com/webintel/articles/how-to-share-an-ios-distribution-certificate/
https://developer.apple.com/support/roles/
https://help.apple.com/developer-account/#/dev04fd06d56
