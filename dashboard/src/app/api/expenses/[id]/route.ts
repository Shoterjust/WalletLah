import { backendFetch, proxyResponse } from "../../_lib/backend";

type RouteContext = {
  params: Promise<{ id: string }>;
};

export async function PATCH(request: Request, context: RouteContext) {
  const { id } = await context.params;
  const body = await request.text();
  const response = await backendFetch(`/api/expenses/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body,
  });
  return proxyResponse(response);
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { id } = await context.params;
  const response = await backendFetch(`/api/expenses/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
  return proxyResponse(response);
}
