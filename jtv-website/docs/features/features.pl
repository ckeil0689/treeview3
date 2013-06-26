#!/usr/bin/perl
use AlokDB;

my $out = 'features.html';
my $in = 'features.db';

my @records = loadRecords();

for (@records) {
    printSummary($_);
}

my %data = (title => 'Coming Java TreeView Features',
	    maintainer => 'Alok Saldanha <a href="mailto:alok@genome.stanford.edu"> &lt;alok@genome.stanford.edu&gt; </a>',
	    lastupdate => 'Mon Oct 29 14:07:59 PST 2001'
	    );

$qncolor = "ff0000";
$qcolor  = "ffff00";
$acolor  = "00ffff";

output_html();

sub printSummary {
    my $rec =shift();
    my $num = $rec->{'num'};
    my $name = $rec->{'name'};
    print "$num: $name\n";
}

sub loadRecords {
    my @ret;
    my $db = AlokDB->new(file => $in);
    my $field = $db->next();
    while ($db->hasNext()) {
	if ($field->[0] eq 'record') {
	    my $rec = {};
	    $field = $db->next();
	    while ($field->[0] ne 'record') {
		my ($key, $val) = ($field->[0], $field->[1]);
		print STDERR "Warning, key $key multiply defined\n"
		    if ($rec->{$key});
		$rec->{$key} = $val;
		if ($db->hasNext()) {
		    $field = $db->next();
		} else {
		    last;
		}
	    }
	    push(@ret, $rec);
	} else {
	    die "expected record, got " . 
		"name: " . $rec->[0] . " val: '" . $rec->[1] ."'\n";
	}
    }
    return @ret;
}

sub html_top
{
    print OF<<EOB;
<html>
<head>
<title>$data{"title"}</title>
</head>
<body bgcolor="black"
         text="white"
         link="#ffff00"
        vlink="#ff0000"
        alink="#ff0000">
<style>
<!--
A:link    {text-decoration: none;      color: #ffff00 }
A:visited {text-decoration: none;      color: #ffff00 }
A:active  {text-decoration: underline; color: #ff0000 }
A:hover   {text-decoration: underline; color: #ff0000 }
-->
</style>



<font face="courier">

<center>
<font size=+1>
$data{"title"}
</font>
<br>
<br>
maintained by $data{"maintainer"}
<br>
<i>last updated by $data{"lastupdate"}</i>
<br><br>
<hr width=50%>
<br>
EOB
    
}

sub html_bottom
{
    print OF<<EOB;
</body></html>
EOB
}

sub html_toc
{
    printf OF "<dl>\n";

    for (@records)
    {
	my $rec = $_;
	my $num = $rec->{'num'};
	my $name = $rec->{'name'};
	
	printf OF "<dd><a href=\"#q%03d\"><font size=+2 color=\"%s\"><b>$num.</b></font>\n", $num, $qncolor;
	printf OF "%s", $name;
	printf OF "</a></dd>\n";
		
    }
    printf OF "</dl>\n";
    printf OF "<br><br>\n";
    printf OF "<hr width=\"50%\">\n";
    printf OF "<br><br>\n";
}

sub html_body
{
    printf OF "<dl>\n";

    for (@records)
    {
	my $rec = $_;
	my $num = $rec->{'num'};
	my $name = $rec->{'name'};
	my $desc = $rec->{'desc'} || "No Description Yet";
	my $impl = $rec->{'vImpl'} || 'unknown';
	printf OF "<dd><a name=q%03d></a><font size=+3 color=\"%s\"><b>$num.</b></font>\n",$num, $qncolor;
	printf OF "<font size=+1 color=\"%s\"><b>%s</b></font>\n", $qcolor, $name;
	printf OF "<br><br><dl><font color=\"%s\">\n", $acolor;

	printf OF "<dd>%s</dd>\n", $desc;
	printf OF "<dd>Planned Implementation Version: <font color=\"#9999FF\"> %s </font> </dd>\n", $impl;

	printf OF "</font></dl><br><br><br>\n";		
    }
    printf OF "</dl>\n";
}

sub output_html
{
    open OF, ">$out";
    &html_top;
    &html_toc;
    &html_body;
    &html_bottom;
    close OF;
}
