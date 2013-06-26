#!/usr/bin/perl

# * $Author: alokito $
# * $RCSfile: aln2cdt.pl,v $
# * $Revision: 1.2 $
# * $Date: 2004-04-06 04:38:58 $
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
    push(@INC, $ENV{'HOME'}.'/perl');
}

use strict;
use vars qw($opt_f);
use Getopt::Std;
use ListManipulation;
use Bio::TreeIO;

sub usage {
	print qq!Convert blah.aln and blah.dnd into blah.cdt and blah.gtr

Usage: $0 blah.aln

full:	$0 [-f <fasta file>] <aln file> [<missing list files>]* [-p <present list files>+]
	
	Where 
		-fasta file is used for annotations only
		-missing list file makes a column with M if the gene is not present in the file
		-present list file makes a column with P if the gene is present in the list file.

!
}
getopts('f:'); # fasta file for annotations...
my $opt_o = 1; # always score orthology.



unless ($#ARGV >= 0) {
	usage();
	exit(0);
}


my $file = shift(@ARGV);

my @listFiles;
my @presFiles;
{ #construct missing/present lists.
	my $flag = 0;
	for (@ARGV) {
		if ($_ eq '-p') {
			$flag = 1;
			next;
		}
		if ($flag) {
			# after flag, we do present.
			push(@presFiles, $_);
		} else {
			# before flag, we do missing.
			push(@listFiles, $_);
		}
	}
}

#in
my $aln = getRoot($file) . '.aln';
my $dnd = getRoot($file) . '.dnd';

#out
my $gtr = getRoot($file) . '.gtr';
my $cdt = getRoot($file) . '.cdt';

unless ($opt_f) {
	my $cand = getRoot($file) . '.fasta';
	if (-f $cand) {
		$opt_f = $cand;
	}
}
if ($opt_f) {
	print STDERR "getting annotations from FASTA file $opt_f\n";
} else {
	print STDERR "No fasta file provided for annotations.\n";
}
unless ((-f $aln) && (-f $dnd)) {
		die "must have aln and dnd file to make gtr/cdt files\n";
}

my ($idRef, $seqRef) = loadAln($aln);
my @id = @{$idRef};
my %seq = %{$seqRef};

my %anno; # holds annotations...
if ($opt_f) {
	open (FASTA, $opt_f);
	while (<FASTA>) {
		if (/^\>([^\s]+)\s+(.+)/) {
			my $id = uc($1);
			my $anno = $2;
			if ($seq{$id}) {
				$anno{$id} = $anno;
			}
		}
	}
	close(FASTA);
}
my @classes= qw(Pair Expanded Paralog Orphan);
my @classCounts;
if ($opt_o) {
	my $treeio = new Bio::TreeIO('-format' => 'newick',
	'-file'   => $dnd);
	my $tree = $treeio->next_tree();
	my $rel = scoreTree($tree);
	for (0 .. $#classes) {
		my $ind = $_;
		print STDERR "loading $classes[$ind]\n";
		my @members; # must find all members of this class
		for (@{$rel->{$classes[$ind]}}) { # for each internal node in class
			my @cand; #expanded list of all candidate descendants
			if ($classes[$ind] eq 'Orphan') {
				# only homogeneous descendants need apply.
				@cand = grep {isHomogeneous($_)} $_->each_Descendent();
				push (@cand, map {$_->get_all_Descendents()} @cand);
			} else {
				@cand = $_->get_all_Descendents();
			}
			if ($#cand == -1) {
				print STDERR "got no cands for some $classes[$ind] node!\n";
			}
			push (@members, map {$_->id()} grep {$_->is_Leaf()} @cand);
		}
		# stupid cleanup..
		@members = map {uc($_)} map {s/_//g;$_} @members;
		$classCounts[$ind] = ListManipulation::counts(\@members);
	}
}

my ($nodeCount, $nodesRef, $root) = loadTree($dnd);
my @listCounts = makeCounts(@listFiles);
my @presCounts = makeCounts(@presFiles);

{#output GTR...
	open(FILE, ">$gtr");
	print FILE join("\t", 'NODEID', 'LEFT', 'RIGHT', 'TIME'), "\n";
	for (@{$nodesRef}) {
		my $node = $_;
		my ($id,$left,$right,$depth) =map {$node->{$_}} 
		('NODEID', 'LEFT', 'RIGHT', 'DEPTH');
		print FILE join("\t", $id, $left->{'NODEID'}, $right->{'NODEID'}, $depth), "\n";
	}
	close(FILE);
}

{#output CDT
	open(FILE, ">$cdt");
	my @annoHeaders = ("GID", "YORF", "LEAF", "ALN", @listFiles, @presFiles);
	if ($opt_o) {
		push(@annoHeaders, @classes);
	}
	push (@annoHeaders, "NAME");
	if ($opt_f) {
		push (@annoHeaders, $opt_f);
	}
	print FILE join("\t", @annoHeaders, "GWEIGHT", "DUMMY"), "\n";
	# in addition to all headers, GWEIGHT is also null for this row.
	print FILE join("\t", "EWEIGHT", (map {""} @annoHeaders), 1),"\n";
	

	for (enumerateLeaves($root)) {
		my $node = $_;
		my $id = $_->{'NODEID'};
		# "GID", "YORF", "LEAF", "ALN"
		my @out = ($id, $id, $node->{'DEPTH'}+$node->{'LENGTH'}, $seq{$id});
		# listFiles
		push (@out, map {($_->{uc($id)})?' ':'M'} @listCounts);
		# presFiles
		push(@out, map {($_->{uc($id)})?'P':' '} @presCounts);
		if ($opt_o) {
			push(@out, map {($_->{uc($id)})?'P':' '} @classCounts);
		}
		# NAME
		push(@out, $id);
		# fasta annotation?
		if ($opt_f) {
			push (@out, $anno{uc($id)});
		}
		# GWEIGHT and DUMMY
		push (@out, 1, "");
		
		print FILE join("\t", @out), "\n";
	}
	close(FILE);
}



sub makeCounts{#load lists...
	my @files = @_;
	my @lists = map {ListManipulation::loadFromTdt($_,0)} @files;
	
	for (0 .. $#lists) {
		my $i = $_;
		$lists[$i] = [map {uc($_)} @{$lists[$i]}];
	}
	return map {ListManipulation::counts($_)} @lists;
}

sub loadAln {
	# expect file name as argument
	my $aln = shift();
	# return \@id, \%seq
	# list of ids, in alignment order
	# hash of id => sequence
	my @id;  # holds ids, in input order
	my %seq; # holds sequence, keyed by id
	{ # load alignments ...
		open (FILE, $aln);
		$_ = <FILE>; # skip first line.
		my $line = 0;
		while (<FILE>) {
			chomp();
			$line++;
			next unless (/\w/);
#			if (/([^\s].*[^\s])\s+([^\s]+)$/) {
			if (/^([\S].*[\S])\s+([\S]+)$/) {
				my $id = uc($1);
				my $seq = $2;
#				print "id $id, seq '$seq'\n";
				unless ($seq{$id}) {
					push(@id,$id);
				}
				$seq{$id} .= $seq;
			} else {
				die "could not parse line $line: '$_'";
			}
			
		}
		close(FILE);
	}
	return (\@id, \%seq);
}


sub enumerateLeaves {
	my $self = shift();
	if ($self->{'LEFT'}) {
		return (map {enumerateLeaves($self->{$_})} ('LEFT', 'RIGHT'));
	} else {
		return $self;
	}
}


sub recurseDepth {
	my $self = shift();
	my $current = shift();
	$self->{'DEPTH'} = $current +  $self->{'LENGTH'};
	my $id = $self->{'NODEID'};
	if ($self->{'LEFT'}) {
		recurseDepth($self->{'LEFT'}, $self->{'DEPTH'});
		recurseDepth($self->{'RIGHT'}, $self->{'DEPTH'});
	}
}

sub loadTree {
	my $nodeCount;
	my $nodesRef = [];
	my $root;
	{ # load tree
		open (FILE, $dnd);
		my $stack;
		while (<FILE>) {
			chomp();
			trim($_);
			$stack .= $_;
		}
		
		($root, $stack) = parseNode($stack, \$nodeCount, $nodesRef);
		print STDERR "root $root, id " .$root->{'NODEID'}." \n";
		print STDERR "$nodeCount total nodes\n";
		recurseDepth($root, 0);
	}
	return ($nodeCount, $nodesRef, $root);
}
sub parseNode {
	my $self = {};
	my $stack = shift();
	# holds number of internal nodes
	my $nodeCountRef = shift();
	# holds list of internal nodes
	my $nodesRef = shift();
	my $printName = 0;
	if ($stack  =~ s/^\s*\(//) { # internal node.
		my $kid;
		my @kids;
		while ($stack !~ s/^\s*\)//) {
			($kid, $stack) = parseNode($stack, $nodeCountRef, $nodesRef);
			($stack =~ s/^\s*\,//);
			push (@kids, $kid);
		}
		if ($#kids == 1) {
			$self->{'LEFT'} = $kids[0];
			$self->{'RIGHT'} = $kids[1];
		} else {
			my $count = $#kids + 1;
			print STDERR "Doh, got $count kids (" , join(", ",map {$_->{'NODEID'}} @kids), ") ";
			$printName = 1;
			$self->{'LEFT'} = shift(@kids);
			$self->{'RIGHT'} = zeroTree($nodesRef, $nodeCountRef, @kids);
		}
		$self->{'NODEID'} = 'NODE' . pad(${$nodeCountRef}++);
		push(@{$nodesRef}, $self);
	} else { # leaf node
		$stack =~ s/^\s*([^:]+)// || die "could not get name for leaf node, stack was $stack";
		$self->{'NODEID'} = uc(trim($1));
	}
	if ($printName == 1) {
		print STDERR "for node ", $self->{'NODEID'}, "\n";
	}
	if ($stack =~ s/^\s*:([-\d\.]+)// ) {
		$self->{'LENGTH'} = $1;
	} elsif ($stack =~ s/^\s*;// ) {
		$self->{'LENGTH'} = 0;
	} else {
		$self->{'LENGTH'} = 1;
	}
	
	return ($self, $stack);
}

sub zeroTree {
	my $nodesRef = shift();
	my $nodeCountRef = shift();
	my $left = shift();
	my $right;
	my @rest = @_;
	if ($#rest == 0) {
		$right = $rest[0];
	} else {
		$right = zeroTree(@rest);
	}

	my $self = {'NODEID' => 'NODE' . pad(${$nodeCountRef}++), 'LEFT' => $left,
	'RIGHT' => $right, 'LENGTH' => 0};
	push(@{$nodesRef}, $self);
		return($self);
}


sub getRoot {
	my $in = shift();
	$in =~ s/\.[^\.]+$//;
	return $in;
}

sub trim {
	my $in = shift();
	$in =~ s/^\s+//;
	$in =~ s/\s+$//g;
	return $in;
}

sub myexec  {
    my $exec  = shift();
    print STDERR "system($exec)\n";
    system($exec);
}

sub pad {
	my $int = shift();
	if ($int < 10) {
		return '0000' . $int;
	}
	if ($int < 100) {
		return '000' . $int;
	}
	if ($int < 1000) {
		return '00' . $int;
	}
	if ($int < 10000) {
		return '0' . $int;
	}
	return $int;
}

# Tree Scoring subroutines...
sub scoreTree {
		#takes Bio::Tree::TreeI as argument...
		my $tree = shift();
		# this is a Bio::Tree::NodeI
		my $node = $tree->get_root_node();
		my $rel = {};
		scoreNode($node, $rel);
		return $rel;
}
sub isHomogeneous {
	my $node = shift();
	my $rel  = {};
	my $type = scoreNode($node, $rel);
	return ($type ne 'MIXED');
}


sub scoreNode {
	# possibilities:
	#	1) No descendants
	#	2) All descendants are of one type
	#	3) All descendants are of mixed type
	
	my $self = shift();
	my $rel = shift();
	if ($self->is_Leaf()) {
		#	1) No descendants
		my $id = $self->id();
		return getType($self->id());
	}
	
	# process just the next generation
	my @nextGen = $self->each_Descendent();
	my @types = map {scoreNode($_, $rel)} @nextGen;
	my $type = $types[0];
	for (@types) {
		if ($_ eq $type) {
			#	2) All descendants are of one type
		} else {
			#	3) All descendants are of mixed type
			$type = 'MIXED';
		}
	}
	
	# now, to figure out what to emit:
	if (($type eq 'CE') or ($type eq 'CB')) {
		#we're homogeneous...
		return ($type);
	}
	
	if (allMixed(@types) == 1) {
		#all kids mixed...
		return ($type);
	}
	
	if ($#nextGen != 1) {
		my $num = $#nextGen+1;
		print STDERR "Internal node had $num children, skipping\n";
		return ($type);
	}
	# okay, time to score relation!
	if (mixedCount(@types) == 1) {
		push(@{$rel->{'Orphan'}}, $self);
	}

	# this should always be the case...
	if (mixedCount(@types) == 0) {
		if (leafCount(@nextGen) == 0) {
			#both are paralog families...
			push(@{$rel->{'Paralog'}}, $self);
		}
		
		if (leafCount(@nextGen) == 1) {
			#only one leaf...
			push(@{$rel->{'Expanded'}}, $self);
		}
		
		if (leafCount(@nextGen) == 2) {
			# pairs...
			push(@{$rel->{'Pair'}}, $self);
		}
	}
	return $type;
}

sub leafCount {
	my $count = 0;
	local($_);
	for (@_) {
		$count++ if ($_->is_Leaf());
	}
	return $count;
}

sub mixedCount {
	my $count = 0;
	local($_);
	for (@_) {
		$count++ if ($_ eq 'MIXED');
	}
	return $count;
}


sub allMixed {
	local($_);
	for (@_) {
		if ($_ ne 'MIXED') {
			return 0;
		}
	}
	return 1;
}


sub getType() {
	my $id = shift();
	if ($id =~ /CBG/) {
		return 'CB';
	} else {
		return 'CE';
	}
}
