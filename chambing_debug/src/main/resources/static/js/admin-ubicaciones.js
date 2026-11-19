'use strict';
window.UbicacionesAdmin = (() => {
  function secciones() {
    return {
      departamentos:{title:'Departamentos',sub:'Administra los departamentos disponibles en las direcciones.',endpoint:'/admin/departamentos',search:'Buscar departamento…',headers:['Departamento','Estado',''],
        row:(d,id)=>`<tr><td class="cell-title">${h(d.nombre)}</td><td>${pillActivo(d.activo)}</td>${actions('editarDepartamento','eliminarDepartamento',id)}</tr>`,empty:'No hay departamentos registrados.',icon:'bi-map',onNew:()=>abrir('departamentos')},
      ciudades:{title:'Ciudades',sub:'Cada ciudad pertenece a un departamento. Puedes cambiar esa relación al editarla.',endpoint:'/admin/ciudades',search:'Buscar ciudad…',headers:['Ciudad','Departamento','Estado',''],
        row:(c,id)=>`<tr><td class="cell-title">${h(c.nombre)}</td><td>${h(c.departamentoNombre)}</td><td>${pillActivo(c.activo)}</td>${actions('editarCiudad','eliminarCiudad',id)}</tr>`,empty:'No hay ciudades registradas.',icon:'bi-buildings',onNew:()=>abrir('ciudades')}
    };
  }
  let modal, tipo, id, apertura=0;
  function crearModal() {
    if(modal)return;
    modal=document.createElement('div');modal.className='modal fade';modal.id='modalUbicacion';modal.tabIndex=-1;
    modal.setAttribute('aria-labelledby','ubicacionTitulo');
    modal.innerHTML=`<div class="modal-dialog"><form class="modal-content"><div class="modal-header"><h5 class="modal-title" id="ubicacionTitulo"></h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button></div><div class="modal-body">
      <label class="form-label" for="ubicacionNombre">Nombre</label><input class="form-control" id="ubicacionNombre" maxlength="100" required>
      <div class="mt-3" data-departamento><label class="form-label" for="ubicacionDepartamento">Departamento al que pertenece</label><select class="form-select" id="ubicacionDepartamento"></select></div>
      <label class="form-label mt-3" for="ubicacionActivo">Estado</label><select class="form-select" id="ubicacionActivo"><option value="true">Activo</option><option value="false">Inactivo</option></select>
      <p class="small text-muted mt-3">Las ubicaciones inactivas conservan las direcciones existentes y dejan de ofrecerse en nuevos registros.</p><p role="status" class="text-danger" data-error></p></div><div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button><button class="btn btn-primary" type="submit">Guardar</button></div></form></div>`;
    document.body.append(modal);
    modal.querySelector('form').onsubmit=guardar;
  }
  async function abrir(seccion, registroId) {
    crearModal(); tipo=seccion; id=registroId || null; const turno=++apertura;
    const item=id?findById(state[tipo].cache,id):null;
    if(id&&!item){toast('No se encontró el registro.',true);return;}
    modal.querySelector('h5').textContent=`${id?'Editar':'Nuevo'} ${tipo==='ciudades'?'ciudad':'departamento'}`;
    modal.querySelector('#ubicacionNombre').value=item?.nombre || '';
    modal.querySelector('#ubicacionActivo').value=String(item?.activo ?? true);
    modal.querySelector('[data-departamento]').hidden=tipo!=='ciudades';
    const selector=modal.querySelector('#ubicacionDepartamento');selector.required=tipo==='ciudades';
    const btn=modal.querySelector('[type="submit"]');btn.disabled=true;
    modal.querySelector('[data-error]').textContent='';
    bootstrap.Modal.getOrCreateInstance(modal).show();
    try{
      if(tipo==='ciudades'){
        selector.replaceChildren(new Option('Cargando departamentos…',''));
        const departamentos=await Ubicaciones.api('/admin/departamentos/opciones');
        if(turno!==apertura)return;
        selector.replaceChildren(new Option('Selecciona un departamento',''));
        for(const d of departamentos)selector.add(new Option(d.nombre+(d.activo?'':' (inactivo)'),d.id));
        selector.value=item?.departamentoId || '';
        if(!departamentos.length)throw new Error('Crea primero un departamento. Cierra este formulario y vuelve a abrirlo después.');
      }
      btn.disabled=false;
    }catch(error){modal.querySelector('[data-error]').textContent=error.message;}
  }
  async function guardar(e) {
    e.preventDefault();const btn=modal.querySelector('[type="submit"]');btn.disabled=true;
    try{
      const payload={nombre:modal.querySelector('#ubicacionNombre').value.trim(),activo:modal.querySelector('#ubicacionActivo').value==='true'};
      if(tipo==='ciudades')payload.departamentoId=modal.querySelector('#ubicacionDepartamento').value;
      await Ubicaciones.api(`/admin/${tipo}${id?'/'+encodeURIComponent(id):''}`,{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
      bootstrap.Modal.getInstance(modal).hide();toast('Ubicación guardada');
      // Invalidar ambas tablas: un cambio de departamento afecta la lista de ciudades.
      state.departamentos.loaded=false;state.ciudades.loaded=false;reload(tipo);
    }catch(error){modal.querySelector('[data-error]').textContent=error.message;}
    finally{btn.disabled=false;}
  }
  async function eliminar(seccion,registroId) {
    const item=findById(state[seccion].cache,registroId);
    if(!await confirmar('¿Eliminar ubicación?',`Se eliminará «${item?.nombre || 'esta ubicación'}». Si tiene relaciones activas, puedes desactivarla en su lugar.`))return;
    try{
      await Ubicaciones.api(`/admin/${seccion}/${encodeURIComponent(registroId)}`,{method:'DELETE'});
      const s=state[seccion];if(s.cache.length===1&&s.page>0)s.page--;
      state.departamentos.loaded=false;state.ciudades.loaded=false;reload(seccion);toast('Ubicación eliminada');
    }catch(error){toast(error.message,true);}
  }
  window.editarDepartamento=id=>abrir('departamentos',id);
  window.editarCiudad=id=>abrir('ciudades',id);
  window.eliminarDepartamento=id=>eliminar('departamentos',id);
  window.eliminarCiudad=id=>eliminar('ciudades',id);
  const campos={};
  async function cargarDireccion(prefijo,dir) {
    campos[prefijo] ||= Ubicaciones.crear(document.getElementById(`${prefijo}_departamento`),document.getElementById(`${prefijo}_ciudad`),{admin:true});
    await campos[prefijo].cargar(dir);
  }
  function direccion(prefijo) {return campos[prefijo].datos();}
  return {secciones,cargarDireccion,direccion};
})();
