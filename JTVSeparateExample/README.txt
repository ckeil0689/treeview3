The purpose of the example application is to assist people who are trying
 to link up Java Treeview style windows with their own applications. In 
 this particular example, we assume that the other application has a 
 notion of genes, but not the microarray experiments, and also no notion 
 of one dimensional gene ordering.

The easiest way to hook up such an application is as follows 
(from Melissa Cline):

1. Prior to running their app, the users run Cluster (or something 
   similar) on their expression data, generating GTR and CDT files.
2. The user runs their app, with appropriate network data.
3. When the user wants to view expression data, a file selection box 
   appears to let the user select CDT files, in the same way that the 
   user operates TreeView now.  So, there will be the app running in one 
   window, TreeView running in another.
4. Integration is achieved by selection events in treeview being captured 
   by the app, and conversely selection events in the app being forwarded
   to treeview.

This application models this by printing out selection events to a 
JTextArea, and also allowing the user to enter a set of gene names into 
a second JTextArea and asking that they be selected.
