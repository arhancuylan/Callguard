<?php
declare(strict_types=1);
namespace CallGuard;
final class ApiError extends \RuntimeException {
    public function __construct(public readonly int $status, string $message) { parent::__construct($message); }
}
final class Policy {
    public const TRIAL_MS = 604800000;
    public static function entitlement(?int $start, bool $pro, int $now): array {
        return ['pro' => $pro, 'trialEnds' => $start === null ? 0 : $start + self::TRIAL_MS, 'serverTime' => $now];
    }
    public static function route(string $uri, string $baseUrl): string {
        $path = parse_url($uri, PHP_URL_PATH) ?: '/';
        $prefix = rtrim(parse_url($baseUrl, PHP_URL_PATH) ?: '', '/');
        if ($prefix !== '' && $path !== $prefix && !str_starts_with($path, $prefix . '/')) throw new ApiError(404, 'İşlem bulunamadı.');
        return '/' . ltrim(substr($path, strlen($prefix)), '/');
    }
    public static function email(mixed $value): string {
        if (!is_string($value)) throw new ApiError(400, 'Geçerli e-posta adresi yaz.');
        $email = strtolower(trim($value));
        if (strlen($email) > 254 || !filter_var($email, FILTER_VALIDATE_EMAIL)) throw new ApiError(400, 'Geçerli e-posta adresi yaz.');
        return $email;
    }
    public static function password(mixed $value): string {
        // bcrypt must not silently truncate inputs; use a SHA-384 prehash before password_hash.
        if (!is_string($value) || mb_strlen($value) < 12 || mb_strlen($value) > 128) throw new ApiError(400, 'Şifre 12–128 karakter olmalı.');
        return $value;
    }
    public static function passwordMaterial(string $value): string { return base64_encode(hash('sha384', $value, true)); }
    public static function backup(mixed $value): array {
        if (!is_array($value) || ($value['version'] ?? null) !== 1 || !is_array($value['contacts'] ?? null) || !array_is_list($value['contacts']) || count($value['contacts']) > 5000) throw new ApiError(400, 'Geçersiz yedek.');
        $contacts = []; $seen = [];
        foreach ($value['contacts'] as $entry) {
            if (!is_array($entry) || !is_string($entry['name'] ?? null) || mb_strlen($entry['name']) > 200 || !is_string($entry['number'] ?? null) || !preg_match('/^\+[1-9][0-9]{6,14}$/D', $entry['number']) || !is_bool($entry['white'] ?? null) || isset($seen[$entry['number']])) throw new ApiError(400, 'Geçersiz veya yinelenen kişi.');
            $seen[$entry['number']] = true;
            $contacts[] = ['name' => $entry['name'], 'number' => $entry['number'], 'white' => $entry['white']];
        }
        $settings = [];
        if (!is_array($value['settings'] ?? null)) throw new ApiError(400, 'Geçersiz ayarlar.');
        foreach ($value['settings'] as $key => $v) {
            if ($key === 'profile') { if (!in_array($v, ['custom','work','sleep','meeting'], true)) throw new ApiError(400, 'Geçersiz profil.'); $settings[$key] = $v; continue; }
            if (!in_array($key, ['rule','media','recordOnly'], true) && !preg_match('/^profile_(custom|work|sleep|meeting)_(rule|media|scheduled|days|start|end|initialized)$/D', (string)$key)) continue;
            $tail = preg_replace('/^profile_(custom|work|sleep|meeting)_/', '', $key);
            $valid = match ($tail) {
                'rule' => in_array($v, ['ALL','WHITELIST','BLACKLIST'], true),
                'media' => in_array($v, ['VOICE','VIDEO','BOTH'], true),
                'recordOnly','scheduled','initialized' => is_bool($v),
                'days' => is_int($v) && $v >= 0 && $v <= 127,
                'start','end' => is_int($v) && $v >= 0 && $v <= 1439,
                default => false,
            };
            if (!$valid) throw new ApiError(400, 'Geçersiz profil ayarı.');
            $settings[$key] = $v;
        }
        $history = $value['history'] ?? [];
        if (!is_array($history) || !array_is_list($history) || count($history) > 500) throw new ApiError(400, 'Geçersiz geçmiş.');
        $cleanHistory = [];
        foreach ($history as $entry) {
            if (!is_array($entry) || !is_string($entry['caller'] ?? null) || mb_strlen($entry['caller']) > 200 || !is_string($entry['result'] ?? null) || mb_strlen($entry['result']) > 200 || !is_bool($entry['video'] ?? null) || !is_int($entry['at'] ?? null) || $entry['at'] < 0 || !is_string($entry['number'] ?? null) || ($entry['number'] !== '' && !preg_match('/^\+[1-9][0-9]{6,14}$/D', $entry['number']))) throw new ApiError(400, 'Geçersiz arama kaydı.');
            $cleanHistory[] = array_intersect_key($entry, array_flip(['caller','result','video','at','number']));
        }
        return ['version' => 1, 'contacts' => $contacts, 'settings' => (object)$settings, 'history' => $cleanHistory];
    }
    public static function revision(mixed $expected, int $actual): void {
        if (!is_int($expected) || $expected !== $actual) throw new ApiError(409, 'Bulut yedeği başka cihazda değişmiş. Önce buluttan geri yükle.');
    }
}
