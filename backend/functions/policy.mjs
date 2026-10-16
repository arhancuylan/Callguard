export const TRIAL_MS=7*24*60*60*1000;
export function entitlement(start,pro,now){return {pro:!!pro,trialEnds:start?Number(start)+TRIAL_MS:0,serverTime:now};}
export function validateBackup(value){
  if(!value||value.version!==1||!Array.isArray(value.contacts)||value.contacts.length>5000)throw new Error('Geçersiz yedek');
  const contacts=value.contacts.map(c=>{if(!c||typeof c.name!=='string'||c.name.length>200||!/^\+[1-9][0-9]{6,14}$/.test(c.number)||typeof c.white!=='boolean')throw new Error('Geçersiz kişi');return {name:c.name,number:c.number,white:c.white};});
  const settings={};for(const [key,v] of Object.entries(value.settings||{})){
    if(key==='profile'){if(!['custom','work','sleep','meeting'].includes(v))throw new Error('Geçersiz profil');settings[key]=v;continue;}
    const tail=key.replace(/^profile_(custom|work|sleep|meeting)_/,'');
    if(['rule','media','recordOnly'].includes(key)||/^profile_(custom|work|sleep|meeting)_(rule|media|scheduled|days|start|end|initialized)$/.test(key)){
      if(tail==='rule'&&!['ALL','WHITELIST','BLACKLIST'].includes(v))throw new Error('Geçersiz kural');
      if(tail==='media'&&!['VOICE','VIDEO','BOTH'].includes(v))throw new Error('Geçersiz tür');
      if(['recordOnly','scheduled','initialized'].includes(tail)&&typeof v!=='boolean')throw new Error('Geçersiz ayar');
      if(['days','start','end'].includes(tail)&&(!Number.isInteger(v)||v<0||v>(tail==='days'?127:1439)))throw new Error('Geçersiz saat');settings[key]=v;
    }
  }
  // Neither purchase/trial claims nor device permissions can enter through a backup.
  const history=(value.history||[]);if(!Array.isArray(history)||history.length>500)throw new Error('Geçersiz geçmiş');
  const cleanHistory=history.map(h=>{if(typeof h.caller!=='string'||h.caller.length>200||typeof h.result!=='string'||h.result.length>200||typeof h.video!=='boolean'||!Number.isSafeInteger(h.at)||h.at<0||typeof h.number!=='string'||(h.number!==''&&!/^\+[1-9][0-9]{6,14}$/.test(h.number)))throw new Error('Geçersiz geçmiş');return {caller:h.caller,result:h.result,video:h.video,at:h.at,number:h.number};});
  return {version:1,contacts,settings,history:cleanHistory};
}
export function requireRevision(expected,actual){if(!Number.isSafeInteger(expected)||expected!==actual){const error=new Error('Bulut yedeği başka cihazda değişmiş. Önce buluttan geri yükle.');error.status=409;throw error;}}
