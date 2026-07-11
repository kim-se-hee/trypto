/**
 * PKCE(Proof Key for Code Exchange) 유틸.
 * 인가 코드가 프론트를 잠깐 거치므로 code_verifier/code_challenge 로 코드 탈취를 막는다.
 * (plan.md: 프론트가 인가를 시작, client_secret 은 백엔드에만)
 */

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function randomBytes(length: number): Uint8Array {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return bytes;
}

/** code_verifier: 43~128자 base64url 무작위 문자열 (32바이트 → 43자) */
export function generateCodeVerifier(): string {
  return base64UrlEncode(randomBytes(32));
}

/** code_challenge: code_verifier 를 SHA-256 해시 후 base64url */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const data = new TextEncoder().encode(verifier);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return base64UrlEncode(new Uint8Array(digest));
}

/** CSRF 방지용 state 값 */
export function generateState(): string {
  return base64UrlEncode(randomBytes(16));
}
