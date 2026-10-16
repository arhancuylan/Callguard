<?php
declare(strict_types=1);
require dirname(__DIR__).'/src/Policy.php';
use CallGuard\Policy; use CallGuard\ApiError;
function check(bool $condition,string $message): void { if (!$condition) throw new RuntimeException($message); }
function rejects(callable $fn,int $code): void { try {$fn();}catch(ApiError $e){check($e->status===$code,'Wrong status');return;}throw new RuntimeException('Request should have been rejected'); }
check(Policy::route('/callguard/v1/backup?x=1','https://mktech.net.tr/callguard')==='/v1/backup','Directory route');
check(Policy::route('/v1/backup','https://callguard.mktech.net.tr')==='/v1/backup','Subdomain route');
rejects(fn()=>Policy::route('/mktech/v1/backup','https://mktech.net.tr/callguard'),404);
rejects(fn()=>Policy::route('/callguard-other/v1/backup','https://mktech.net.tr/callguard'),404);
check(Policy::entitlement(100,false,200)['trialEnds']===100+Policy::TRIAL_MS,'Seven days');
check(Policy::entitlement(null,false,200)['trialEnds']===0,'No invented trial');
$backup=Policy::backup(['version'=>1,'contacts'=>[['name'=>'Ayşe','number'=>'+905551112233','white'=>true]],'settings'=>['rule'=>'ALL','pro'=>true,'trialEnds'=>99999999,'enabled'=>true,'profile_sleep_start'=>1320],'history'=>[]]);
check((array)$backup['settings']===['rule'=>'ALL','profile_sleep_start'=>1320],'Backup must not grant rights or permissions');
check(json_encode(Policy::backup(['version'=>1,'contacts'=>[],'settings'=>[]])['settings'])==='{}','Empty settings must remain an object for Android');
rejects(fn()=>Policy::backup(['version'=>1,'contacts'=>[],'settings'=>['profile_work_start'=>2000]]),400);
rejects(fn()=>Policy::backup(['version'=>1,'contacts'=>[['name'=>'X','number'=>'private','white'=>true]],'settings'=>[]]),400);
rejects(fn()=>Policy::backup(['version'=>1,'contacts'=>array_fill(0,2,['name'=>'X','number'=>'+905551112233','white'=>true]),'settings'=>[]]),400);
rejects(fn()=>Policy::revision(0,1),409); Policy::revision(1,1);
$first=str_repeat('a',72).'one'; $second=str_repeat('a',72).'two';
$hash=password_hash(Policy::passwordMaterial($first),PASSWORD_DEFAULT);
check(!password_verify(Policy::passwordMaterial($second),$hash),'Long passwords must not truncate to the same bcrypt input');
echo "PASS: path isolation, trial boundaries, backup validation, rights exclusion, revisions and password handling\n";
