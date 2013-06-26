#!/usr/bin/perl
my $color = "#AA00FF";
my $black = "#000000";
my $line = <>;

# deal with header
my @fields = split("\t", $line);
splice(@fields,3, 0, "FGCOLOR");
print join("\t", @fields);

while ($line = <>) {
    #parse line
    my @fields = split("\t", $line);

    if ($fields[2] =~ /RPS/) {

	# color RPS gene purple
	splice(@fields,3, 0, $color);

    } elsif ($fields[2] =~ /RPL/) {

	# color RPL gene purple
	splice(@fields,3, 0, $color);

    } else {

	# color everything else black
	splice(@fields,3, 0, $black);

    }
    # spit line out again
    print join("\t", @fields);

}
