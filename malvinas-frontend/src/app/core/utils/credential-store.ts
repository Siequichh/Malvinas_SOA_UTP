const APP_PASS  = 'malvinas-soa-dispatch-2024';
const SALT      = new TextEncoder().encode('malvinas-credential-salt');
const STORE_KEY = 'malvinas_saved_creds';

async function deriveKey(): Promise<CryptoKey> {
  const raw = await crypto.subtle.importKey(
    'raw', new TextEncoder().encode(APP_PASS), 'PBKDF2', false, ['deriveKey']
  );
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt: SALT, iterations: 100_000, hash: 'SHA-256' },
    raw,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt']
  );
}

async function encrypt(text: string, key: CryptoKey): Promise<string> {
  const iv        = crypto.getRandomValues(new Uint8Array(12));
  const encrypted = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, new TextEncoder().encode(text));
  const buf       = new Uint8Array(12 + encrypted.byteLength);
  buf.set(iv);
  buf.set(new Uint8Array(encrypted), 12);
  return btoa(String.fromCharCode(...buf));
}

async function decrypt(b64: string, key: CryptoKey): Promise<string> {
  const buf       = Uint8Array.from(atob(b64), c => c.charCodeAt(0));
  const decrypted = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: buf.slice(0, 12) }, key, buf.slice(12));
  return new TextDecoder().decode(decrypted);
}

export async function saveCredentials(dni: string, password: string): Promise<void> {
  const key     = await deriveKey();
  const payload = JSON.stringify({ dni, password });
  localStorage.setItem(STORE_KEY, await encrypt(payload, key));
}

export async function loadCredentials(): Promise<{ dni: string; password: string } | null> {
  const stored = localStorage.getItem(STORE_KEY);
  if (!stored) return null;
  try {
    const key  = await deriveKey();
    const json = await decrypt(stored, key);
    return JSON.parse(json);
  } catch {
    localStorage.removeItem(STORE_KEY);
    return null;
  }
}

export function clearCredentials(): void {
  localStorage.removeItem(STORE_KEY);
}
