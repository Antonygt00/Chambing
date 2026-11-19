'use strict';
(() => {
  const empresa = !!document.getElementById('formEmpresa');
  const form = document.getElementById(empresa ? 'formEmpresa' : 'formModificacion');
  const api = window.Ubicaciones.api;
  const campo = id => document.getElementById(id);
  const ubicacion = Ubicaciones.crear(campo('selectDepartamento'),campo('selectCiudad'),{requerido:empresa});
  let id;
  const aviso = document.createElement('p'); aviso.setAttribute('role','status'); form.append(aviso);
  const guardar = form.querySelector('button[type="submit"],button'); guardar.disabled = true;
  const reintentar = document.createElement('button'); reintentar.type='button'; reintentar.className='btn btn-outline-primary';
  reintentar.textContent='Reintentar carga del perfil'; reintentar.hidden=true; form.after(reintentar);
  async function cargar() {
    guardar.disabled=true; aviso.textContent='Cargando perfil…'; reintentar.hidden=true;
    try {
      const perfil = await api(empresa ? '/empresa/me' : '/usuario/me');
      id = empresa ? perfil.idEmpresa : perfil.idUsuario;
      if (empresa) {
        campo('inputNombreEmpresa').value=perfil.nombreEmpresa || '';
        campo('inputNIT').value=perfil.nit || '';
      } else {
        campo('inputNombre').value=perfil.nombre || ''; campo('inputApellido').value=perfil.apellido || '';
        campo('inputEdad').value=perfil.edad || '';
        if (perfil.genero && ![...campo('selectGenero').options].some(o=>o.value===perfil.genero)) campo('selectGenero').add(new Option(perfil.genero,perfil.genero));
        campo('selectGenero').value=perfil.genero || '';
      }
      campo('inputEmail').value=perfil.correo || perfil.email || '';
      campo('inputCalle').value=perfil.direccion?.calle || '';
      if (empresa) {
        if (campo('inputDescripcion')) campo('inputDescripcion').value = perfil.descripcion || '';
        if (campo('inputSector')) campo('inputSector').value = perfil.sector || '';
        if (campo('inputTamano')) campo('inputTamano').value = perfil.tamano || '';
        if (campo('inputWeb')) campo('inputWeb').value = perfil.sitioWeb || '';
        if (campo('inputLogo')) campo('inputLogo').value = perfil.logoUrl || '';
        if (campo('inputBeneficios')) campo('inputBeneficios').value = (perfil.beneficios || []).join(', ');
      } else if (campo('inputResumen')) {
        campo('inputResumen').value = perfil.resumen || '';
        campo('inputFoto').value = perfil.fotoUrl || '';
        campo('inputAnios').value = perfil.aniosExperiencia ?? '';
        campo('inputDisponible').checked = perfil.disponibleParaTrabajo !== false;
        campo('inputHabilidades').value = (perfil.habilidades || []).map(h => h.nombre).filter(Boolean).join(', ');
        campo('inputExperiencia').value = (perfil.experiencia || []).map(e => [e.cargo, e.empresa, e.anioInicio, e.anioFin].filter(v => v != null && v !== '').join(' | ')).join('\n');
        campo('inputEducacion').value = (perfil.educacion || []).map(e => [e.titulo, e.institucion, e.nivel, e.anioFin].filter(v => v != null && v !== '').join(' | ')).join('\n');
        campo('inputCertificaciones').value = (perfil.certificaciones || []).map(e => [e.nombre, e.entidad, e.anio].filter(v => v != null && v !== '').join(' | ')).join('\n');
      }
      await ubicacion.cargar(perfil.direccion || {});
      form.classList.remove('d-none');
      const incompleto = !empresa && perfil.perfilCompleto === false;
      aviso.textContent = incompleto ? 'Completa los campos obligatorios para ver las vacantes.' : '';
      guardar.disabled=false;
    } catch (error) {
      form.classList.remove('d-none'); aviso.textContent=error.message; reintentar.hidden=false;
    } finally { campo('loadingSpinner')?.classList.add('d-none'); }
  }
  form.addEventListener('submit',async e => {
    e.preventDefault(); guardar.disabled=true;
    try {
      const direccion={calle:campo('inputCalle').value.trim(),...ubicacion.datos()};
      const lineas = (id, map) => (campo(id)?.value || '').split('\n').map(l => l.trim()).filter(Boolean).map(l => map(l.split('|').map(p => p.trim())));
      const datos=empresa ? {
        nombreEmpresa:campo('inputNombreEmpresa').value.trim(),
        correo:campo('inputEmail').value.trim(),
        nit:Number(campo('inputNIT').value)||null,
        direccion,
        descripcion: campo('inputDescripcion')?.value.trim(),
        sector: campo('inputSector')?.value.trim(),
        tamano: campo('inputTamano')?.value,
        sitioWeb: campo('inputWeb')?.value.trim(),
        logoUrl: campo('inputLogo')?.value.trim(),
        beneficios: (campo('inputBeneficios')?.value || '').split(',').map(s => s.trim()).filter(Boolean)
      } : {
        nombre:campo('inputNombre').value.trim(),
        apellido:campo('inputApellido').value.trim(),
        edad:Number(campo('inputEdad').value)||null,
        genero:campo('selectGenero').value,
        direccion,
        resumen: campo('inputResumen')?.value.trim(),
        fotoUrl: campo('inputFoto')?.value.trim(),
        aniosExperiencia: campo('inputAnios')?.value === '' ? null : Number(campo('inputAnios').value),
        disponibleParaTrabajo: !!campo('inputDisponible')?.checked,
        habilidades: (campo('inputHabilidades')?.value || '').split(',').map(s => s.trim()).filter(Boolean).map(nombre => ({nombre})),
        experiencia: lineas('inputExperiencia', p => ({cargo:p[0], empresa:p[1], anioInicio:Number(p[2])||null, anioFin:Number(p[3])||null})),
        educacion: lineas('inputEducacion', p => ({titulo:p[0], institucion:p[1], nivel:p[2], anioFin:Number(p[3])||null})),
        certificaciones: lineas('inputCertificaciones', p => ({nombre:p[0], entidad:p[1], anio:Number(p[2])||null}))
      };
      if(campo('inputPassword').value)datos.contrasena=campo('inputPassword').value;
      aviso.textContent='Guardando…';
      const guardado = await api(`/${empresa?'empresa':'usuario'}/${encodeURIComponent(id)}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(datos)});
      campo('inputPassword').value='';
      if (!empresa && guardado && guardado.perfilCompleto) {
        aviso.textContent='Perfil listo. Entrando a las vacantes…';
        window.location.href='/user-dashboard';
        return;
      }
      aviso.textContent = !empresa
        ? 'Faltan datos obligatorios: resumen, años de experiencia, al menos una habilidad, una experiencia y una educación.'
        : 'Perfil actualizado correctamente.';
    } catch(error) {aviso.textContent=error.message;} finally {guardar.disabled=false;}
  });
  reintentar.onclick=cargar;
  cargar();
})();
