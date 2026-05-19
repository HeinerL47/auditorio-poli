/**
 * Proteccion contra bfcache (boton Atras/Adelante del navegador).
 */
(function () {
  'use strict';

  var CLAVE_SALIO = 'auditorio_salio';

  function irALogin() {
    window.location.replace('/login?expired');
  }

  function marcarSalida() {
    try {
      sessionStorage.setItem(CLAVE_SALIO, '1');
    } catch (e) { /* ignorar */ }
  }

  function limpiarSalida() {
    try {
      sessionStorage.removeItem(CLAVE_SALIO);
    } catch (e) { /* ignorar */ }
  }

  function cerrarSesionEnServidor() {
    return fetch('/logout', {
      method: 'POST',
      credentials: 'same-origin',
      cache: 'no-store'
    }).catch(function () { /* ignorar */ });
  }

  function verificarSesion() {
    if (sessionStorage.getItem(CLAVE_SALIO) === '1') {
      cerrarSesionEnServidor().finally(irALogin);
      return;
    }

    fetch('/api/session', {
      method: 'GET',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { Accept: 'application/json', 'Cache-Control': 'no-cache' }
    })
      .then(function (r) {
        if (!r.ok) {
          marcarSalida();
          irALogin();
          return null;
        }
        return r.json();
      })
      .then(function (data) {
        if (!data || data.authenticated !== true) {
          marcarSalida();
          irALogin();
          return;
        }
        limpiarSalida();
      })
      .catch(function () {
        marcarSalida();
        irALogin();
      });
  }

  /* Desactiva bfcache en Chrome/Edge/Firefox */
  window.addEventListener('unload', function () {});

  window.addEventListener('pageshow', function (e) {
    if (e.persisted) {
      if (sessionStorage.getItem(CLAVE_SALIO) === '1') {
        irALogin();
        return;
      }
      window.location.reload();
      return;
    }
    verificarSesion();
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', verificarSesion);
  } else {
    verificarSesion();
  }
})();
