/**
 * Seguridad de sesion en la pagina de login (sin alterar el HTML visual).
 */
(function () {
  'use strict';

  var CLAVE_SALIO = 'auditorio_salio';
  var CLAVE_LOGOUT = 'auditorio_logout_ts';

  function marcarSalida() {
    try {
      sessionStorage.setItem(CLAVE_SALIO, '1');
      localStorage.setItem(CLAVE_LOGOUT, String(Date.now()));
    } catch (e) { /* ignorar */ }
  }

  function limpiarSalida() {
    try {
      sessionStorage.removeItem(CLAVE_SALIO);
      localStorage.removeItem(CLAVE_LOGOUT);
    } catch (e) { /* ignorar */ }
  }

  if (location.search.indexOf('logout') >= 0 || location.search.indexOf('expired') >= 0) {
    marcarSalida();
  }

  window.addEventListener('pageshow', function (event) {
    if (!event.persisted) return;
    marcarSalida();
    fetch('/logout', { method: 'POST', credentials: 'same-origin', cache: 'no-store' })
      .finally(function () { location.reload(); });
  });

  var form = document.getElementById('formLogin');
  if (form) {
    form.addEventListener('submit', function () {
      limpiarSalida();
    });
  }

  if (window.history && window.history.replaceState) {
    window.history.replaceState(null, '', location.pathname + location.search);
  }
})();
