'use strict';
(() => {
  const header = document.querySelector('.editorial-page .header_section');
  if (!header) return;
  const menu = header.querySelector('.navbar-collapse');
  const toggle = header.querySelector('.navbar-toggler');
  const desktop = window.matchMedia('(min-width: 992px)');

  function medir() {
    // Abrir el menú móvil no debe desplazar el contenido de la página.
    if (!menu.classList.contains('show'))
      document.body.style.setProperty('--nav-h', `${header.offsetHeight}px`);
  }
  function cerrar() {
    menu.classList.remove('show');
    toggle.setAttribute('aria-expanded', 'false');
    medir();
  }
  toggle.addEventListener('click', () => {
    const abierto = menu.classList.toggle('show');
    toggle.setAttribute('aria-expanded', String(abierto));
    if (!abierto) medir();
  });
  menu.addEventListener('click', e => { if (e.target.closest('a')) cerrar(); });
  header.addEventListener('keydown', e => {
    if (e.key === 'Escape' && menu.classList.contains('show')) { cerrar(); toggle.focus(); }
  });
  const scroll = () => header.classList.toggle('scrolled', window.scrollY > 10);
  window.addEventListener('scroll', scroll, {passive: true});
  window.addEventListener('resize', () => { if (desktop.matches) cerrar(); medir(); });
  window.addEventListener('load', medir);
  if ('ResizeObserver' in window) new ResizeObserver(medir).observe(header);
  medir(); scroll();
})();
