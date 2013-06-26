#!/usr/bin/perl

BEGIN {
    push(@INC, $ENV{'HOME'}.'/perl');
}
use strict;
use vars qw($opt_t $opt_c $opt_h);
use Getopt::Std;
use ListManipulation;

sub usage {
	print qq!Usage:
	$0 -t <threshold> [-c <color file>] [-h <header>] <Tree File>
	
Adds NODECOLOR column to tree file (.gtr or .atr) 
colors in subtrees different colors. 
subtrees defined as nodes that cross <threshold> cutoff 
NOTE - will overwrite existing NODECOLOR.
	
	-t	threshold at which to define subtree
	-c	(Optional) file consisting of #RRGGBB hex colors, one per line, to iterate through when coloring subtrees. Defaults to rainbow colors. See example file "blues.color"
	-h	(Optional) header of tree file to apply threshold to. Defaults to CORRELATION.

!;
	exit(1);
}

getopts('t:c:h:'); # column number of pcl,cdt files to join on.
my $trFile = shift(@ARGV);

############ THRESHOLD
my $threshold;
if ($opt_t =~ /\d/) {
	$threshold = $opt_t;
} else {
	if ($opt_t) {
		print STDERR "Invalid threshold $opt_t. Threshold must be number.\n";
		exit(1);
	} else {
		usage();
	}
}



############ COLORS
my @colors;
if ($opt_c) {
	@colors = @{ListManipulation::loadFromFile($opt_c)};
} else {
	@colors = (
	'#990000', # Red
	'#FF5555', # Orange
	'#55FF00', # Yellow
	'#009900', # Green
	'#0000FF', # Blue
	'#9900FF' # Purple
	);
}

############ HEADER
my $header;
if ($opt_h) {
	$header = $opt_h;
} else {
	$header = 'CORRELATION';
}


my $gtr = GTR_Analysis::newFromTrFile($trFile);
my $index = $gtr->getHeaderIndex($header);
if ($index < 0) {
	die " Error: Specified header $header not found";
} else {
	print STDERR "Using header $index, '$header'\n";
}

my $startVal = $gtr->getValueByIndex($gtr->getNumNode() - 1, $index);
my $startGreater;
if ($startVal < $threshold) {
	print STDERR "root has $startVal, looking for subtrees with $header > $threshold\n";
	$startGreater = (1 == 0);
} else {
	print STDERR "root has $startVal, looking for subtrees with $header < $threshold\n";
	$startGreater = (1 == 1);
}

my $colorIndex = $gtr->getHeaderIndex('NODECOLOR');
if ($colorIndex < 0) {
	print STDERR "Adding column NODECOLOR\n";
	$colorIndex = $gtr->addColumn('NODECOLOR');
} else {
	print STDERR "Overwriting column $colorIndex, NODECOLOR\n";
}

my $clusterCount = 0;
my @stack = $gtr->getNumNode() - 1; # keep track of remaining nodes during tree traversal.
while ($#stack >= 0) {
	my $nodeIdx = shift(@stack);
	next if ($nodeIdx<0); # hit leaf node.
	my $crossedThreshold = ''; # boolean to score threshold crossing...
	{
		my $val = $gtr->getValueByIndex($nodeIdx, $index);
		if ($startGreater) { # need to look for decrease below T.
			$crossedThreshold = 1 if ($val < $threshold);
		} else {
			$crossedThreshold = 1 if ($val > $threshold);
		}
	}
	if ($crossedThreshold) {
		$gtr->setValueByIndexRecursive($nodeIdx, $colorIndex, $colors[$clusterCount++ % ($#colors + 1)]);
	} else {
		$gtr->setValueByIndex($nodeIdx, $colorIndex, '#000000');
# depth or breadth?
#		unshift(@stack,
		push(@stack, 
		$gtr->getLeftChild($nodeIdx),
		$gtr->getRightChild($nodeIdx));
	}
}
print STDERR "Found $clusterCount clusters\n";

$gtr->saveTrFile($trFile);

package GTR_Analysis;


################################################################################
#new creates a skeleton object with no nodes
################################################################################
################################################################################
sub new{
	bless {
		HeaderRow => [],
		NodeList => [],
		NodeHash => '',
		NumHeader => 0,
		NumNode => 0
	};
}
################################################################################
################################################################################
#newFromPCLfile - accept one argument (filename for a Tree file) and return
#the GTR object.  Assumes every column up to EWEIGHT is annotation, everything 
#after is data.  Score columns mislabeled as annotation can be recast as score
#or data columns later
################################################################################
################################################################################
sub newFromTrFile{
    my $self = new;
    my $filename = shift;
		unless(open(FILE, $filename)){
			Message("Couldn't open $filename - can't insert data\n");
			return;
		}
		Message("Loading from $filename \n");
		$self->LoadFromFh(*FILE);
		close(FILE);
		return $self;
}

################################################################################
################################################################################
# Arguments - 1)Dataset Object 2)Filename
#
# Result - Inserts the data in the tree file specified into the dataset
################################################################################
################################################################################
sub LoadFromFh{
    my $self = shift();
		my $fh = shift(); # must pass glob or strict will barf.
		
		my $line1 = <$fh>;
		chomp $line1;
		my(@line1) = split(/\t/, $line1);

		my $i = 0;
		if ($line1[0] eq 'NODEID') {
			$self->{'HeaderRow'} = \@line1;
			$self->{'NumHeader'} = $#line1 + 1;
		} else {
			if ($#line1 == 3) {
				$self->{'HeaderRow'} = ['NODEID', 'LEFT', 'RIGHT','CORRELATION'];
				$self->{'NumHeader'} = 4;
				
				$self->{'NodeList'}[$i] = \@line1;
				$i++;
				
			} else{
				# houston, we have a problem!
				my $n = $#line1 + 1;
				print STDERR "Error parsing tree file! found $n headers, so cannot be legacy tree file, but first header was $line1[0], not NODEID, so cannot be generalized tree file. Please fix";
				exit(1);
			}
		}
		

		
		while (<$fh>) {
			chomp();
			my(@linei) = split(/\t/, $_);
			$self->{'NodeList'}[$i] = \@linei;
			Message("Loaded $i\n") if ($i % 1000 == 0);
			$i++;
		}
		Message("$i Total Nodes\n");
		$self->{'NumNode'} = $i;
}
sub hashNodes {
	my $self = shift();
	my $i;
	my $nodes = $self->{'NodeList'};
	my %hash;
	for ($i = 0; $i < $self->getNumNode(); $i++) {
		$hash{$nodes->[$i][0]} = $i;
	}
	$self->{'NodeHash'} = \%hash;
}

sub hashHeaders {
	my $self = shift();
	my $i;
	my $nodes = $self->{'HeaderRow'};
	my %hash;
	for ($i = 0; $i < $self->getNumHeader(); $i++) {
		$hash{$nodes->[$i]} = $i;
	}
	$self->{'HeaderHash'} = \%hash;
}

sub saveTrFile{
	my $self = shift();
	my $outFile = shift();
	open (OUT, ">$outFile");
	print OUT join("\t", @{$self->{'HeaderRow'}}), "\n";
	for (@{$self->{'NodeList'}}) {
	print OUT join("\t", @{$_}), "\n";
	}
}	


########################################################
# returns left child of node specified by index
########################################################
sub getLeftChild {
	my $self = shift();
	my $nodeIdx = shift();
	$self->hashHeaders() unless ($self->{'HeaderHash'});
	my $leftId = $self->getValueByIndex
	($nodeIdx, $self->{'HeaderHash'}{'LEFT'});
	return $self->getNodeIndex($leftId);
}

########################################################
# returns right child of node specified by index
########################################################
sub getRightChild {
	my $self = shift();
	my $nodeIdx = shift();
	$self->hashHeaders() unless ($self->{'HeaderHash'});
	return $self->getNodeIndex($self->getValueByIndex
	($nodeIdx, $self->{'HeaderHash'}{'RIGHT'}));
}

sub getNodeIndex {
	my $self = shift();
	$self->hashNodes() unless ($self->{'NodeHash'});
	my $nodeId = shift();
	my $idx = $self->{'NodeHash'}{$nodeId};
	if ($idx =~ /\d/) {
		return $idx;
	} else {
		return -1;
	}
}

sub addColumn {
	my $self = shift();
	my $name = shift();
	my $n = $self->getNumHeader();
	$self->{'HeaderRow'}[$n] = $name;
	$self->{'NumHeader'}++;
	$self->{'HeaderHash'} = '';
	return $n;
}
sub getNumHeader() {
	my $self = shift();
	return $self->{'NumHeader'};
}
sub getNumNode() {
	my $self = shift();
	return $self->{'NumNode'};
}

sub getHeaders {
	my $self = shift();
	return @{$self->{'HeaderRow'} };
}
sub getHeaderIndex {
	my $self = shift();
	my $header = shift();
	my @headers = $self->getHeaders();
	for (0 .. $#headers) {
		return $_ if (uc($headers[$_]) eq $header);
	}
	return -1;
}
sub getValueByIndex {
	my $self = shift();
	my $nodeIndex = shift();
	my $headerIndex = shift();
	return $self->{'NodeList'}[$nodeIndex][$headerIndex];
}

sub setValueByIndex {
	my $self = shift();
	my $nodeIndex = shift();
	my $headerIndex = shift();
	my $value = shift();
	$self->{'NodeList'}[$nodeIndex][$headerIndex] = $value;
}
sub setValueByIndexRecursive {
	my $self = shift();
	my $nodeIndex = shift();
	my $headerIndex = shift();
	my $value = shift();
	$self->{'NodeList'}[$nodeIndex][$headerIndex] = $value;
	my $leftIdx = $self->getLeftChild($nodeIndex);
	my $rightIdx = $self->getRightChild($nodeIndex);
	if ($leftIdx >= 0) {
		$self->setValueByIndexRecursive($leftIdx, $headerIndex, $value);
	}
	if ($rightIdx >= 0) {
		$self->setValueByIndexRecursive($rightIdx, $headerIndex, $value);
	}
}
################################################################################
sub message {
    my ($msg, $device, $logfile) = @_;
    if ($device eq "" || $device eq "console") {
        untie *STDERR;
    } else {
        tie *STDERR, ref $device, $device;
    }
    print STDERR $msg;
    untie *STDERR;
    if($logfile ne "") {
        print $logfile $msg;
    }
}

sub Message {
    message(@_);
}