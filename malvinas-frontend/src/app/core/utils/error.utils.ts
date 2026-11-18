// Render free tier: un servicio dormido responde 502/503/504 (o nada) durante ~1 min mientras arranca.
const WAKING = [0, 502, 503, 504];

export function parseLoginError(err: any): string {
  const status = err?.status;
  if (status === 401 || status === 403 || status === 404) return 'DNI o contraseña incorrectos.';
  if (status === 400) return err?.error?.message || 'Revisa el DNI (8 dígitos) y la contraseña.';
  if (status === 429) return 'Demasiados intentos. Espera un minuto antes de reintentar.';
  if (WAKING.includes(status)) return 'El servidor está iniciando. Espera un minuto y vuelve a intentar.';
  return 'No se pudo iniciar sesión. Intenta nuevamente.';
}

export function parseApiError(err: any): string {
  const status = err?.status;
  // El backend devuelve el motivo real (regla de negocio o campo invalido): mostrarlo.
  if (status === 400) return err?.error?.message || 'Completa todos los campos requeridos.';
  if (status === 401 || status === 403) return 'No tienes permiso para realizar esta acción.';
  if (status === 404) return 'El recurso solicitado no existe.';
  if (status === 409) return err?.error?.message || 'Conflicto: el registro ya existe.';
  if (status === 429) return 'Demasiadas solicitudes. Espera un minuto.';
  if (WAKING.includes(status)) return 'El servidor está iniciando. Intenta en un momento.';
  return 'Ocurrió un error inesperado. Intenta nuevamente.';
}
