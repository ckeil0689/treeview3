#!/usr/bin/perl
use strict;
#export XSL=/usr/local/share/xml/xsl/docbook-xsl
#export FOP=/usr/local/share/xml/fop
unless (-d "html/JTVUserManual") {
	execute("");
	execute("cd JTVUserManual;xsltproc -o single.html \$XSL/html/docbook.xsl JTVUserManual.xml");
	
	execute("cd JTVUserManual;xsltproc \$XSL/html/chunk.xsl JTVUserManual.xml ");
	
	execute("cd JTVUserManual;xsltproc -o JTVUserManual.fo \$XSL/fo/docbook.xsl JTVUserManual.xml ");
	execute("cd JTVUserManual; \$FOP/fop -fo JTVUserManual.fo -pdf JTVUserManual.pdf");
	execute("mkdir html");
	execute("mkdir html/JTVUserManual");
	execute("mv JTVUserManual/JTVUserManual.pdf html/JTVUserManual");
	execute("mv JTVUserManual/*.html html/JTVUserManual");
	execute("cp -r JTVUserManual/figures html/JTVUserManual");
}

unless (-d "html/JTVProgrammerGuide") {
	execute("");
	execute("cd JTVProgrammerGuide;xsltproc -o single.html \$XSL/html/docbook.xsl JTVProgrammerGuide.xml");
	
	execute("cd JTVProgrammerGuide;xsltproc \$XSL/html/chunk.xsl JTVProgrammerGuide.xml ");
	
	execute("cd JTVProgrammerGuide;xsltproc -o JTVProgrammerGuide.fo \$XSL/fo/docbook.xsl JTVProgrammerGuide.xml ");
	execute("cd JTVProgrammerGuide;\$FOP/fop -fo JTVProgrammerGuide.fo -pdf JTVProgrammerGuide.pdf");
	execute("mkdir html");
	execute("mkdir html/JTVProgrammerGuide");
	execute("mv JTVProgrammerGuide/JTVProgrammerGuide.pdf html/JTVProgrammerGuide");
	execute("mv JTVProgrammerGuide/*.html html/JTVProgrammerGuide");
	execute("cp -r JTVProgrammerGuide/figures html/JTVProgrammerGuide");
}
sub execute {
	my $cmd = shift();
	print STDERR "$cmd\n";
	system($cmd);
}
