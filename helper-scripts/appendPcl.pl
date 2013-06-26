#!/usr/bin/perl

# * $Author: alokito $
# * $RCSfile: appendPcl.pl,v $
# * $Revision: 1.3 $
# * $Date: 2005-07-19 17:25:15 $
# * $Name:  $
# *
# * This file is part of Java TreeView
# * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved.
# *
# * This software is provided under the GNU GPL Version 2. In particular,
# *
# * 1) If you modify a source file, make a comment in it containing your name and the date.
# * 2) If you distribute a modified version, you must do it under the GPL 2.
# * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alokito@users.sourceforge.net when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
# *
# * A full copy of the license can be found in gpl.txt or online at
# * http://www.gnu.org/licenses/gpl.txt


BEGIN {
    push(@INC, $ENV{'HOME'}.'/perl/lib');
}
use strict;
use vars qw($opt_c $opt_p);
use Getopt::Std;
use PCL_Analysis;

if ($#ARGV == -1) {
	print qq!Append pcl data onto an existing cdt file

Usage: $0 -c 1 -p 0 blah.cdt blah.pcl > blah2.cdt

full:	$0 [-c <cdt_col_index>] [-p <pcl_col_index>] <cdt_file> <pcl_file>
	
	-c: use indicated column of cdt file instead of second column
	-p: use indicated column of pcl file instead of first column
	
All options are using zero indexed columns.
	
adds the data in the pcl file to the cdt file, using the
second column of the cdt file, and the first column of
the pcl file to figure out what rows correspond.

!;
exit(0);
}


getopts('c:p:'); # column number of pcl,cdt files to join on.

my $pclFile = shift();
my $otherFile = shift();

my $pcl = PCL_Analysis::newFromPCLfile($pclFile);
my $other = PCL_Analysis::newFromPCLfile($otherFile);
	my $EWEIGHTs = $pcl->getEWEIGHTs();
print STDERR "cdt has eweights ", join(", ", @{$EWEIGHTs}), "\n";
$opt_p = 0 unless ($opt_p =~ /\d/);
$opt_c = 1 unless ($opt_c =~ /\d/);

$pcl->AppendDataset($other, $opt_c, $opt_p);
my	$ExptNames = $pcl->getExperimentNames();
	print STDERR "current experiments " , join (", ", @ {$ExptNames}), "\n";


$pcl->PrintDataset(*STDOUT);
