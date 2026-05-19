/**
 * Sesion: bloqueo bfcache, sincronizacion entre pestanas al cerrar sesion.
 * No verifica en cada foco (evita expulsar por multiples pestanas activas).
 */
(function () {
  'use strict';

  var CLAVE_SALIO = 'auditorio_salio';
  var CLAVE_LOGOUT = 'auditorio_logout_ts';

  function irALogin() {
    window.location.replace('/login?expired');
  }

  function sesionCerradaLocalmente() {
    return sessionStorage.getItem(CLAVE_SALIO) === '1'
        || localStorage.getItem(CLAVE_LOGOUT) != null;
  }

  function limpiarMarcasSalida() {
    try {
      sessionStorage.removeItem(CLAVE_SALIO);
      localStorage.removeItem(CLAVE_LOGOUT);
    } catch (e) { /* ignorar */ }
  }

  /* Cerrar sesion en todas las pestanas cuando una hace logout */
  window.addEventListener('storage', function (e) {
    if (e.key === CLAVE_LOGOUT && e.newValue) {
      try { sessionStorage.setItem(CLAVE_SALIO, '1'); } catch (err) {}
      irALogin();
    }
  });

  if (sesionCerradaLocalmente()) {
    irALogin();
    return;
  }

  /* Impide bfcache en la mayoria de navegadores */
  window.addEventListener('unload', function () {});

  window.addEventListener('pageshow', function (e) {
    if (sesionCerradaLocalmente()) {
      irALogin();
      return;
    }
    if (e.persisted) {
      window.location.reload();
    }
  });

  /* Tras carga normal, quitar marcas si el servidor acepta la sesion */
  fetch('/api/session', {
    method: 'GET',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: { Accept: 'application/json' }
  })
    .then(function (r) {
      if (r.ok) {
        limpiarMarcasSalida();
      }
    })
    .catch(function () { /* ignorar en paginas publicas */ });

})();
