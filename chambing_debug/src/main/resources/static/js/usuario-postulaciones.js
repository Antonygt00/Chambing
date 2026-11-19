'use strict';
(() => {
  const C = window.Chambing;
  const contenedor = document.getElementById('postulaciones-body');
  let usuario, postulaciones = [], filtro = '', resultado = '';
  const barra = document.createElement('div'); barra.className = 'seleccion-barra';
  barra.innerHTML = `<label>Fase <select class="form-control" data-fase><option value="">Todas las fases</option>${C.fases.map(([v,n]) => `<option value="${v}">${n}</option>`).join('')}</select></label>
    <label>Resultado <select class="form-control" data-resultado><option value="">Todos</option><option value="Pendiente">En proceso</option><option value="Aprobado">Seleccionado</option><option value="Rechazado">Rechazado</option></select></label>
    <button class="btn btn-outline-primary" type="button" data-recargar>Actualizar seguimiento</button><span data-conteo role="status"></span>`;
  contenedor.before(barra);
  barra.querySelector('[data-fase]').onchange = e => { filtro = e.target.value; render(); };
  barra.querySelector('[data-resultado]').onchange = e => { resultado = e.target.value; render(); };
  barra.querySelector('[data-recargar]').onclick = () => cargar();
  async function cargar() {
    const btn = barra.querySelector('button'); btn.disabled = true;
    try {
      if (!usuario) usuario = await C.api('/usuario/me');
      document.getElementById('nombreNav').textContent = `${usuario.nombre || ''} ${usuario.apellido || ''}`.trim();
      postulaciones = await C.api(`/usuario/postulaciones/${encodeURIComponent(usuario.idUsuario)}`);
      postulaciones.sort((a,b) => new Date(b.fechaPostulacion) - new Date(a.fechaPostulacion));
      render(); C.destacar();
    } catch(error) { contenedor.textContent = error.message; }
    finally { btn.disabled = false; }
  }
  function render() {
    const lista = postulaciones.filter(p => (!filtro || filtro === p.faseActual) && (!resultado || resultado === p.estadoPostulacion));
    barra.querySelector('[data-conteo]').textContent = `${lista.length} postulaciones`;
    contenedor.replaceChildren();
    if (!lista.length) { contenedor.textContent = postulaciones.length ? 'No hay postulaciones con estos filtros.' : 'Todavía no te has postulado. Busca una oferta para comenzar.'; return; }
    for (const p of lista) {
      const card = document.createElement('article'); card.className = 'seleccion-candidato'; card.dataset.postulacion = p.idPostulacion;
      card.innerHTML = `<div class="seleccion-cabecera"><div><h2>${C.h(p.propuestaTitulo)}</h2><small>Inscripción: ${C.h(C.fecha(p.fechaPostulacion))}</small></div>${C.resultado(p)}</div>
        ${C.progreso(p)}${C.historial(p,false)}${C.documentos(p)}
        ${p.estadoPostulacion === 'Pendiente' && p.documentos.length < 5 ? `<form class="seleccion-form"><label>Adjuntar hoja de vida o documento PDF<input class="form-control" type="file" name="archivo" accept="application/pdf,.pdf" required></label><small>PDF de hasta 5 MB. Máximo 5 archivos por postulación. Solo tú y la empresa de la oferta pueden descargarlos.</small><br><button class="btn btn-outline-primary" type="submit">Enviar documento</button><p class="seleccion-mensaje" role="status"></p></form>` : ''}`;
      const form = card.querySelector('form');
      if (form) form.onsubmit = async e => {
        e.preventDefault();
        const file = form.elements.archivo.files[0]; const msg = form.querySelector('[role="status"]');
        if (!file || file.size > 5*1024*1024 || !file.name.toLowerCase().endsWith('.pdf')) { msg.textContent = 'Selecciona un PDF de hasta 5 MB.'; return; }
        const btn = form.querySelector('button'); btn.disabled = true; msg.textContent = 'Enviando documento…';
        try {
          const actualizado = await C.api(`/api/postulaciones/${encodeURIComponent(p.idPostulacion)}/documentos`, {method:'POST',body:new FormData(form)});
          postulaciones = postulaciones.map(item => item.idPostulacion === p.idPostulacion ? actualizado : item);
          render();
          const nueva = [...contenedor.children].find(el => el.dataset.postulacion === p.idPostulacion);
          if (nueva) {
            const aviso = document.createElement('p'); aviso.role = 'status'; aviso.textContent = 'Documento enviado. La empresa recibirá una notificación.'; nueva.append(aviso);
          }
        } catch(error) { msg.textContent = error.message; }
        finally { btn.disabled = false; }
      };
      contenedor.append(card);
    }
  }
  cargar();
})();
