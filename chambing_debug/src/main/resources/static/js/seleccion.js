'use strict';
window.Chambing = (() => {
  const fases = [
    ['INSCRIPCION', 'Inscripción'], ['REVISION_DOCUMENTAL', 'Revisión documental'],
    ['POTENCIAL_CANDIDATO', 'Potencial candidato'], ['SELECCIONADO', 'Seleccionado']
  ];
  const h = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const nombreFase = clave => fases.find(f => f[0] === clave)?.[1] || 'Inscripción';
  const fecha = v => {
    if (!v) return 'Fecha no disponible';
    const d = Array.isArray(v) ? new Date(v[0], v[1]-1, v[2], v[3] || 0, v[4] || 0) : new Date(v);
    return isNaN(d) ? 'Fecha no disponible' : d.toLocaleString('es-CO');
  };
  async function api(url, options = {}) {
    const res = await fetch(url, {...options, headers: {Accept:'application/json', ...options.headers}});
    if (res.redirected || res.headers.get('content-type')?.includes('text/html'))
      throw new Error('Tu sesión venció. Inicia sesión de nuevo.');
    const text = await res.text();
    let data;
    try { data = text ? JSON.parse(text) : null; } catch (_) { data = text; }
    if (!res.ok) throw new Error(data?.message || data?.detail || (typeof data === 'string' ? data : `No se pudo completar la operación (${res.status})`));
    return data;
  }
  const put = (url, body) => api(url, {method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
  function progreso(p) {
    const actual = Math.max(0, fases.findIndex(f => f[0] === p.faseActual));
    return `<ol class="seleccion-fases" aria-label="Fase actual: ${h(nombreFase(p.faseActual))}">${fases.map(([clave,nombre],i) =>
      `<li class="${i < actual ? 'completa' : i === actual ? 'actual' : ''}" ${i === actual ? 'aria-current="step"' : ''}><span>${i+1}</span>${nombre}</li>`).join('')}</ol>`;
  }
  function historial(p, empresa) {
    return `<details class="seleccion-historial"><summary>Historial del proceso (${p.historial.length})</summary>${p.historial.length
      ? `<ol>${p.historial.map(c => `<li><strong>${h(nombreFase(c.fase))} · ${h(c.resultado)}</strong><small>${h(fecha(c.fecha))}</small>
          ${c.observacion ? `<p>${h(c.observacion)}</p>` : ''}${empresa && c.notaInterna ? `<p class="nota-interna"><strong>Nota interna:</strong> ${h(c.notaInterna)}</p>` : ''}</li>`).join('')}</ol>`
      : '<p>Registro anterior al seguimiento por fases. No hay cambios históricos registrados.</p>'}</details>`;
  }
  function documentos(p) {
    return `<div class="seleccion-documentos"><h3>Documentos</h3>${p.documentos.length ? `<ul>${p.documentos.map(d =>
      `<li><a href="/api/postulaciones/${encodeURIComponent(p.idPostulacion)}/documentos/${encodeURIComponent(d.id)}">${h(d.nombre)}</a> <small>(${Math.ceil(d.tamano/1024)} KB) · ${h(fecha(d.fecha))}</small></li>`).join('')}</ul>` : '<p class="text-muted">Todavía no se han adjuntado documentos.</p>'}</div>`;
  }
  function resultado(p) {
    return `<span class="seleccion-resultado ${p.estadoPostulacion === 'Rechazado' ? 'rechazado' : p.estadoPostulacion === 'Aprobado' ? 'seleccionado' : ''}">${p.estadoPostulacion === 'Aprobado' ? 'Seleccionado' : p.estadoPostulacion === 'Rechazado' ? 'Rechazado' : 'En proceso'}</span>`;
  }
  function destacar() {
    const id = new URLSearchParams(location.search).get('postulacion');
    const el = [...document.querySelectorAll('[data-postulacion]')].find(el => el.dataset.postulacion === id);
    if (el) { el.classList.add('seleccion-destacada'); el.scrollIntoView({behavior:'smooth', block:'center'}); }
  }
  return {fases, h, nombreFase, fecha, api, put, progreso, historial, documentos, resultado, destacar};
})();
