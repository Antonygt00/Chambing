'use strict';
(() => {
  const C = window.Chambing;
  const oferta = new URLSearchParams(location.search).get('idPropuesta');
  const contenedor = document.getElementById('postulaciones-body');
  let empresa, postulaciones = [], filtro = '', resultado = '', busqueda = '', porProbabilidad = false;
  const barra = document.createElement('div'); barra.className = 'seleccion-barra';
  barra.innerHTML = `<label>Fase <select class="form-control" data-fase><option value="">Todas las fases</option>${C.fases.map(([v,n]) => `<option value="${v}">${n}</option>`).join('')}</select></label>
    <label>Resultado <select class="form-control" data-resultado><option value="">Todos</option><option value="Pendiente">En proceso</option><option value="Aprobado">Seleccionado</option><option value="Rechazado">Rechazado</option></select></label>
    <label>Buscar candidato <input class="form-control" type="search" placeholder="Nombre o correo" data-busqueda></label>
    <button type="button" class="btn btn-outline-warning" data-probabilidad>Ordenar por probabilidad</button>
    <button type="button" class="btn btn-outline-primary" data-recargar>Actualizar</button><span role="status" data-conteo></span>`;
  contenedor.before(barra);
  barra.querySelector('[data-fase]').onchange = e => { filtro = e.target.value; render(); };
  barra.querySelector('[data-resultado]').onchange = e => { resultado = e.target.value; render(); };
  barra.querySelector('[data-busqueda]').oninput = e => { busqueda = e.target.value.toLowerCase().trim(); render(); };
  barra.querySelector('[data-recargar]').onclick = () => cargar();
  barra.querySelector('[data-probabilidad]').onclick = () => { porProbabilidad = !porProbabilidad; cargar(); };
  async function cargar() {
    const btn = barra.querySelector('[data-recargar]'); btn.disabled = true;
    try {
      if (!oferta) throw new Error('Abre una oferta desde Mis Propuestas para gestionar sus candidatos.');
      if (!empresa) empresa = await C.api('/empresa/me');
      document.getElementById('nombreNav').textContent = empresa.nombreEmpresa;
      postulaciones = await C.api(`/empresa/${encodeURIComponent(empresa.idEmpresa)}/postulaciones/${encodeURIComponent(oferta)}${porProbabilidad ? '?ordenar=probabilidad' : ''}`);
      render(); C.destacar();
    } catch(error) { contenedor.textContent = error.message; }
    finally { btn.disabled = false; }
  }
  function render() {
    const lista = postulaciones.filter(p => (!filtro || p.faseActual === filtro)
      && (!resultado || p.estadoPostulacion === resultado)
      && (!busqueda || `${p.usuarioNombre} ${p.usuarioEmail}`.toLowerCase().includes(busqueda)));
    barra.querySelector('[data-conteo]').textContent = `${lista.length} de ${postulaciones.length} candidatos`;
    contenedor.replaceChildren();
    if (!lista.length) { contenedor.textContent = 'No hay postulantes con estos filtros.'; return; }
    for (const p of lista) {
      const card = document.createElement('article'); card.className = 'seleccion-candidato'; card.dataset.postulacion = p.idPostulacion;
      card.innerHTML = `<div class="seleccion-cabecera"><div><h2>${C.h(p.usuarioNombre)}</h2><p>${C.h(p.usuarioEmail)}<br><small>${C.h(p.propuestaTitulo)} · Inscripción: ${C.h(C.fecha(p.fechaPostulacion))}</small>
        ${p.etiqueta ? `<br><span class="badge badge-${p.etiqueta === 'Alta' ? 'success' : p.etiqueta === 'Media' ? 'warning' : 'secondary'}">Probabilidad ${C.h(p.etiqueta)} · ${Math.round(Number(p.probabilidad || 0) * 100)}%</span>` : ''}</p></div>${C.resultado(p)}</div>
        ${C.progreso(p)}${C.documentos(p)}${C.historial(p,true)}
        <form class="seleccion-form"><h3>Actualizar proceso</h3>
          <label>Fase<select class="form-control" name="fase">${C.fases.map(([v,n]) => `<option value="${v}" ${p.faseActual === v ? 'selected' : ''}>${n}</option>`).join('')}</select></label>
          <label>Resultado<select class="form-control" name="resultado"><option value="Pendiente">En proceso</option><option value="Aprobado">Seleccionado</option><option value="Rechazado">Rechazado</option></select></label>
          <label>Observación para el candidato<textarea class="form-control" name="observacion" maxlength="2000" placeholder="Explica el avance o indica los documentos pendientes"></textarea></label>
          <small>La observación es obligatoria para rechazar, retroceder o reabrir un proceso.</small>
          <label class="mt-3">Nota interna de la empresa<textarea class="form-control" name="notaInterna" maxlength="2000" placeholder="Solo la empresa puede ver esta nota"></textarea></label>
          <button class="btn btn-primary" type="submit">Guardar cambio</button><p role="status" class="seleccion-mensaje"></p>
        </form>`;
      const form = card.querySelector('form'); form.elements.resultado.value = p.estadoPostulacion;
      form.elements.fase.onchange = e => {
        form.elements.resultado.value = e.target.value === 'SELECCIONADO' ? 'Aprobado' : 'Pendiente';
      };
      form.elements.resultado.onchange = e => {
        if (e.target.value === 'Aprobado') form.elements.fase.value = 'SELECCIONADO';
        else if (e.target.value === 'Pendiente' && form.elements.fase.value === 'SELECCIONADO')
          form.elements.fase.value = 'POTENCIAL_CANDIDATO';
      };
      form.onsubmit = async e => {
        e.preventDefault();
        const btn = form.querySelector('button'); const msg = form.querySelector('[role="status"]');
        btn.disabled = true; msg.className = 'seleccion-mensaje'; msg.textContent = 'Guardando…';
        try {
          const datos = Object.fromEntries(new FormData(form)); datos.version = p.version;
          const actualizado = await C.put(`/empresa/${encodeURIComponent(empresa.idEmpresa)}/postulaciones/${encodeURIComponent(p.idPostulacion)}/fase`,datos);
          const cambioReal = actualizado.version !== p.version;
          postulaciones = postulaciones.map(item => item.idPostulacion === p.idPostulacion ? actualizado : item);
          render();
          const nueva = [...contenedor.children].find(el => el.dataset.postulacion === p.idPostulacion);
          if (nueva) { nueva.querySelector('[role="status"]').textContent = cambioReal ? 'Cambio guardado. El candidato recibirá una notificación.' : 'La fase y el resultado ya estaban guardados. No se envió otro aviso.'; nueva.querySelector('button').focus(); }
          else barra.querySelector('[data-conteo]').textContent += ' · Cambio guardado';
        } catch(error) { msg.classList.add('text-danger'); msg.textContent = error.message; }
        finally { btn.disabled = false; }
      };
      contenedor.append(card);
    }
  }
  cargar();
})();
