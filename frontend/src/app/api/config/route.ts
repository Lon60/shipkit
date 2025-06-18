import { NextResponse } from 'next/server';
import { env } from '@/env';

export async function GET() {
  return NextResponse.json({
    graphqlUrl: env.GRAPHQL_URL,
    appName: env.NEXT_PUBLIC_APP_NAME,
  });
} 