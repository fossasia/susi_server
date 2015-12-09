#!/usr/bin/perl
# Requirements Dep: CPAN:JSON
# Installtion: cpan install JSON

use LWP::Simple;                # From CPAN
use JSON qw( decode_json );     # From CPAN
use strict;                     # Good practice
use warnings;                   # Good practice

my $num_args = $#ARGV + 1;
if ($num_args != 1) {
	print "\nUsage: search.pl string\n";
	exit;
}

my $url = "http://loklak.org/api/search.json?q=$ARGV[0]";
my $json = get( $url );
die "Could not get $url!" unless defined $json;

my $decoded_json = decode_json( $json );

foreach my $tweets(@{$decoded_json->{'statuses'}}){
	my %tweets_hash = ();
	$tweets_hash{text} = "$tweets->{'text'}";
	while (my($k, $v) = each (%tweets_hash)){
		print "$v\n";
	}
}
