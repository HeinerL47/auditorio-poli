/* Ejecutar en <head> antes de pintar la pagina (evita flash al usar Adelante). */
(function () {
  if (sessionStorage.getItem('auditorio_salio') === '1') {
    location.replace('/login?expired');
  }
})();
