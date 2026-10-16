<?php
declare(strict_types=1);
// Copy to config.php OUTSIDE public_html. No real credentials belong in GitHub.
return [
    'base_url' => 'https://mktech.net.tr/callguard',
    'database' => [
        'host' => 'localhost', 'port' => 3306,
        'name' => 'CPANELUSER_callguard', 'user' => 'CPANELUSER_cguser',
        'password' => 'REPLACE_WITH_A_NEW_RANDOM_PASSWORD',
    ],
    // Generate independently: base64_encode(random_bytes(32)). Never reuse MK Tech keys.
    'data_key' => 'REPLACE_WITH_32_BYTES_BASE64',
    'account_key' => 'REPLACE_WITH_ANOTHER_32_BYTES_BASE64',
    'google_client_id' => '',
    'play_package' => 'com.callguard.mobile',
    'play_product' => 'callguard_pro_lifetime',
    // Separate least-privilege service account; file stays in the private directory.
    'play_credentials' => __DIR__ . '/play-service-account.json',
    'smtp' => [
        'host' => '', 'port' => 587, 'encryption' => 'tls',
        'username' => '', 'password' => '', 'from' => '', 'name' => 'CallGuard by AC',
    ],
];
