'use strict';
window.Ubicaciones = (() => {
  async function api(url, options = {}) {
    const res = await fetch(url, {headers:{Accept:'application/json'}, ...options});
    if (res.redirected || res.headers.get('content-type')?.includes('text/html')) throw new Error('Inicia sesión de nuevo.');
    const texto = await res.text();
    let datos; try { datos = texto ? JSON.parse(texto) : null; } catch (_) { datos = texto; }
    if (!res.ok) throw new Error(datos?.message || (typeof datos === 'string' && datos) || 'No se pudo cargar el catálogo de ubicaciones.');
    return datos;
  }
  const clave = n => String(n || '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').trim().toLowerCase().replace(/\s+/g,' ').replace(/^bogota d\.?c\.?$/,'bogota');
  function crear(dep, ciudad, {admin = false, requerido = false} = {}) {
    const base = admin ? '/admin' : '/api/catalogos';
    const opciones = admin ? '/opciones' : '';
    let version = 0, ocupado = true, error = '', guardada = {};
    const aviso = document.createElement('div'); aviso.className = 'small mt-2';
    const mensaje = document.createElement('span'); mensaje.setAttribute('role','status');
    const reintentar = document.createElement('button'); reintentar.type = 'button';
    reintentar.className = 'btn btn-sm btn-outline-secondary ml-2'; reintentar.textContent = 'Reintentar'; reintentar.hidden = true;
    aviso.append(mensaje,reintentar); ciudad.after(aviso);
    function informar(texto, fallo = false) { mensaje.textContent = texto; aviso.classList.toggle('text-danger',fallo); reintentar.hidden = !fallo; }
    function llenar(select, lista, placeholder, actualId, actualNombre) {
      select.replaceChildren(new Option(placeholder,''));
      for (const item of lista) {
        const opt = new Option(item.nombre + (item.activo ? '' : ' (inactivo)'),item.id);
        opt.dataset.nombre = item.nombre; select.append(opt);
      }
      let actual = lista.find(item => actualId ? item.id === actualId : clave(item.nombre) === clave(actualNombre));
      if (actual) select.value = actual.id;
      else if (actualId || actualNombre) {
        // Conservar la dirección actual si su catálogo fue desactivado; no ofrecerla para nuevas direcciones.
        const opt = new Option(`${actualNombre || 'Ubicación guardada'} (guardada, no disponible)`,actualId || actualNombre);
        opt.dataset.nombre = actualNombre || ''; opt.disabled = true; opt.selected = true; select.append(opt);
      }
    }
    async function cargarCiudades(dir = {}, token = ++version) {
      ocupado = true; error = ''; ciudad.disabled = true;
      ciudad.replaceChildren(new Option(dep.value ? 'Cargando ciudades…' : 'Selecciona un departamento',''));
      if (!dep.value) { ocupado = false; informar(''); return; }
      informar('Cargando ciudades…');
      try {
        const lista = await api(`${base}/ciudades${opciones}?departamentoId=${encodeURIComponent(dep.value)}`);
        if (token !== version) return;
        llenar(ciudad,lista,'Selecciona una ciudad',dir.ciudadId,dir.ciudad);
        ciudad.disabled = false; informar(lista.length || dir.ciudad ? '' : 'Este departamento no tiene ciudades disponibles.');
      } catch (e) {
        if (token !== version) return;
        error = e.message; informar(error,true);
      } finally { if (token === version) ocupado = false; }
    }
    async function cargar(dir = {}) {
      guardada = dir; const token = ++version;
      ocupado = true; error = ''; dep.disabled = true; ciudad.disabled = true;
      dep.replaceChildren(new Option('Cargando departamentos…',''));
      ciudad.replaceChildren(new Option('Selecciona un departamento',''));
      informar('Cargando ubicaciones…');
      try {
        const lista = await api(`${base}/departamentos${opciones}`);
        if (token !== version) return;
        llenar(dep,lista,'Selecciona un departamento',dir.departamentoId,dir.departamento);
        dep.disabled = false;
        await cargarCiudades(dir,token);
        if (!lista.length && !dir.departamento) informar('No hay departamentos disponibles. El administrador debe registrarlos.');
      } catch (e) {
        if (token === version) { error = e.message; ocupado = false; informar(error,true); }
      }
    }
    dep.addEventListener('change',() => cargarCiudades());
    reintentar.onclick = () => cargar(guardada);
    function datos() {
      if (ocupado) throw new Error('Espera a que termine de cargar el catálogo de ubicaciones.');
      if (error) throw new Error(error);
      if ((requerido || dep.value || ciudad.value) && (!dep.value || !ciudad.value)) throw new Error('Selecciona un departamento y una ciudad.');
      const nombre = select => select.selectedOptions[0]?.dataset.nombre || '';
      return {departamentoId: dep.value || null, ciudadId: ciudad.value || null, departamento:nombre(dep), ciudad:nombre(ciudad)};
    }
    return {cargar,datos};
  }
  return {api,crear,clave};
})();
