<?php
declare(strict_types=1);
namespace CallGuard;

final class Api {
    private \PDO $db;
    private string $dataKey;
    private string $accountKey;
    private ?\Google\Client $playClient = null;
    public function __construct(private array $config) {
        $url = parse_url($config['base_url'] ?? '');
        if (($url['scheme'] ?? '') !== 'https' || empty($url['host']) || isset($url['query']) || isset($url['fragment'])) throw new \RuntimeException('Invalid base URL');
        $this->dataKey = base64_decode($config['data_key'] ?? '', true) ?: '';
        $this->accountKey = base64_decode($config['account_key'] ?? '', true) ?: '';
        if (strlen($this->dataKey) !== 32 || strlen($this->accountKey) !== 32 || hash_equals($this->dataKey, $this->accountKey)) throw new \RuntimeException('Independent 32-byte keys required');
        $c = $config['database'];
        if (!preg_match('/^[a-zA-Z0-9_]+$/D', $c['name'])) throw new \RuntimeException('Invalid database');
        $this->db = new \PDO('mysql:host='.$c['host'].';port='.(int)($c['port'] ?? 3306).';dbname='.$c['name'].';charset=utf8mb4', $c['user'], $c['password'], [\PDO::ATTR_ERRMODE=>\PDO::ERRMODE_EXCEPTION, \PDO::ATTR_DEFAULT_FETCH_MODE=>\PDO::FETCH_ASSOC, \PDO::ATTR_EMULATE_PREPARES=>false]);
    }
    private function now(): int { return (int)floor(microtime(true) * 1000); }
    private function query(string $sql, array $params=[]): \PDOStatement { $statement=$this->db->prepare($sql); $statement->execute($params); return $statement; }
    private function userId(string $email): string { return hash_hmac('sha256',$email,$this->accountKey); }
    private function transaction(callable $callback): mixed {
        $this->db->beginTransaction();
        try { $result=$callback(); $this->db->commit(); return $result; }
        catch (\Throwable $e) { $this->db->rollBack(); throw $e; }
    }
    private function encrypt(string $plain): string {
        $iv=random_bytes(12); $tag=''; $cipher=openssl_encrypt($plain,'aes-256-gcm',$this->dataKey,OPENSSL_RAW_DATA,$iv,$tag);
        if ($cipher===false) throw new \RuntimeException('Encryption failed');
        return base64_encode($iv.$tag.$cipher);
    }
    private function decrypt(string $encoded): string {
        $raw=base64_decode($encoded,true); if ($raw===false || strlen($raw)<28) throw new \RuntimeException('Invalid encrypted data');
        $plain=openssl_decrypt(substr($raw,28),'aes-256-gcm',$this->dataKey,OPENSSL_RAW_DATA,substr($raw,0,12),substr($raw,12,16));
        if ($plain===false) throw new \RuntimeException('Authentication tag mismatch'); return $plain;
    }
    private function session(array $user): array {
        $token=bin2hex(random_bytes(32));
        $this->query('INSERT INTO cg_sessions(token_hash,user_id,expires_ms) VALUES(?,?,?)',[hash('sha256',$token),$user['id'],$this->now()+2592000000]);
        return ['token'=>$token,'userId'=>$user['id'],'email'=>$user['email']];
    }
    private function auth(?string $token): array {
        if (!preg_match('/^[a-f0-9]{64}$/D',$token ?? '')) throw new ApiError(401,'Tekrar giriş yap.');
        $user=$this->query('SELECT u.* FROM cg_sessions s JOIN cg_users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_ms>?',[hash('sha256',$token),$this->now()])->fetch();
        if (!$user) throw new ApiError(401,'Oturum sona ermiş. Tekrar giriş yap.'); return $user;
    }
    private function emailToken(array $user,string $kind): void {
        $smtp=$this->config['smtp'];
        if (empty($smtp['host']) || empty($smtp['from'])) throw new ApiError(503,'E-posta servisi henüz kurulmadı.');
        $token=bin2hex(random_bytes(32));
        $this->query('INSERT INTO cg_email_tokens(token_hash,user_id,kind,expires_ms) VALUES(?,?,?,?)',[hash('sha256',$token),$user['id'],$kind,$this->now()+3600000]);
        $mail=new \PHPMailer\PHPMailer\PHPMailer(true); $mail->isSMTP(); $mail->CharSet='UTF-8';
        $mail->Host=$smtp['host']; $mail->Port=(int)$smtp['port']; $mail->Timeout=12;
        $mail->SMTPAuth=true; $mail->Username=$smtp['username']; $mail->Password=$smtp['password'];
        $mail->SMTPSecure=($smtp['encryption'] ?? 'tls')==='ssl' ? \PHPMailer\PHPMailer\PHPMailer::ENCRYPTION_SMTPS : \PHPMailer\PHPMailer\PHPMailer::ENCRYPTION_STARTTLS;
        // Keep PHPMailer's TLS verification defaults; never accept self-signed/unverified peers.
        $mail->setFrom($smtp['from'],$smtp['name'] ?? 'CallGuard'); $mail->addAddress($user['email']);
        $mail->Subject=match($kind){'verify'=>'CallGuard e-posta doğrulaması','delete'=>'CallGuard hesap silme onayı',default=>'CallGuard şifre yenileme'};
        $mail->Body='İşlemi tamamlamak için bağlantıyı aç: '.rtrim($this->config['base_url'],'/').'/v1/email/'.$kind.'?token='.$token."\nBu isteği sen yapmadıysan bu e-postayı yok say.";
        try { $mail->send(); } catch (\Throwable $e) { $this->query('DELETE FROM cg_email_tokens WHERE token_hash=?',[hash('sha256',$token)]); throw new ApiError(503,'E-posta şu anda gönderilemiyor. Yeniden dene.'); }
    }
    private function takeToken(string $token,string $kind,callable $action): mixed {
        if (!preg_match('/^[a-f0-9]{64}$/D',$token)) throw new ApiError(400,'Geçersiz bağlantı.');
        return $this->transaction(function() use($token,$kind,$action) {
            $row=$this->query('SELECT user_id FROM cg_email_tokens WHERE token_hash=? AND kind=? AND expires_ms>? FOR UPDATE',[hash('sha256',$token),$kind,$this->now()])->fetch();
            if (!$row) throw new ApiError(400,'Bağlantı sona ermiş veya kullanılmış.');
            $result=$action($row['user_id']); $this->query('DELETE FROM cg_email_tokens WHERE token_hash=?',[hash('sha256',$token)]); return $result;
        });
    }
    private function play(string $token,array $user,bool $acknowledge=false): void {
        if ($token==='' || strlen($token)>4096) throw new ApiError(400,'Geçersiz satın alma.');
        $path=$this->config['play_credentials'];
        if (!is_file($path)) throw new ApiError(503,'Satın alma doğrulaması henüz kurulmadı.');
        if ($this->playClient===null) {
            $this->playClient=new \Google\Client(); $this->playClient->setAuthConfig($path);
            $this->playClient->setScopes(['https://www.googleapis.com/auth/androidpublisher']);
            $this->playClient->setHttpClient(new \GuzzleHttp\Client(['timeout'=>12,'connect_timeout'=>5,'verify'=>true]));
        }
        $client=$this->playClient->authorize();
        $url='https://androidpublisher.googleapis.com/androidpublisher/v3/applications/'.rawurlencode($this->config['play_package']).'/purchases/products/'.rawurlencode($this->config['play_product']).'/tokens/'.rawurlencode($token);
        try {
            $response=$client->request('GET',$url); $data=json_decode((string)$response->getBody(),true,512,JSON_THROW_ON_ERROR);
            if (($data['purchaseState'] ?? null)!==0 || ($data['consumptionState'] ?? null)===1) throw new ApiError(403,'Satın alma tamamlanmamış veya iptal edilmiş.');
            if (!hash_equals(hash('sha256',$user['id']),$data['obfuscatedExternalAccountId'] ?? '')) throw new ApiError(403,'Satın alma farklı bir CallGuard hesabına ait.');
            if ($acknowledge && ($data['acknowledgementState'] ?? 0)===0) $client->request('POST',$url.':acknowledge',['json'=>(object)[]]);
        } catch (\GuzzleHttp\Exception\ClientException $e) {
            if (in_array($e->getResponse()->getStatusCode(),[404,410],true)) throw new ApiError(403,'Satın alma kaydı geçerli değil.');
            // Google API authorization/configuration failures are not evidence of a refunded purchase.
            throw new ApiError(503,'Google Play doğrulaması şu anda yapılamıyor.');
        }
    }
    private function status(array $user): array {
        $pro=(bool)$user['pro'];
        $purchase=$this->query('SELECT token_encrypted FROM cg_purchases WHERE user_id=? ORDER BY updated_ms DESC LIMIT 1',[$user['id']])->fetch();
        if ($purchase) {
            try { $this->play($this->decrypt($purchase['token_encrypted']),$user); $pro=true; }
            catch (ApiError $e) { if ($e->status!==403) throw $e; $pro=false; }
            if ($pro!==(bool)$user['pro']) $this->query('UPDATE cg_users SET pro=? WHERE id=?',[(int)$pro,$user['id']]);
        }
        $trial=$this->query('SELECT started_ms FROM cg_trials WHERE user_id=?',[$user['id']])->fetch();
        return Policy::entitlement($trial ? (int)$trial['started_ms'] : null,$pro,$this->now());
    }
    private function jsonBody(): array {
        if ((int)($_SERVER['CONTENT_LENGTH'] ?? 0)>4194304) throw new ApiError(413,'Yedek en fazla 4 MB olabilir.');
        $raw=file_get_contents('php://input',false,null,0,4194305);
        if ($raw===false || strlen($raw)>4194304) throw new ApiError(413,'Yedek en fazla 4 MB olabilir.');
        if (str_starts_with($_SERVER['CONTENT_TYPE'] ?? '','application/x-www-form-urlencoded')) { parse_str($raw,$data); return $data; }
        try { $data=$raw===''?[]:json_decode($raw,true,512,JSON_THROW_ON_ERROR); } catch (\JsonException $e) { throw new ApiError(400,'Geçersiz istek.'); }
        if (!is_array($data)) throw new ApiError(400,'Geçersiz istek.'); return $data;
    }
    private function rate(string $route): void {
        // Shared hosting: use database counters, never process-local memory or spoofable forwarding headers.
        $window=intdiv(time(),60); $kind=str_starts_with($route,'/v1/auth/')?'auth':'api';
        $bucket=hash_hmac('sha256',($_SERVER['REMOTE_ADDR'] ?? 'unknown').':'.$kind,$this->accountKey);
        $this->query('INSERT INTO cg_rate_limits(bucket_hash,window_id,hits) VALUES(?,?,1) ON DUPLICATE KEY UPDATE hits=IF(window_id=VALUES(window_id),hits+1,1),window_id=VALUES(window_id)',[$bucket,$window]);
        $row=$this->query('SELECT hits FROM cg_rate_limits WHERE bucket_hash=?',[$bucket])->fetch();
        if ((int)$row['hits']>($kind==='auth'?15:90)) throw new ApiError(429,'Çok fazla istek. Bir dakika sonra yeniden dene.');
        if (random_int(1,100)===1) $this->query('DELETE FROM cg_rate_limits WHERE window_id<?',[$window-60]);
    }
    private function html(string $content): array { return ['_html'=>'<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>CallGuard</title><body style="font:18px system-ui;max-width:480px;margin:48px auto;padding:24px;color:#08634e"><h1>CallGuard by AC</h1>'.$content.'</body>']; }
    public function handle(): void {
        try {
            if (($_SERVER['HTTPS'] ?? '')!=='on' && ($_SERVER['SERVER_PORT'] ?? '')!=='443') throw new ApiError(426,'HTTPS bağlantı gerekli.');
            $route=Policy::route($_SERVER['REQUEST_URI'] ?? '/',$this->config['base_url']); $this->rate($route);
            $authorization=$_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
            $token=str_starts_with($authorization,'Bearer ')?substr($authorization,7):null;
            $result=$this->dispatch($_SERVER['REQUEST_METHOD'] ?? 'GET',$route,$this->jsonBody(),$token,$_GET);
            if (isset($result['_html'])) { header('Content-Type: text/html; charset=utf-8'); echo $result['_html']; }
            else { header('Content-Type: application/json; charset=utf-8'); echo json_encode($result,JSON_UNESCAPED_UNICODE|JSON_THROW_ON_ERROR); }
        } catch (ApiError $e) { http_response_code($e->status); header('Content-Type: application/json; charset=utf-8'); echo json_encode(['error'=>$e->getMessage()],JSON_UNESCAPED_UNICODE); }
    }
    public function dispatch(string $method,string $route,array $data=[],?string $token=null,array $query=[]): array {
        if ($method==='GET' && $route==='/health') return ['ok'=>true,'service'=>'CallGuard'];
        if ($method==='GET' && $route==='/account-deletion') return $this->html('<p>E-posta adresine gönderilecek bağlantıyla hesabını ve bulut yedeğini silebilirsin. Deneme ve satın almaya ilişkin asgari doğrulama kayıtları korunur.</p><form method="post" action="'.htmlspecialchars(rtrim($this->config['base_url'],'/').'/v1/auth/delete-request',ENT_QUOTES,'UTF-8').'"><input type="email" name="email" required autocomplete="email"><button>Silme bağlantısı gönder</button></form>');
        if ($method==='GET' && $route==='/v1/email/verify') {
            $this->takeToken($query['token'] ?? '','verify',function($id){$this->query('UPDATE cg_users SET verified=1 WHERE id=?',[$id]);});
            return $this->html('E-posta doğrulandı. Uygulamaya dönüp giriş yapabilirsin.');
        }
        if ($method==='GET' && in_array($route,['/v1/email/reset','/v1/email/delete'],true)) {
            $t=$query['token'] ?? ''; if (!is_string($t) || !preg_match('/^[a-f0-9]{64}$/D',$t)) throw new ApiError(400,'Geçersiz bağlantı.');
            $delete=str_ends_with($route,'/delete');
            $fields=$delete?'<p>Hesabın ve bulut yedeğin kalıcı olarak silinecek.</p>':'<label>Yeni şifre (en az 12 karakter)<input type="password" name="password" minlength="12" maxlength="128" required autocomplete="new-password"></label>';
            return $this->html('<form method="post" action="'.htmlspecialchars(rtrim($this->config['base_url'],'/').$route,ENT_QUOTES,'UTF-8').'"><input type="hidden" name="token" value="'.$t.'">'.$fields.'<button>'.($delete?'Hesabı ve yedeği sil':'Şifreyi yenile').'</button></form>');
        }
        if ($method==='POST' && $route==='/v1/email/reset') {
            $password=Policy::password($data['password'] ?? null); $passwordHash=password_hash(Policy::passwordMaterial($password),PASSWORD_DEFAULT);
            $this->takeToken($data['token'] ?? '','reset',function($id) use($passwordHash){$this->query('UPDATE cg_users SET password_hash=? WHERE id=?',[$passwordHash,$id]);$this->query('DELETE FROM cg_sessions WHERE user_id=?',[$id]);});
            return $this->html('Şifren yenilendi. Uygulamaya dönüp giriş yapabilirsin.');
        }
        if ($method==='POST' && $route==='/v1/email/delete') {
            $this->takeToken($data['token'] ?? '','delete',function($id){$this->query('DELETE FROM cg_users WHERE id=?',[$id]);}); return $this->html('Hesabın ve bulut yedeğin silindi.');
        }
        if ($method==='POST' && $route==='/v1/auth/challenge') {
            $nonce=bin2hex(random_bytes(32)); $this->query('DELETE FROM cg_auth_challenges WHERE expires_ms<?',[$this->now()]);
            $this->query('INSERT INTO cg_auth_challenges(nonce_hash,expires_ms) VALUES(?,?)',[hash('sha256',$nonce),$this->now()+300000]); return ['nonce'=>$nonce];
        }
        if ($method==='POST' && $route==='/v1/auth/google') {
            if (empty($this->config['google_client_id'])) throw new ApiError(503,'Google ile giriş henüz kurulmadı.');
            if (!is_string($data['idToken'] ?? null) || strlen($data['idToken'])>16384) throw new ApiError(401,'Google kimliği doğrulanamadı.');
            $google=new \Google\Client(['client_id'=>$this->config['google_client_id']]);
            $google->setHttpClient(new \GuzzleHttp\Client(['timeout'=>12,'connect_timeout'=>5,'verify'=>true]));
            try { $payload=$google->verifyIdToken($data['idToken']); } catch (\Throwable $e) { throw new ApiError(401,'Google kimliği doğrulanamadı.'); }
            if (!$payload || !($payload['email_verified'] ?? false)) throw new ApiError(401,'Google e-posta kimliği doğrulanamadı.');
            $nonce=$payload['nonce'] ?? ''; if (!is_string($nonce) || !preg_match('/^[a-f0-9]{64}$/D',$nonce)) throw new ApiError(401,'Google girişini yeniden başlat.');
            return $this->transaction(function() use($nonce,$payload) {
                $statement=$this->query('DELETE FROM cg_auth_challenges WHERE nonce_hash=? AND expires_ms>?',[hash('sha256',$nonce),$this->now()]); if ($statement->rowCount()!==1) throw new ApiError(401,'Google girişini yeniden başlat.');
                $email=Policy::email($payload['email'] ?? null);
                $this->query('INSERT INTO cg_users(id,email,verified,created_ms) VALUES(?,?,1,?) ON DUPLICATE KEY UPDATE verified=1',[$this->userId($email),$email,$this->now()]);
                return $this->session($this->query('SELECT * FROM cg_users WHERE email=?',[$email])->fetch());
            });
        }
        if ($method==='POST' && in_array($route,['/v1/auth/register','/v1/auth/login','/v1/auth/reset','/v1/auth/resend','/v1/auth/delete-request'],true)) {
            $email=Policy::email($data['email'] ?? null); $user=$this->query('SELECT * FROM cg_users WHERE email=?',[$email])->fetch();
            if ($route==='/v1/auth/register') {
                $password=Policy::password($data['password'] ?? null);
                if (empty($this->config['smtp']['host'])) throw new ApiError(503,'E-posta servisi henüz kurulmadı.');
                if (!$user) { $this->query('INSERT INTO cg_users(id,email,password_hash,created_ms) VALUES(?,?,?,?)',[$this->userId($email),$email,password_hash(Policy::passwordMaterial($password),PASSWORD_DEFAULT),$this->now()]); $user=$this->query('SELECT * FROM cg_users WHERE email=?',[$email])->fetch(); $this->emailToken($user,'verify'); }
                return ['message'=>'Adres uygunsa doğrulama e-postası gönderildi. Doğruladıktan sonra giriş yap.'];
            }
            if ($route!=='/v1/auth/login') {
                if (empty($this->config['smtp']['host'])) throw new ApiError(503,'E-posta servisi henüz kurulmadı.');
                if ($user && ($route!=='/v1/auth/resend' || !$user['verified'])) $this->emailToken($user,match($route){'/v1/auth/reset'=>'reset','/v1/auth/delete-request'=>'delete',default=>'verify'});
                return ['message'=>'Adres uygunsa e-posta gönderildi.'];
            }
            $password=$data['password'] ?? null;
            $dummy='$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi';
            $valid=is_string($password) && mb_strlen($password)<=128 && password_verify(Policy::passwordMaterial($password),$user['password_hash'] ?? $dummy);
            if (!$user || !$user['password_hash'] || !$valid) throw new ApiError(401,'E-posta veya şifre hatalı.');
            if (!$user['verified']) throw new ApiError(403,'Önce e-posta adresini doğrula.'); return $this->session($user);
        }
        $user=$this->auth($token);
        if ($method==='GET' && $route==='/v1/entitlement') return $this->status($user);
        if ($method==='POST' && $route==='/v1/trial') { $this->query('INSERT INTO cg_trials(user_id,started_ms) VALUES(?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id)',[$user['id'],$this->now()]); return $this->status($user); }
        if ($method==='POST' && $route==='/v1/purchase') {
            if (!is_string($data['token'] ?? null)) throw new ApiError(400,'Geçersiz satın alma.');
            $this->play($data['token'],$user,true); $tokenHash=hash('sha256',$data['token']);
            $this->transaction(function() use($user,$tokenHash,$data) {
                $this->query('SELECT id FROM cg_users WHERE id=? FOR UPDATE',[$user['id']]);
                $existing=$this->query('SELECT user_id FROM cg_purchases WHERE token_hash=? FOR UPDATE',[$tokenHash])->fetch();
                if ($existing && $existing['user_id']!==$user['id']) throw new ApiError(409,'Satın alma başka hesaba bağlı.');
                $this->query('INSERT INTO cg_purchases(token_hash,user_id,token_encrypted,updated_ms) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE updated_ms=VALUES(updated_ms)',[$tokenHash,$user['id'],$this->encrypt($data['token']),$this->now()]);
                $this->query('UPDATE cg_users SET pro=1 WHERE id=?',[$user['id']]);
            });
            $user['pro']=1; return $this->status($user);
        }
        if ($method==='GET' && $route==='/v1/backup') {
            $backup=$this->query('SELECT revision,data,updated_ms FROM cg_backups WHERE user_id=?',[$user['id']])->fetch();
            return !$backup ? ['revision'=>0,'backup'=>null] : ['revision'=>(int)$backup['revision'],'updatedAt'=>(int)$backup['updated_ms'],'backup'=>json_decode($this->decrypt($backup['data']),false,512,JSON_THROW_ON_ERROR)];
        }
        if ($method==='PUT' && $route==='/v1/backup') {
            $backup=Policy::backup($data['backup'] ?? null);
            return $this->transaction(function() use($user,$data,$backup) {
                $this->query('SELECT id FROM cg_users WHERE id=? FOR UPDATE',[$user['id']]);
                $old=$this->query('SELECT revision FROM cg_backups WHERE user_id=?',[$user['id']])->fetch(); $revision=$old ? (int)$old['revision'] : 0; Policy::revision($data['revision'] ?? null,$revision);
                $now=$this->now(); $this->query('INSERT INTO cg_backups(user_id,revision,data,updated_ms) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE revision=VALUES(revision),data=VALUES(data),updated_ms=VALUES(updated_ms)',[$user['id'],$revision+1,$this->encrypt(json_encode($backup,JSON_UNESCAPED_UNICODE|JSON_THROW_ON_ERROR)),$now]); return ['revision'=>$revision+1,'updatedAt'=>$now];
            });
        }
        if ($method==='POST' && $route==='/v1/auth/logout') { $this->query('DELETE FROM cg_sessions WHERE token_hash=?',[hash('sha256',$token)]); return ['ok'=>true]; }
        if ($method==='DELETE' && $route==='/v1/account') { $this->query('DELETE FROM cg_users WHERE id=?',[$user['id']]); return ['ok'=>true]; }
        throw new ApiError(404,'İşlem bulunamadı.');
    }
}
