import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const sessionCookieName = "walletlah_backend_session";

export function backendBaseUrl() {
  const value = process.env.WALLETLAH_API_BASE_URL?.replace(/\/$/, "");
  if (!value) {
    throw new Error("WALLETLAH_API_BASE_URL is required.");
  }
  return value;
}

export async function backendFetch(path: string, init: RequestInit = {}) {
  try {
    const cookieStore = await cookies();
    const sessionId = cookieStore.get(sessionCookieName)?.value;
    const headers = new Headers(init.headers);

    if (sessionId) {
      headers.set("Cookie", `JSESSIONID=${sessionId}`);
    }
    if (init.body && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }

    return await fetch(`${backendBaseUrl()}${path}`, {
      ...init,
      headers,
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { message: "WalletLah backend is unavailable. Check WALLETLAH_API_BASE_URL and the backend service." },
      { status: 503 }
    );
  }
}

export async function proxyResponse(response: Response) {
  const contentType = response.headers.get("content-type") ?? "application/json";
  const body = await response.text();
  return new NextResponse(body, {
    status: response.status,
    headers: {
      "content-type": contentType,
    },
  });
}

export async function setBackendSessionFrom(response: Response) {
  const setCookie = response.headers.get("set-cookie");
  const match = setCookie?.match(/JSESSIONID=([^;]+)/);
  if (!match) {
    return;
  }

  const cookieStore = await cookies();
  cookieStore.set(sessionCookieName, match[1], {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24 * 7,
  });
}

export async function clearBackendSession() {
  const cookieStore = await cookies();
  cookieStore.delete(sessionCookieName);
}
