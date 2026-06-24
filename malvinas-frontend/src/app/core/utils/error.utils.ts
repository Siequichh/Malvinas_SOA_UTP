export function parseLoginError(err: any): string {
  const status = err?.status;
  if (status === 401 || status === 403 || status === 404) return 'DNI o contraseña incorrectos.';
  if (status === 0) return 'No se pudo conectar al servidor. Verifica tu conexión.';
  return 'Error al iniciar sesión. Intenta nuevamente.';
}

export function parseApiError(err: any): string {
  const status = err?.status;
  if (status === 400) return err?.error?.message || 'Datos inválidos. Revisa el formulario.';
  if (status === 401 || status === 403) return 'No tienes permiso para realizar esta acción.';
  if (status === 404) return 'El recurso solicitado no existe.';
  if (status === 409) return err?.error?.message || 'Conflicto: el registro ya existe.';
  if (status === 0) return 'No se pudo conectar al servidor.';
  return 'Ocurrió un error inesperado. Intenta nuevamente.';
}
