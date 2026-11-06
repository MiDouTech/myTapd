// Real HTTP + disposable MySQL verification. No API mocks. See docs/MTS-3-delivery.md.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const crypto = require('node:crypto');
const {execFileSync} = require('node:child_process');
const env = process.env;
const db = env.MTS3_TEST_DATABASE;
assert.match(db || '', /^mts3_[a-z0-9_]+$/);
const base = env.MTS3_TEST_BASE || 'http://127.0.0.1:18080';
assert.equal(new URL(base).hostname, '127.0.0.1', 'Only isolated local services are supported');
assert.ok(env.MTS3_TEST_JWT_SECRET, 'Set the local backend signing secret');
function sql(query) {
  return execFileSync(env.MTS3_MYSQL_BIN || 'mysql', ['--no-defaults','--host=127.0.0.1',
    '--port=' + (env.MTS3_MYSQL_PORT || '13306'), '--user=' + (env.MYSQL_TEST_USER || 'root'),
    '--default-character-set=utf8mb4', '--batch','--skip-column-names', db], {input:query, encoding:'utf8'}).trim();
}
function token(id) {
  const enc = v => Buffer.from(JSON.stringify(v)).toString('base64url');
  const s = enc({alg:'HS256',typ:'JWT'}) + '.' + enc({sub:String(id),userId:id,username:'integration',roles:['ADMIN'],type:'access',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600});
  return s + '.' + crypto.createHmac('sha256',env.MTS3_TEST_JWT_SECRET).update(s).digest('base64url');
}
const observations=[];
async function request(path, id, body, extra={}) {
  const response=await fetch(base+path,{method:body?'POST':'GET',headers:{...(id?{Authorization:'Bearer '+token(id)}:{}),'Content-Type':'application/json',...extra},body:body?JSON.stringify(body):undefined});
  const text=await response.text();
  let json; try {json=JSON.parse(text);} catch {throw new Error(path+' non-JSON status '+response.status);}
  observations.push({path,actor:id||'anonymous/API-key',status:response.status,code:json.code});
  return json;
}
const ticket=991003, employee=991001, external=991002, disabled=991004;
const detail='/api/ticket/detail/'+ticket, comment='/api/ticket/'+ticket+'/comment';
const publicPath='/api/open/ticket/MTS3-INTEGRATION';
async function main() {
  sql(`INSERT INTO sys_user(id,name,wecom_userid,account_status) VALUES
    (${employee},'Integration Employee','mts3-employee',1),(${external},'Integration External',NULL,1),(${disabled},'Integration Disabled','mts3-disabled',2)
    ON DUPLICATE KEY UPDATE account_status=VALUES(account_status),wecom_userid=VALUES(wecom_userid);
    INSERT INTO ticket(id,ticket_no,title,category_id,creator_id,source) VALUES(${ticket},'MTS3-INTEGRATION','Comment integration',1,${external},'WEB')
    ON DUPLICATE KEY UPDATE title=VALUES(title);
    DELETE FROM ticket_comment WHERE ticket_id=${ticket};`);
  const image='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAADUlEQVR4AWJigAI4AwAAAP//Y0HangAAAAZJREFUAwAASAAFuIFDmQAAAABJRU5ErkJggg==';
  const secret='MTS3_PRIVATE_'+crypto.randomBytes(8).toString('hex');
  const rich={visibility:'INTERNAL',contentFormat:'HTML',content:`<p onclick="bad()"><strong>${secret}</strong></p><table><tr><td>cell</td></tr></table><img src="${image}" onerror="bad()"><script>bad()</script><a href="javascript:bad()">link</a>`};
  assert.equal((await request(comment,employee,rich)).code,200);
  const inside=await request(detail,employee); assert.equal(inside.code,200);
  const note=inside.data.comments.find(c=>c.content.includes(secret)); assert.ok(note);
  assert.ok(note.content.includes(image)); assert.ok(note.content.includes('<strong>')); assert.ok(note.content.includes('<table>'));
  assert.doesNotMatch(note.content,/onerror|onclick|javascript:|<script/);
  assert.equal(sql(`SELECT COUNT(*) FROM ticket_attachment WHERE ticket_id=${ticket}`),'0','Private images must not create public attachment rows');
  for(const actor of [external,disabled,null]) {
    const result=await request(detail,actor);
    assert.ok(!JSON.stringify(result).includes(secret)); assert.ok(!JSON.stringify(result).includes(image));
    const write=await request(comment,actor,rich); assert.notEqual(write.code,200);
  }
  const publicly=await request(publicPath,null); assert.equal(publicly.code,200);
  assert.ok(!JSON.stringify(publicly).includes(secret)); assert.ok(!JSON.stringify(publicly).includes(image));
  for(const url of ['https://example.test/private.png','data:image/svg+xml;base64,PHN2Zz4=','data:image/png;base64,eA==']) {
    assert.notEqual((await request(comment,employee,{...rich,content:'<p>invalid</p><img src="'+url+'">'})).code,200);
  }
  assert.equal((await request(comment,employee,{visibility:'PUBLIC',contentFormat:'HTML',content:'<p><b>MTS3_PUBLIC</b><img src="https://example.test/public.png" onerror="bad()"></p><script>bad()</script>'})).code,200);
  const published=await request(publicPath,null);
  assert.ok(JSON.stringify(published).includes('MTS3_PUBLIC')); assert.doesNotMatch(JSON.stringify(published),/onerror|<script/);
  assert.equal((await request(comment,employee,{content:'<img src=x onerror=bad()>MTS3_PLAIN'})).code,200);
  const plain=(await request(detail,employee)).data.comments.find(c=>c.content.includes('MTS3_PLAIN'));
  assert.equal(plain.contentFormat,'PLAIN_TEXT');
  const key=await request('/api/user/api-key/create',employee,{name:'MTS3 integration'}); assert.equal(key.code,200);
  const keyValue=key.data.apiKey;
  assert.ok(keyValue,'API key response contract');
  assert.equal((await request(comment,null,{visibility:'PUBLIC',content:'MTS3_AGENT'},{'X-Api-Key':keyValue})).code,200);
  assert.equal((await request(detail,employee)).data.comments.find(c=>c.content==='MTS3_AGENT').visibility,'INTERNAL');
  // Existing JWT must lose internal access immediately after binding is revoked.
  sql(`UPDATE sys_user SET wecom_userid=NULL WHERE id=${employee}`);
  assert.ok(!JSON.stringify(await request(detail,employee)).includes(secret));
  assert.notEqual((await request(comment,employee,rich)).code,200);
  sql(`UPDATE sys_user SET wecom_userid='mts3-employee',account_status=2 WHERE id=${employee}`);
  assert.notEqual((await request(detail,employee)).code,200);
  sql(`UPDATE sys_user SET account_status=1 WHERE id=${employee}`);
  assert.equal(sql(`SELECT COUNT(*) FROM sys_operation_log WHERE request_path='${comment}' AND request_params LIKE '%MTS3_PRIVATE_%'`),'0');
  const appSecret=crypto.randomBytes(24).toString('hex');
  sql(`INSERT INTO integration_app(id,app_name,app_key,app_secret,system_code,default_category_id,permissions)
    VALUES(991003,'MTS3 test','mts3-test','${appSecret}','mts3-test',1,'plugin:launch-token,plugin:ticket:create,plugin:ticket:read-mine')
    ON DUPLICATE KEY UPDATE app_secret=VALUES(app_secret);`);
  async function launch(externalUserId) {
    const path='/api/open/v1/plugin/launch-token', timestamp=String(Math.floor(Date.now()/1000)), nonce=crypto.randomBytes(16).toString('hex');
    const canonical=['POST',path,'',timestamp,nonce,'mts3-test'].join('\n');
    const result=await request(path,null,{externalUserId,userName:'MTS3 customer'},{'X-App-Key':'mts3-test','X-Timestamp':timestamp,'X-Nonce':nonce,'X-Signature':crypto.createHmac('sha256',appSecret).update(canonical).digest('hex')});
    assert.equal(result.code,200);return result.data.launchToken;
  }
  const launchToken=await launch('owner');
  const claims=JSON.parse(Buffer.from(launchToken.split('.')[1],'base64url').toString());
  sql(`UPDATE ticket SET integration_app_id=991003,creator_id=${Number(claims.userId)} WHERE id=${ticket}`);
  const pluginPath='/api/open/v1/plugin/tickets/MTS3-INTEGRATION';
  const pluginHeaders={Authorization:'Bearer '+launchToken};
  const plugin=await request(pluginPath+'/detail',null,null,pluginHeaders);assert.equal(plugin.code,200);
  assert.ok(!JSON.stringify(plugin).includes(secret));assert.ok(!JSON.stringify(plugin).includes(image));
  assert.equal((await request(pluginPath+'/messages',null,{content:'MTS3_PLUGIN',videos:['https://example.test/video.mp4']},pluginHeaders)).code,200);
  const pluginReply=(await request(publicPath,null)).data.comments.find(c=>c.content.includes('MTS3_PLUGIN'));
  assert.ok(pluginReply.content.includes('<video'));
  const otherToken=await launch('other');
  assert.notEqual((await request(pluginPath+'/detail',null,null,{Authorization:'Bearer '+otherToken})).code,200);
  const result={passed:true,database:db,backend:base,checks:['bound enabled employee read/write','unbound external cannot read/write internal','disabled and anonymous denied','real image bytes persisted and read only in authorized detail','no private attachment/CDN URL','external/SVG/invalid image rejected','private/public HTML sanitized','public reply visible','legacy plain text format retained','API key forced internal','binding/status revocation effective with existing JWT','comment content absent from operation log'],observations};
  result.checks.push('real signed plugin launch and public-only detail','plugin public reply preserves video','other plugin owner denied');
  fs.writeFileSync(env.MTS3_RESULT_FILE||'comment-http-result.json',JSON.stringify(result,null,2));
  console.log(JSON.stringify({passed:true,checks:result.checks.length,requests:observations.length}));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
