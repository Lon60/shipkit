import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { env } from '@/env';

export async function GET(request: NextRequest) {
  const headers = request.headers;
  const originHeader = headers.get('x-forwarded-host') ?? headers.get('host');
  const proto = headers.get('x-forwarded-proto') ?? 'http';

  const internalHostPattern = /(gateway-api|localhost:8080)(?![\w-])/i;

  const graphqlUrl = env.GRAPHQL_URL && !internalHostPattern.test(env.GRAPHQL_URL)
    ? env.GRAPHQL_URL
    : `${proto}://${originHeader}/graphql`;

  return NextResponse.json({
    graphqlUrl,
    appName: env.NEXT_PUBLIC_APP_NAME,
  });
} 