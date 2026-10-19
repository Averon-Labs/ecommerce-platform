<?php

use Illuminate\Foundation\Application;
use Illuminate\Http\Request;

define('LARAVEL_START', microtime(true));

// Works with both layouts:
//   account root:  htdocs/index.php  +  eshopverse_app/   (recommended)
//   inside htdocs: htdocs/index.php  +  htdocs/eshopverse_app/
$appBase = is_dir(__DIR__.'/eshopverse_app')
    ? __DIR__.'/eshopverse_app'
    : __DIR__.'/../eshopverse_app';

if (file_exists($maintenance = $appBase.'/storage/framework/maintenance.php')) {
    require $maintenance;
}

require $appBase.'/vendor/autoload.php';

/** @var Application $app */
$app = require_once $appBase.'/bootstrap/app.php';

$app->handleRequest(Request::capture());
