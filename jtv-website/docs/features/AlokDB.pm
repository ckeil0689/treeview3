package AlokDB; # this package is object-oriented...

use IO::File;

sub new {
    #perl's god-awful object syntax:
    my $proto = shift;
    my $class = ref($proto) || $proto;
    my %args = @_;
    my $self = \%args;
    
    bless ($self, $class);
    $self->prime();
    return $self;
}

sub prime {
    my $self = shift();
    unless ($self->{'file'}) {
	die "Must specify file to load from";
    }
    unless (-f $self->{'file'}) {
	die "Could not find file file " . $self->{'file'};
    }

    $self->{'handle'} = new IO::File;
    open($self->{'handle'}, $self->{'file'}) or die "could noe open file " .$self->{'file'}. ": $!";
    $self->{'hasNext'} = 1;
    $self->parseCommand();
}

sub hasNext {
    my $self = shift();
    return $self->{'hasNext'};
}

sub next {
    my $self = shift();
    my $n = $self->{'command'};
    $self->parseCommand();
    return $n;
}

sub parseCommand {
    my $self = shift();
    if ($self->{'hasNext'} == 0) {
	return;
    }
    my $line;
    if ($self->{'line'} =~ /./) {
	$line = $self->{'line'};
	$self->{'line'} = '';
    } else {
	my $fh = $self->{'handle'};
	$line = <$fh>;
	unless ($line =~ /./) {
	    $self->{'hasNext'} = 0;
	    $self->{'command'} = [];
	    return;
	}
    }
    
    if ($line =~ /^:([^\s]+):(.*\n)/) {
	# found command, time to parse
	$self->{'hasNext'} = 1;
	
	my $key = $1;
	my $val = $2;
	my $fh = $self->{'handle'};
	while ($line = <$fh>) {
	    if ($line =~ /^:([^\s]+):/) {
		$self->{'line'}    = $line;
		last;
	    } else {
		$val .= $line
		    unless ($line =~ /^\s*$/);
	    }
	}
	chomp($val);
	$self->{'command'} = [$key, $val];
	return;
    } else {
	die "Parse error! expected :command:, got line " . $line;
    }

}
1;
