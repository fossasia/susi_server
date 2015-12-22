#!/usr/bin/python 
import os
import requests


def print_response_from_file(file_name, path):
	current_file = open(path + file_name, 'r', encoding="utf8")
	for line in current_file:
		response = requests.get("http://localhost:9000/api/search.json?q=" +  line)
		x = response.json()
		try:
			print(x['statuses'])
		except Exception as e:
			print(e)


def print_all_files_lines_in_defined_directory(path):
	for fn in os.listdir(path):
	    if fn == 'README.txt':
	    	pass
	    elif fn.endswith(".txt"):
	        print_response_from_file(fn, path)
	    else:
	    	pass


if __name__ == "__main__":
	print_all_files_lines_in_defined_directory('../test/queries/')
