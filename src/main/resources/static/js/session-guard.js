/**
 * Impide ver paginas protegidas con boton "Adelante" sin sesion activa (bfcache del navegador).
 */
(function () {
  'use strict';

  function irALogin() {
    window.location.replace('/login?expired');
  }

  function verificarSesion() {
    return fetch('/api/session', {
      method: 'GET',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { Accept: 'application/json' }
    })
      .then(function (r) {
        if (!r.ok) {
          irALogin();
          return null;
        }
        return r.json();
      })
      .then(function (data) {
        if (data && data.authenticated === false) {
          irALogin();
        }
      })
      .catch(irALogin);
  }

  function esNavegacionAtrasAdelante() {
    var entries = window.performance && performance.getEntriesByType
        ? performance.getEntriesByType('navigation') : [];
    if (entries.length > 0 && entries[0].type === 'back_forward') {
      return true;
    }
    return window.performance && performance.navigation
        && performance.navigation.type === 2;
  }

  window.addEventListener('pageshow', function (e) {
    if (e.persisted || esNavegacionAtrasAdelante()) {
      verificarSesion();
    }
  });

  if (esNavegacionAtrasAdelante()) {
    verificarSesion();
  }
})();
