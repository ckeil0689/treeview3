#!/usr/bin/perl
# * $Author: alokito $
# * $RCSfile: splitFasta.pl,v $
# * $Revision: 1.3 $
# * $Date: 2005-07-19 17:24:08 $
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



# splits fasta file into seq files
BEGIN {
	push(@INC, $ENV{'HOME'}.'/perl/lib');
}
use strict;
use vars qw($opt_l $opt_o $opt_f);
use Getopt::Std;
use ListManipulation;

sub usage() {
	print STDERR qq!Usage:
	
	$0 [-l <listfile>] [-o <outputfile>] [-f fasta|bare] blah.fasta
	
	by default, splits fasta file into seq files
	
	Options:
	
	-l only split out ids from list
	-o output to single file instead of splitting into several
	-f use fasta/bare format (defaults to bare sequence)
!;
	
	
}
getopts('l:o:f:'); # column number of pcl,cdt files to join on.

unless (@ARGV) {
	usage();
	exit(1);
}

my (%keep, @keep);
if ($opt_l) {
	my $list = ListManipulation::loadFromTdt($opt_l, 0);
	%keep = %{ListManipulation::counts($list)};
	@keep = @{$list};
}
my @missing = @keep;
my $format = $opt_f or 'bare';
if (($format ne 'bare') and ($format ne 'fasta')) {
	print STDERR "unrecognized output format $format\n";
}
if ($opt_o) {
	open (OUT, ">$opt_o");
}
my $i;
my @seen;
my $id = <>;
chomp($id);
while ($id =~ s/^\>//) {
	print STDERR '.';
	$i++;
	if ($i % 100 == 0) {
		print STDERR "$i\n";
	} elsif ($i % 10 == 0) {
		print STDERR " ";
	}
	my $seq = <>;
	while (( $_ = <>) !~ /^\>/) {
		chomp();
		last unless ($_);
		$seq .= $_;
	}
	chomp();
	my $next  = $_;
	# process current...
	print STDERR "Processing $id\n";
	my $sought = 1;
	if ($opt_l) {
		if (grep {$id =~ /$_/} @missing) {
			@missing = removeEl($id, @missing);
			push (@seen, $id);
		} else {
			$sought = 0;
		}
	} 
	
	
	if ($sought) {
		unless ($opt_o) {
			open (OUT, ">$id.fasta") || die " could not open file:$!";
		}
		
		if ($format eq 'fasta') {
			print OUT ">$id\n";
		}
		print OUT $seq, "\n";
		unless ($opt_o) {
			close(OUT);
		}
	}
	$id = $next;
}

if ($opt_o) {
	close (OUT);
}

if ($opt_l) {
	# check to see if we missed any...
	for (@missing) {
		print STDERR "Missing $_\n";
	}
}

sub removeEl {
	my $el = shift();
	my @in = @_;
	my @out;
	for (@in) {
		if ($el =~ /$_/) {
			next;
		} else {
			push (@out, $_);
		}
	}
	return @out;
}
