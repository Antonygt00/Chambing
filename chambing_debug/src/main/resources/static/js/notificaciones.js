'use strict';
(() => {
  const C = window.Chambing;
  const boton = document.createElement('button');
  boton.className = 'notificaciones-boton'; boton.type = 'button';
  boton.setAttribute('aria-expanded','false'); boton.setAttribute('aria-controls','notificaciones-panel');
  boton.textContent = 'Notificaciones';
  const panel = document.createElement('section');
  panel.id = 'notificaciones-panel'; panel.className = 'notificaciones-panel'; panel.hidden = true;
  panel.setAttribute('aria-label','Notificaciones');
  panel.innerHTML = `<header><h2>Notificaciones</h2><button type="button" class="btn btn-sm btn-light" data-cerrar aria-label="Cerrar notificaciones">×</button></header>
    <div class="notificaciones-acciones"><button class="btn btn-sm btn-outline-primary" data-todas>Marcar todas como leídas</button><button class="btn btn-sm btn-light" data-recargar>Actualizar</button></div>
    <p role="status" data-estado></p><ul class="notificaciones-lista"></ul><div class="notificaciones-paginacion"><button class="btn btn-sm btn-light" data-anterior>Anterior</button><span></span><button class="btn btn-sm btn-light" data-siguiente>Siguiente</button></div>`;
  document.body.append(boton, panel);
  let pagina = 0, paginas = 1, ocupado = false;
  const estado = panel.querySelector('[data-estado]');
  async function contador() {
    try {
      const {total} = await C.api('/api/notificaciones/pendientes');
      boton.textContent = total ? `Notificaciones (${total})` : 'Notificaciones';
      boton.setAttribute('aria-label', `${total} notificaciones sin leer`);
    } catch (_) { /* La bandeja muestra el error al abrirla; no interrumpe la página. */ }
  }
  async function cargar() {
    if (ocupado) return;
    ocupado = true; estado.className = ''; estado.textContent = 'Cargando…';
    try {
      const data = await C.api(`/api/notificaciones?page=${pagina}&size=15`);
      paginas = Math.max(1, data.totalPages);
      const lista = panel.querySelector('ul'); lista.replaceChildren();
      for (const n of data.content) {
        const li = document.createElement('li'); li.className = n.leida ? '' : 'sin-leer';
        const a = document.createElement('a'); a.textContent = n.mensaje;
        // Solo destinos locales conocidos, aunque un documento antiguo contenga otra URL.
        a.href = /^\/(proposals-states|proposals-answer)\?/.test(n.enlace) ? n.enlace : '/proposals-states';
        a.addEventListener('click', async e => {
          e.preventDefault();
          try { await C.put(`/api/notificaciones/${encodeURIComponent(n.id)}/leida`, {}); }
          catch (error) { estado.textContent = error.message; return; }
          location.assign(a.href);
        });
        const fecha = document.createElement('small'); fecha.textContent = C.fecha(n.fecha);
        li.append(a,fecha);
        if (!n.leida) {
          const leer = document.createElement('button'); leer.className = 'btn btn-sm btn-light'; leer.textContent = 'Marcar como leída';
          leer.onclick = async () => {
            leer.disabled = true;
            try { await C.put(`/api/notificaciones/${encodeURIComponent(n.id)}/leida`, {}); await contador(); await cargar(); }
            catch(error) { estado.textContent = error.message; leer.disabled = false; }
          };
          li.append(leer);
        }
        lista.append(li);
      }
      estado.textContent = data.content.length ? '' : 'No tienes notificaciones todavía.';
      panel.querySelector('[data-anterior]').disabled = pagina === 0;
      panel.querySelector('[data-siguiente]').disabled = pagina >= paginas-1;
      panel.querySelector('.notificaciones-paginacion span').textContent = `${pagina+1} / ${paginas}`;
    } catch(error) { estado.className = 'notificaciones-error'; estado.textContent = error.message; }
    finally { ocupado = false; }
  }
  function cerrar() { panel.hidden = true; boton.setAttribute('aria-expanded','false'); boton.focus(); }
  boton.onclick = () => {
    panel.hidden = !panel.hidden; boton.setAttribute('aria-expanded',String(!panel.hidden));
    if (!panel.hidden) { cargar(); panel.querySelector('[data-cerrar]').focus(); }
  };
  panel.querySelector('[data-cerrar]').onclick = cerrar;
  panel.querySelector('[data-recargar]').onclick = () => { contador(); cargar(); };
  panel.querySelector('[data-anterior]').onclick = () => { if (pagina>0) { pagina--; cargar(); } };
  panel.querySelector('[data-siguiente]').onclick = () => { if (pagina<paginas-1) { pagina++; cargar(); } };
  panel.querySelector('[data-todas]').onclick = async e => {
    e.target.disabled = true;
    try { await C.put('/api/notificaciones/leidas',{}); await contador(); await cargar(); }
    catch(error) { estado.textContent = error.message; }
    finally { e.target.disabled = false; }
  };
  document.addEventListener('keydown',e => { if (e.key === 'Escape' && !panel.hidden) cerrar(); });
  contador();
  setInterval(() => { if (!document.hidden) { contador(); if (!panel.hidden) cargar(); } },15000);
})();
