<?php
/* $Id: image_jpeg__link.inc.php,v 1.1 2004-12-05 20:16:35 alokito Exp $ */
// vim: expandtab sw=4 ts=4 sts=4:

function PMA_transformation_image_jpeg__link($buffer, $options = array(), $meta = '') {
    require_once('./libraries/transformations/global.inc.php');

    $transform_options = array ('string' => '<a href="transformation_wrapper.php' . $options['wrapper_link'] . '" alt="[__BUFFER__]">[BLOB]</a>');
    $buffer = PMA_transformation_global_html_replace($buffer, $transform_options);

    return $buffer;
}

?>
