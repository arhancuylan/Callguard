<?php
declare(strict_types=1);
// Optional cPanel Terminal check. Deliberately unavailable over HTTP: no public phpinfo.
if (PHP_SAPI!=='cli') {http_response_code(404);exit;}
$checks=['PHP 8.2+'=>version_compare(PHP_VERSION,'8.2.0','>=')];
foreach (['pdo_mysql','openssl','curl','mbstring','json'] as $extension) $checks[$extension]=extension_loaded($extension);
$checks['Composer vendor package']=is_file(__DIR__.'/vendor/autoload.php');
$checks['Private config']=is_file(__DIR__.'/config.php');
foreach ($checks as $name=>$ok) echo ($ok?'PASS ':'FAIL ').$name.PHP_EOL;
exit(in_array(false,$checks,true)?1:0);
