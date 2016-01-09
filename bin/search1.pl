#!/usr/bin/perl
# Install required modules:
#   cpan install JSON
#   cpan install LWP

use strict;                     
use warnings;             
use JSON qw( decode_json );           
use LWP::Simple;                

my $amount_of_args = $#ARGV + 1;
if ($amount_of_args != 1) {
	print "\nUsage: search.pl search_term\n";
	exit;
}
 
my $url = "http://loklak.org/api/search.json?q=$ARGV[0]";
my $json = get( $url );
die "Sorry! Could not open $url!" unless defined $json;

my $decoded_json = decode_json( $json );

foreach my $tweets(@{$decoded_json->{'statuses'}}){
	my %tweets_hash = ();
	$tweets_hash{text} = "$tweets->{'text'}";
	while (my($k, $v) = each (%tweets_hash)){
		print "$v\n";
	}
}
