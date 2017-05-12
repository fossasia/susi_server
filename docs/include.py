"""Module to include documentation files from the parent directory
without breaking relative links.

Usage
-----
python3 inlcude.py [filename [filename ...]]
"""
import os
import re
import argparse
import functools


rel_link_template = {'rst': (':doc:`{}<{}>`', re.compile(r'`(?P<title>[^`]*?)</docs/(?P<rel_path>[^`]*?)>`__')),
                     'md': ('[{}]({})', re.compile(r'\[(?P<title>[^\[\]]*?)\]\(/docs/(?P<rel_path>[^)(]*?)\)')),}


def _fixlink(matchObject, fmtstr):
    """Helper method to be used as a callback for re.sub"""
    rel_path = matchObject.group('rel_path')
    title = matchObject.group('title')
    rel_path = rel_path.replace('.rst', '').replace('.md', '')
    return fmtstr.format(title, rel_path)


def _get_filetype(filename):
    """Return extension of a file"""
    return filename.split('.')[-1]


def include_file(filename):
    """Includes a file from the parent dir named `filename`.

    Parameters
    ----------
    filename: Name of the documentation file to be included.
    """
    filetype = _get_filetype(filename)
    string = ''
    with open(os.path.join(os.pardir, filename)) as file:
        string = file.read(-1)
    try:
        regexp = rel_link_template[filetype][1]
        func = functools.partial(_fixlink, fmtstr=rel_link_template[filetype][0])
    except KeyError:
        # File is not rst or md. Copy the contents as it is.
        print("No regular expression found for filetype {}, ignoring {}".format(filetype, filename))
    else:
        # Matching template found. Fix the relative links
        string = re.sub(regexp, func, string)
    finally:
        # Create the included file inside the docs directory with updated contents.
        with open(filename, 'w') as new_file:
            new_file.write(string)


def main():
    parser = argparse.ArgumentParser(description='Include files from root while fixing relative links')
    parser.add_argument('filename', type=str, nargs='*',
                        help='includes the specified files')
    filenames = parser.parse_args().filename
    
    for filename in filenames:
        include_file(filename)
        
            

if __name__ == '__main__':
    main()

