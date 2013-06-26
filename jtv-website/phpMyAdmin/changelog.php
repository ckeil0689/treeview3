<?php
// Simple script to set correct charset for changelog
/* $Id: changelog.php,v 1.1 2004-12-05 20:16:29 alokito Exp $ */
// vim: expandtab sw=4 ts=4 sts=4:

header('Content-type: text/plain; charset=utf-8');
readfile('ChangeLog');
?>
