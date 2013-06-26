#!/usr/bin/perl
#
#
#  an html faq generator, primarilly for the efnet #nin faq
#
#   scott "jerry" lawrence
#   jsl@absynth.com
#
# 7 July 1999  - initial version
#

$faq_in_file   = "jtv.dat";
$faq_html_file = "../FAQ.html";

# color for question number
$qncolor = "222222";

$qcolor  = "222200";
$acolor  = "000000";

sub read_in_file
{
    open DATA_FILE, $faq_in_file;
    $qno = 0;
    $inq = 0;
    $ina = 0;

    while (<DATA_FILE>)
    {
        chomp $_;

	if ($inq > 0) {
	    $key = sprintf("q%03d", $qno);
	    if ($inq == 1) {
		$inq = 2;
		$data{$key}=sprintf("%s %s", $data{$key}, $_);
		printf("Q%3d: %s\n", $qno, $_);
	    } else {
		$subd{$key} .= $_;
	    }
	} elsif ($ina == 1)
	{
	    $ano++;
	    $key = sprintf("a%03d.%02d", $qno, $ano);
	    $data{$key}=sprintf("%s", $_);
	} else {
	    if($_ eq "q") {
		$inq = 1;
		$qno++;
		$ano=0;
	    } elsif ($_ eq "a") {
		$ina = 1;
	    } else {
		@foo = split "=", $_,2;
		$data{$foo[0]}= $foo[1];
	    }
	}
	if ($_ eq "")
	{
	    $inq = 0;
	    $ina = 0;
	}
    }
    close DATA_FILE;
    $data{"lastupdate"} = `date` unless ($data{"lastupdate"});
}


sub html_top
{
    print OF<<EOB;
<html>
<head>
<title>$data{"title"}</title>
</head>
<body bgcolor="white"
         text="black">
<style>
<!--
A:link    {text-decoration: underline; }
A:active  {text-decoration: underline; color: #ff0000 }
A:hover   {text-decoration: underline; color: #ff0000 }
-->
</style>



<center>
<h1>
$data{"title"}
</h1>
<br>
<br>
maintained by $data{"maintainer"}
<br>
<i>last updated $data{"lastupdate"}</i>
<br><br>
</center>
<hr width=50%>
<br>
EOB

#  <i>contributions by:</i><br>
#  EOB
#      $foo=" ";
#      $x = 1;
#      while ($foo ne "")
#      {
#  	   $foo = $data{sprintf "credits.%02d", $x};
#  	   printf OF "$foo<br>\n";
#  	   $x++;
#      }
#  
#      print OF<<EOB;
#  <hr width=50%>
#  <br>
#  </center>
#  
#  EOB
    
}

sub html_bottom
{
    print OF<<EOB;
</body></html>
EOB
}

sub html_toc
{
    $x = 1;
    $key = sprintf "q%03d", $x;

    printf OF "<dl>\n";

    for ($x=1 ; $data{$key} ne "" ; $x++)
    {
	printf OF "<dd><font size=+1 color=\"%s\"><b>$x.</b></font><a href=\"#q%03d\">\n", $qncolor, $x;
	printf OF "%s", $data{$key};
	printf OF "</a></dd>\n";
		
	$key = sprintf "q%03d", $x+1;
    }
    printf OF "</dl>\n";
    printf OF "<br><br>\n";
    printf OF "<hr width=\"50%\">\n";
    printf OF "<br><br>\n";
}

sub html_body
{
    $x = 1;
    $key = sprintf "q%03d", $x;

    printf OF "<dl>\n";

    for ($x=1 ; $data{$key} ne "" ; $x++)
    {
	printf OF "<dd><a name=q%03d></a><font size=+2 color=\"%s\"><b>$x.</b></font>\n",$x, $qncolor;
	printf OF "<font color=\"%s\"><b>%s\n", $qcolor, $data{$key};
	printf OF "$subd{$key}</b></font>\n";
	printf OF "<br><br><dl><font color=\"%s\">\n", $acolor;

	$y = 1;
	$keya = sprintf "a%03d.%02d", $x, $y;
	my $all;
	for ($y=1 ; $data{$keya} ne "" ; $y++)
	{
	    $all .= " " .$data{$keya};
	    $keya = sprintf "a%03d.%02d", $x, $y+1;
	}
	printf OF "<dd>%s</dd>\n", $all;

	printf OF "</font></dl><br><br><br>\n";
		
	$key = sprintf "q%03d", $x+1;
    }
    printf OF "</dl>\n";
}

sub output_html
{
    open OF, ">$faq_html_file";
    &html_top;
    &html_toc;
    &html_body;
    &html_bottom;
    close OF;
}
my %data;
my %subd
&read_in_file;
&output_html;
