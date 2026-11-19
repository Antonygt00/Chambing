const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
class Element {
  constructor(){this.options=[];this.listeners={};this.classList={toggle(){}};this.dataset={};this._value='';}
  replaceChildren(...items){this.options=items;this._value='';}
  append(...items){this.options.push(...items);}
  after(){}
  setAttribute(){}
  addEventListener(event,handler){this.listeners[event]=handler;}
  get value(){return this.options.find(o=>o.selected)?.value || this._value;}
  set value(v){this._value=v;this.options.forEach(o=>o.selected=o.value===v);}
  get selectedOptions(){return this.options.filter(o=>o.value===this.value);}
}
function setup(responder){
  const ctx={window:{},document:{createElement:()=>new Element()},Option:function(text,value){return {text,value,dataset:{}};},fetch:async(url,opts)=>{
    const data=await responder(url,opts);
    return {ok:true,headers:new Map([['content-type','application/json']]),text:async()=>JSON.stringify(data)};
  },URLSearchParams,encodeURIComponent,Error,JSON};
  vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/ubicaciones.js'),'utf8'),ctx);
  const dep=new Element(),ciudad=new Element();
  return {dep,ciudad,selector:ctx.window.Ubicaciones.crear(dep,ciudad),Ubicaciones:ctx.window.Ubicaciones};
}
const departamentos=[{id:'d1',nombre:'Bolívar',activo:true},{id:'d2',nombre:'Atlántico',activo:true}];
test('restaura la dirección guardada mediante IDs y solo solicita las ciudades de ese departamento',async()=>{
  const urls=[];
  const {selector,dep,ciudad}=setup(url=>{urls.push(url);return url.includes('/ciudades')?[{id:'c1',nombre:'Cartagena',activo:true}]:departamentos;});
  await selector.cargar({departamentoId:'d1',ciudadId:'c1'});
  assert.equal(dep.value,'d1');assert.equal(ciudad.value,'c1');
  assert.ok(urls.includes('/api/catalogos/ciudades?departamentoId=d1'));
  assert.equal(selector.datos().departamento,'Bolívar');assert.equal(selector.datos().ciudad,'Cartagena');
});
test('cambiar departamento borra la ciudad anterior y exige elegir una ciudad válida',async()=>{
  const {selector,dep,ciudad}=setup(url=>url.includes('/ciudades')?[{id:url.includes('d1')?'c1':'c2',nombre:url.includes('d1')?'Cartagena':'Barranquilla',activo:true}]:departamentos);
  await selector.cargar({departamentoId:'d1',ciudadId:'c1'});
  dep.value='d2';await dep.listeners.change();
  assert.equal(ciudad.value,'');assert.ok(!ciudad.options.some(o=>o.value==='c1'));
  assert.throws(()=>selector.datos(),/Selecciona un departamento y una ciudad/);
  ciudad.value='c2';assert.equal(selector.datos().departamentoId,'d2');
});
test('el catálogo conserva una ubicación guardada desactivada sin ofrecerla como opción nueva',async()=>{
  const {selector,dep}=setup(()=>[]);
  await selector.cargar({departamentoId:'d1',departamento:'Bolívar',ciudadId:'c1',ciudad:'Cartagena'});
  assert.equal(selector.datos().ciudadId,'c1');assert.equal(dep.selectedOptions[0].disabled,true);
});
test('un fallo de carga impide guardar silenciosamente una dirección vacía',async()=>{
  const {selector}=setup(()=>{throw new Error('Servidor no disponible');});
  await selector.cargar();assert.throws(()=>selector.datos(),/Servidor no disponible/);
});
test('una respuesta antigua de ciudades no pisa el departamento elegido después',async()=>{
  let resolver;
  const {selector,dep,ciudad}=setup(url=>{
    if(!url.includes('/ciudades'))return departamentos;
    if(url.includes('d1'))return new Promise(resolve=>resolver=resolve);
    return [{id:'c2',nombre:'Barranquilla',activo:true}];
  });
  await selector.cargar();dep.value='d1';const antiguo=dep.listeners.change();
  dep.value='d2';await dep.listeners.change();resolver([{id:'c1',nombre:'Cartagena',activo:true}]);await antiguo;
  assert.ok(ciudad.options.some(o=>o.value==='c2'));assert.ok(!ciudad.options.some(o=>o.value==='c1'));
});
