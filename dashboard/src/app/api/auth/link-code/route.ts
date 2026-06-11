import { backendFetch, proxyResponse, setBackendSessionFrom } from "../../_lib/backend";

export async function POST(request: Request) {
  const body = await request.text();
  const response = await backendFetch("/api/dashboard/auth/link-code", {
    method: "POST",
    body,
  });

  if (response.ok) {
    await setBackendSessionFrom(response);
  }

  return proxyResponse(response);
}
