import { backendFetch, proxyResponse } from "../../_lib/backend";

export async function GET() {
  const response = await backendFetch("/api/dashboard/summary");
  return proxyResponse(response);
}
