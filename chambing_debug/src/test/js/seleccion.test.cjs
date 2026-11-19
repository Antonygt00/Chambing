const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const resources = path.join(__dirname,'../../main/resources');
function helpers(fetch) {
  const context = {window:{},fetch,Date,JSON,Error,encodeURIComponent,document:{querySelectorAll:()=>[]},location:{search:''},URLSearchParams};
  vm.runInNewContext(fs.readFileSync(path.join(resources,'static/js/seleccion.js'),'utf8'),context);
  return context.window.Chambing;
}
test('los textos del candidato, historial y archivos no pueden inyectar HTML',()=>{
  const C = helpers();
  const p={idPostulacion:'p',historial:[{fase:'INSCRIPCION',resultado:'Pendiente',observacion:'<img src=x onerror=alert(1)>',notaInterna:'Secreto'}],documentos:[{id:'a/b',nombre:'<script>x</script>',tamano:10}]};
  assert.ok(C.historial(p,false).includes('&lt;img'));
  assert.ok(!C.historial(p,false).includes('<img'));
  assert.ok(!C.historial(p,false).includes('Secreto'));
  assert.ok(C.historial(p,true).includes('Secreto'));
  assert.ok(C.documentos(p).includes('&lt;script&gt;'));
  assert.ok(C.documentos(p).includes('a%2Fb'));
});
test('el seguimiento identifica la fase actual sin ocultar las otras fases',()=>{
  const C=helpers(); const html=C.progreso({faseActual:'POTENCIAL_CANDIDATO'});
  assert.equal((html.match(/<li /g)||[]).length,4);
  assert.equal((html.match(/aria-current="step"/g)||[]).length,1);
  assert.ok(html.includes('Potencial candidato'));
});
test('un rechazo mantiene visible el resultado junto a la fase',()=>{
  const C=helpers(); assert.ok(C.resultado({estadoPostulacion:'Rechazado'}).includes('Rechazado'));
  assert.ok(C.resultado({estadoPostulacion:'Aprobado'}).includes('Seleccionado'));
});
test('el cliente muestra el conflicto de versión del backend',async()=>{
  const C=helpers(async()=>({ok:false,status:409,headers:new Map([['content-type','application/json']]),text:async()=>'{"message":"Recarga antes de continuar"}'}));
  await assert.rejects(C.api('/api/postulaciones/p'),/Recarga antes de continuar/);
});
test('una redirección o HTML de login se presenta como sesión vencida',async()=>{
  const C=helpers(async()=>({redirected:true,headers:new Map(),text:async()=>'<html>Login</html>'}));
  await assert.rejects(C.api('/api/notificaciones'),/sesión venció/);
});
test('marcar una notificación leída acepta cuerpo vacío',async()=>{
  const C=helpers(async(url,options)=>{
    assert.equal(options.method,'PUT');
    return {ok:true,headers:new Map(),text:async()=>''};
  });
  assert.equal(await C.put('/api/notificaciones/n/leida',{}),null);
});
test('todo el JavaScript local e inline mantiene sintaxis válida',()=>{
  for(const file of fs.readdirSync(path.join(resources,'static/js')))
    if(file.endsWith('.js')) new vm.Script(fs.readFileSync(path.join(resources,'static/js',file),'utf8'),{filename:file});
  for(const file of fs.readdirSync(path.join(resources,'templates'))){
    if(!file.endsWith('.html'))continue;
    const html=fs.readFileSync(path.join(resources,'templates',file),'utf8');
    for(const match of html.matchAll(/<script\b[^>]*>([^]*?)<\/script>/g))
      if(match[1].trim())new vm.Script(match[1],{filename:file});
  }
});
