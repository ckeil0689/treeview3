#!/bin/bash
# $Id: template_generator_mimetype.sh,v 1.1 2004-12-05 20:16:35 alokito Exp $
#
# Shell script that adds a new mimetype without transform function.
#
# The filename should contain either 'mimetype_subtype' or 'mimetype'.
# The suffix '.inc.php' is appended automatically!
#
# Example:  template_generator_mimetype.sh 'filename'
# 
if [ $# == 0 ]
then
  echo "Usage: template_generator_mimetype.sh 'filename'"
  echo ""
  exit 65
fi

./generator.sh 'TEMPLATE_MIMETYPE' "$1"
echo " "
echo "New MIMETYPE $1.inc.php added."
