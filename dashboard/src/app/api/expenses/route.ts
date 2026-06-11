import { backendFetch, proxyResponse } from "../_lib/backend";

export async function GET(request: Request) {
  const url = new URL(request.url);
  const response = await backendFetch(`/api/expenses${url.search}`);
  return proxyResponse(response);
}

export async function POST(request: Request) {
  const body = await request.text();
  const response = await backendFetch("/api/expenses", {
    method: "POST",
    body,
  });
  return proxyResponse(response);
}
