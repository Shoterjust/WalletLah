import { backendFetch, proxyResponse } from "../_lib/backend";

export async function GET() {
  const response = await backendFetch("/api/recurring-expenses");
  return proxyResponse(response);
}

export async function POST(request: Request) {
  const body = await request.text();
  const response = await backendFetch("/api/recurring-expenses", {
    method: "POST",
    body,
  });
  return proxyResponse(response);
}
