<?php
/* $Id: tbl_properties.php,v 1.1 2004-12-05 20:16:29 alokito Exp $ */
// vim: expandtab sw=4 ts=4 sts=4:


/**
 * Runs common work
 */
require('./tbl_properties_common.php');
$err_url   = 'tbl_properties.php' . $err_url;
$url_query .= '&amp;goto=tbl_properties.php&amp;back=tbl_properties.php';

/**
 * Top menu
 */
require('./tbl_properties_table_info.php');

?>
<!-- TABLE WORK -->
<?php
/**
 * Query box, bookmark, insert data from textfile
 */
$goto = 'tbl_properties.php';
require('./tbl_query_box.php');

/**
 * Displays the footer
 */
echo "\n";
require_once('./footer.inc.php');
?>
