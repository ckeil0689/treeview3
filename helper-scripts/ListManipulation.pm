package ListManipulation;
srand;


# * $Author: alokito $
# * $RCSfile: ListManipulation.pm,v $
# * $Revision: 1.3 $
# * $Date: 2005-07-19 17:27:58 $
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



######################
use strict;###########

################################################################################
################################################################################

#   MODULE NAME: ListManipulation

#  Non-object oriented perl module - a bunch of routines for list manupulation.


#     Author: Alok Saldanha
#     Date:   August 11, 2003


#  Routines:

#  *** In the following examples, 
#        $listref means a reference to a list of things
#        $subref means a reference to a subroutine


#  ListManipulation::intersection($listref1, $listref2, ... $listrefn)
#     finds intersection of lists, using "eq"

#  ListManipulation::union($listref1, $listref2, ... $listrefn)
#     finds union of lists, using "eq"

#  ListManipulation::loadFromFile($filename);
#     loads genelist from file

#  ListManipulation::loadFromFh($fh);
#     loads genelist from fh, use *STDIN to avoid package issues.

sub intersection {
	my @lists = @_;
	my %counts;
	my $n;
	for (@lists) {
		$n++;
		my @members = keys %{counts($_)}; # get members, need to worry about multiple occurance per list.
#		print STDERR "list ", @list;
		for (@members) {
			$counts{$_}++;
		}
	}
#	print STDERR "n is $n\n, counts ", %counts, "\n";
	my @common;
	for (keys %counts) {
		if ($counts{$_} == $n) {
			push(@common, $_);
		}
	}
	return \@common;
}

sub union {
	my @lists = @_;
	my $counts = counts(@lists);
	my @present = keys %{$counts};
	return \@present;
}


sub counts {
	my @lists = @_;
	my %counts;
	my $n;
	for (@lists) {
		$n++;
		my @list = @{$_};
#		print STDERR "list ", @list;
		for (@list) {
			$counts{$_}++;
		}
	}
	return \%counts;
}

sub size {
	my @lists = @_;
	my @sizes;
	for (@lists) {
		push(@sizes, $#{$_} + 1);
	}
	return @sizes;
}


sub loadFromFile {
	my $file = shift();
	open (FILE, $file) || die "ListManipulation::loadFromFile() could not open $file: $!";
	my $list = loadFromFh(*FILE);
	close(FILE);
	return $list;
}

sub loadFromFh {
	my $fh = shift();
	my @list = <$fh>;
	chomp(@list);
	return \@list;
}

sub loadFromTdt {
	my $raw = loadFromFile(shift());
	my $index = shift();
	my @out;
	for (@{$raw}) {
		my @els = split("\t", $_);
		push(@out, $els[$index]);
	}
	return \@out;
}

## The following three very well-annotated functions were shamelessly taken from GO::TermFinder by Gavin.
############################################################################
sub __hypergeometric{
############################################################################
# This method returns the hypergeometric probability value for
# sampling without replacement.  The calculation is the probability of
# picking x positives from a sample of n, given that there are M
# positives in a population of N.
#
# The value is calculated as:
#
#       (M choose x) (N-M choose n-x)
# P =   -----------------------------
#               N choose n
#
# where generically n choose r is number of permutations by which r
# things can be chosen from a population of n (see __logNCr())
#
# However, given that these n choose r values may be extremely high (as they are
# are calculated using factorials) it is safer to do this instead in log space,
# as we are far less likely to have an overflow.
#
# thus :
#
# log(P) = log(M choose x) + log(N-M choose n-x) - log (N choose n);
#
# this means we can now calculate log(n choose R) for our
# hypergeometric calculation (see below).
#
    my ($x, $n, $M, $N) = @_;

    return exp(__logNCr($M, $x) + __logNCr($N - $M, $n-$x) - __logNCr($N, $n));

}

############################################################################
sub __logNCr{
############################################################################
# This method returns the log of n choose R.  This means that it can do the
# calculation in log space itself.
#
#
#           n!
# nCr =  ---------
#        r! (n-r)!
#
# which means:
#
#
#
# log(nCr) = log(n!) - (log(r!) + log((n-r)!))
#

    my ($n, $r) = @_;
		return  __logFact($n) - (__logFact($r) + 	__logFact($n - $r));
}

my @factCache; # array of values...
############################################################################
sub __logFact{
############################################################################
# Since :
#
#     n!  = n * (n-1) * (n-2) ... * 1
#
# Then :
#
# log(n!) = log(n * (n-1) * (n-2) ... * 1)
#
#         = log(n) + log(n-1) + log(n-2) ... + log(1)
#
#         = log(n) + log(n-1!)

  my $n = shift();
	unless ($n <= $#factCache) {
		if ($n == 0) {
			$factCache[0] = 0; #0! = 1, log(0!) = log(1) = 0.
		} else {
			$factCache[$n] = log($n) + __logFact($n  - 1);
		}
	}
	return $factCache[$n];
}

sub subtraction {
    my $first = shift();
    my %harem;
    for (@_) {
        my $list = $_;
        for (@{$list}) {
            $harem{$_}++;
        }
    }
    my @out;
    for (@{$first}) {
        unless ($harem{$_}) {
            push (@out, $_);
        }
    }
    return \@out;
}

1;
