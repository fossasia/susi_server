---

layout: default

title: Susi - Docs

---
# Susi

[![Join the chat at https://gitter.im/fossasia/susi_server](https://badges.gitter.im/fossasia/susi_server.svg)](https://gitter.im/fossasia/susi_server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/fossasia/susi_server.svg?branch=development)](https://travis-ci.org/fossasia/susi_server)
[![Join the chat at https://gitter.im/loklak/loklak](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/loklak/loklak)
[![Docker Pulls](https://img.shields.io/docker/pulls/mariobehling/loklak.svg?maxAge=2592000?style=flat-square)](https://hub.docker.com/r/mariobehling/loklak/)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/fossasia/susi_server.svg)](http://isitmaintained.com/project/fossasia/susi_server "Percentage of issues still open")
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/fossasia/susi_server.svg)](http://isitmaintained.com/project/fossasia/susi_server "Average time to resolve an issue")
[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Wow Check Susi on @gitHub @asksusi: https://github.com/fossasia/susi_server &url=%5Bobject%20Object%5D)
[![Twitter Follow](https://img.shields.io/twitter/follow/lklknt.svg?style=social&label=Follow&maxAge=2592000?style=flat-square)](https://twitter.com/lklknt)

Susi is a server application which is able to collect messages from various sources, including twitter. The server contains a search index and a peer-to-peer index sharing interface. All messages are stored in an elasticsearch index. An automatic deployment from the development branch at GitHub is available for tests here https://susi-server.herokuapp.com

## Communication

Please join our mailing list to discuss questions regarding the project: https://groups.google.com/forum/#!forum/loklak

Our chat channel is on gitter here: https://gitter.im/fossasia/susi_server

[**Contributior Guide**](/docs/contributor-guide.md)|
[**Documentation for Developers** :book:](/docs/documentation-guide.md)

## Why should I use Susi?

If you like to be anonymous when searching things, want to archive tweets or messages about specific topics and if you are looking for a tool to create statistics about tweet topics, then you may consider Susi. With Susi you can:

- collect and store a very, very large amount of tweets
- create your own search engine for tweets
- omit authentication enforcement for API requests on twitter
- share tweets and tweet archives with other loklak users
- search anonymously on your own search portal
- create your own tweet search portal or statistical evaluations
- use [Kibana](https://github.com/elastic/kibana) to analyze large amounts of tweets for statistical data.


## Where can I get the latest news about Susi?

Hey, this is the tool for that! Just put http://loklak.org/api/search.rss?q=%23susi into your rss reader. Oh wait.. you will get a lot of information about tasty Cambodian food with that as well. Alternatively you may also read the authors timeline using http://loklak.org/api/search.rss?q=0rb1t3r or just follow @0rb1t3r (that's a zero after the at sign)

## What is the software license?

LGPL 2.1

Have fun!
@0rb1t3r
