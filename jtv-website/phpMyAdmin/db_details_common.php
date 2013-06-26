<?php
/* $Id: db_details_common.php,v 1.1 2004-12-05 20:16:29 alokito Exp $ */
// vim: expandtab sw=4 ts=4 sts=4:

/**
 * Gets some core libraries
 */
require_once('./libraries/grab_globals.lib.php');
require_once('./libraries/common.lib.php');
require_once('./libraries/bookmark.lib.php');

PMA_checkParameters(array('db'));

/**
 * Defines the urls to return to in case of error in a sql statement
 */
$err_url_0 = 'main.php?' . PMA_generate_common_url();
$err_url   = $cfg['DefaultTabDatabase'] . '?' . PMA_generate_common_url($db);


/**
 * Ensures the database exists (else move to the "parent" script) and displays
 * headers
 */
if (!isset($is_db) || !$is_db) {
    // Not a valid db name -> back to the welcome page
    if (!empty($db)) {
        $is_db = PMA_DBI_select_db($db);
    }
    if (empty($db) || !$is_db) {
        PMA_sendHeaderLocation($cfg['PmaAbsoluteUri'] . 'main.php?' . PMA_generate_common_url('', '', '&') . (isset($message) ? '&message=' . urlencode($message) : '') . '&reload=1');
        exit;
    }
} // end if (ensures db exists)

/**
 * Changes database charset if requested by the user
 */
if (isset($submitcollation) && !empty($db_collation) && PMA_MYSQL_INT_VERSION >= 40101) {
    list($db_charset) = explode('_', $db_collation);
    $sql_query        = 'ALTER DATABASE ' . PMA_backquote($db) . ' DEFAULT' . PMA_generateCharsetQueryPart($db_collation);
    $result           = PMA_DBI_query($sql_query);
    $message          = $strSuccess;
    unset($db_charset, $db_collation);
}

// Displays headers
if (!isset($message)) {
    $js_to_run = 'functions.js';
    require_once('./header.inc.php');
    // Reloads the navigation frame via JavaScript if required
    if (isset($reload) && $reload) {
        echo "\n";
        ?>
<script type="text/javascript" language="javascript1.2">
<!--
var hashformDone = false;
function hashformReload() {
<?php
    if ($cfg['QueryFrame'] && $cfg['QueryFrameJS']) {
?>
    if (hashformDone != true) {
        if (typeof(window.parent.frames['queryframe'].document.hashform.hash) != 'undefined' && typeof(window.parent.frames['nav']) != 'undefined') {
            window.parent.frames['nav'].location.replace('./left.php?<?php echo PMA_generate_common_url('', '', '&');?>&hash=' +
                                                         window.parent.frames['queryframe'].document.hashform.hash.value);
            hashformDone = true;
        } else {
            setTimeout("hashformReload();",500);
        }
    }
<?php
    } else {
?>
        window.parent.frames['nav'].location.replace('./left.php?<?php echo PMA_generate_common_url('', '', '&');?>&hash=' +
                                                         '<?php echo md5($cfg['PmaAbsoluteUri']); ?>');
<?php
    }
?>
}
setTimeout("hashformReload();",500);
//-->
</script>
        <?php
    }
    echo "\n";
} else {
    PMA_showMessage($message);
}

/**
 * Set parameters for links
 */
$url_query = PMA_generate_common_url($db);

?>
