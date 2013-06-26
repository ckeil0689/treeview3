<?php
/* $Id: server_common.inc.php,v 1.1 2004-12-05 20:16:29 alokito Exp $ */
// vim: expandtab sw=4 ts=4 sts=4:

/**
 * Gets some core libraries
 */
require_once('./libraries/grab_globals.lib.php');
require_once('./libraries/common.lib.php');

/**
 * Handles some variables that may have been sent by the calling script
 */
unset($db, $table);

/**
 * Set parameters for links
 */
$url_query = PMA_generate_common_url();

/**
 * Defines the urls to return to in case of error in a sql statement
 */
$err_url = 'main.php' . $url_query;

/**
 * Displays the headers
 */
require_once('./header.inc.php');

/**
 * Checks for superuser privileges
 */
// We were checking privileges with 'USE mysql' but users with the global
// priv CREATE TEMPORARY TABLES or LOCK TABLES can do a 'USE mysql'
// (even if they cannot see the tables)

$is_superuser = PMA_DBI_try_query('SELECT COUNT(*) FROM mysql.user');

// now, select the mysql db
if ($is_superuser) {
    PMA_DBI_free_result($is_superuser);
    PMA_DBI_select_db('mysql', $userlink);
    $is_superuser = TRUE;
} else {
    $is_superuser = FALSE;
}

?>
