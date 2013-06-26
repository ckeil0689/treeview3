#!/usr/bin/perl
use strict;

my $version = $ARGV[0];
unless ($version) {
	print "Usage:
	$0 <version_string>

	Remember to increment version number and 
	tag package first.
";
	exit(1);
}

my $buildVersion = getBuildXmlVersion();
my $javaVersion = getTreeViewAppVersion();

unless (($buildVersion eq $javaVersion) and ($buildVersion eq $version)) {
    print STDERR "Error: Version Mismatch\n";
    print STDERR "Specified Version $version\n";
    print STDERR "build.xml Version $buildVersion\n";
    print STDERR "Java Treeview Version $javaVersion\n";
    exit(1);
}

my $bin = "TreeView-" .$version . "-bin";
my $win = "TreeView-" .$version . "-win";
my $osx = "TreeView-" .$version . "-osx";
my $src = "TreeView-" .$version . "-src";
my $applet ="TreeView-" .$version . "-applet";
my $doc =  "TreeView-".$version . "-javadoc";
my $winInst = 'windows-installer.tar.gz';

unless (-d $src) { #src version
	execute("git clone file://.. jtreeview");
	execute("mv jtreeview/LinkedView $src");
	execute("rm -rf jtreeview");
	execute("tar cvf $src.tar $src");
	execute("gzip $src.tar");
}

unless (-d $doc) { # javadoc
    execute("javadoc -private -classpath lib/nanoxml-2.2.2.jar -d javadoc `find src | grep java`");
    execute("mv javadoc $doc");
    execute("zip -r $doc.zip $doc");
}

unless (-d $bin) { # .tar.gz version...
	execute("ant dist");
	execute("mv dist $bin");
	# tar.gz
	execute("tar cvf $bin.tar $bin");
	execute("gzip $bin.tar");
}
unless (-d $win) { # .zip version...
	execute("ant dist");
	execute("mv dist $win");
	execute("tar xvzf $winInst");
	if (-f 'setup.exe') { # did it untar to this directory?
	    execute("mv setup.exe windows $win");
	} elsif (-f 'windows/setup.exe') { # or a windows subdirectory?
	    execute("mv windows/setup.exe windows/windows $win");
	} else {
	    die ' could not find file "setup.exe" in windows installer.'; 
	}
	execute("zip -r $win.zip $win");
}

unless (-d $osx) { # osx version
	execute("ant bundle");
	execute("mv bundle $osx");
	#command line disk image?
	execute("zip -r $osx.zip $osx");
}


unless (-d $applet) { #applet version
    execute("ant applet");
	execute("mv applet $applet");
	execute("tar cvf $applet.tar $applet");
	execute("gzip $applet.tar");
}    

sub getBuildXmlVersion { #probably should parse XML, but I'm lazy.
    open (XML, "build.xml") || die "could not open build.xml : $!";
    while (<XML>) {
        if (/version=\"(\d+\.\d+\.\d+.*)\"/) {
            close(XML);
            return $1;
        }
    }
    close(XML);
    return "Could not determine version";
}
sub getTreeViewAppVersion {
    open (TV, "src/edu/stanford/genetics/treeview/TreeViewApp.java");
    while (<TV>) {
        if (/versionTag\s+=\s+\"(\d+\.\d+\.\d+.*)\"/) {
            close (TV);
            return $1;
        }
    }
    close(TV);
    return "Could not determine version";
}


sub execute {
	my $cmd = shift();
	print STDERR "$cmd\n";
	system($cmd);
}
