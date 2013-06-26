#!/usr/bin/perl
my $val = pack("H*",shift(@ARGV));
my $dec = unpack("c*", $val);
my $char = pack("u*", $dec);
print "searching for $char\n";
while(<>) {
    print $_ if (/$val/);
}
