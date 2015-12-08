#!/usr/bin/env ruby
require 'net/http'
require 'json'

if ARGV.length == 0
  puts 'Set search query after script name like "./search.rb fossasia"'
  exit
end

url = URI.parse('http://loklak.org/api/search.json?q=' + ARGV[0])
req = Net::HTTP::Get.new(url.to_s)
res = Net::HTTP.start(url.host, url.port) {|http|
  http.request(req)
}

tweets = JSON.parse(res.body)
tweets['statuses'].each do |tweet|
  puts tweet['text']
end
