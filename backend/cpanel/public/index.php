<?php
declare(strict_types=1);
ini_set('display_errors', '0');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');
header("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'");
$private = getenv('CALLGUARD_PRIVATE_DIR') ?: dirname(__DIR__, 2) . '/callguard-private';
try {
    if (!is_file($private . '/config.php') || !is_file($private . '/vendor/autoload.php')) {
        http_response_code(503); header('Content-Type: application/json; charset=utf-8');
        echo '{"error":"CallGuard sunucu kurulumu henüz tamamlanmadı."}'; exit;
    }
    $resolved=realpath($private); $root=realpath($_SERVER['DOCUMENT_ROOT'] ?? __DIR__);
    if (!$resolved || !$root || $resolved===$root || str_starts_with($resolved,$root.DIRECTORY_SEPARATOR) || str_contains($resolved,'/public_html/')) throw new RuntimeException('Private directory must be outside all web roots');
    require $private . '/vendor/autoload.php';
    require $private . '/src/Policy.php';
    require $private . '/src/Api.php';
    (new \CallGuard\Api(require $private . '/config.php'))->handle();
} catch (Throwable $error) {
    http_response_code(503); header('Content-Type: application/json; charset=utf-8');
    // Never send DB details, file paths, tokens or credentials to the response/log.
    echo '{"error":"CallGuard servisine şu anda ulaşılamıyor."}';
}
