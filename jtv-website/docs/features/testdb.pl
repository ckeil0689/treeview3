#!/usr/bin/perl
use AlokDB;
my $db = AlokDB->new(file => 'test.db');
while ($db->hasNext()) {
    my $rec = $db->next();
    print "name: " . $rec->[0] . " val: '" . $rec->[1] ."'\n";
}
