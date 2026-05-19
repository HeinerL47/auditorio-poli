/* Bloqueo inmediato en <head> (bfcache / Adelante sin sesion). v3 */
(function () {
  if (sessionStorage.getItem('auditorio_salio') === '1') {
    location.replace('/login?expired');
    return;
  }
  if (localStorage.getItem('auditorio_logout_ts')) {
    location.replace('/login?expired');
  }
})();
