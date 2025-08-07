"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { getPreviousPath, clearBackendStartingFlag } from "@/lib/startup";

export default function StartingPage() {
  const [status, setStatus] = useState<string>("Starting components...");
  const [attempts, setAttempts] = useState<number>(0);
  const controllerRef = useRef<AbortController | null>(null);

  const pollIntervalMs = 1500;

  const previousPath = useMemo(() => {
    if (typeof window === "undefined") return "/";
    return getPreviousPath() ?? "/";
  }, []);

  useEffect(() => {
    let stopped = false;

    async function ping() {
      if (stopped) return;
      controllerRef.current?.abort();
      controllerRef.current = new AbortController();
      const signal = controllerRef.current.signal;

      try {
        // Call the public health GraphQL query (permitAll)
        const res = await fetch("/api/graphql", {
          method: "POST",
          headers: { "content-type": "application/json" },
          cache: "no-store",
          body: JSON.stringify({ query: "query{ status { status adminInitialized domainInitialized } }" }),
          signal,
        });
        const code = res.status;
        setStatus(`Checking backend... (${code})`);
        if (res.ok) {
          const json = (await res.json()) as { data?: { status?: { status?: string } } };
          if (json?.data?.status?.status) {
            clearBackendStartingFlag();
            window.location.href = previousPath || "/";
            return;
          }
        }
      } catch {
        // ignore
      }
      setAttempts((a) => a + 1);
      window.setTimeout(() => void ping(), pollIntervalMs);
    }

    const t = window.setTimeout(() => void ping(), 300);
    return () => {
      stopped = true;
      window.clearTimeout(t);
      controllerRef.current?.abort();
    };
  }, [pollIntervalMs, previousPath]);

  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-6 py-10">
      <div className="w-full max-w-5xl grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12 items-center">
        {/* Left: Brand + Message */}
        <div className="space-y-6 text-center lg:text-left">
          <div className="space-y-2">
            <h1 className="text-4xl font-bold tracking-tight">Shipkit is getting ready</h1>
            <p className="text-muted-foreground">{status}</p>
          </div>
          <div className="rounded-lg border bg-card p-4 text-sm text-muted-foreground">
            We’re preparing the platform components. This usually takes a moment after startup.
            You’ll be redirected automatically when everything is ready.
          </div>
          <div className="text-xs text-muted-foreground">Checks attempted: {attempts}</div>
        </div>

        {/* Right: Illustration (hidden on mobile) */}
        <div className="hidden lg:block">
          <div className="relative">
            <div className="absolute -inset-6 rounded-2xl bg-gradient-to-tr from-primary/10 to-transparent blur-xl" />
            <div className="relative rounded-2xl border bg-card p-6 shadow-sm">
              <div className="grid grid-cols-3 gap-3">
                <div className="h-24 rounded-md bg-muted" />
                <div className="h-24 rounded-md bg-muted" />
                <div className="h-24 rounded-md bg-muted" />
                <div className="h-24 rounded-md bg-muted" />
                <div className="h-24 rounded-md bg-muted" />
                <div className="h-24 rounded-md bg-muted" />
              </div>
              <div className="mt-6 h-2 w-2/3 rounded bg-muted" />
              <div className="mt-2 h-2 w-1/2 rounded bg-muted" />
              <div className="mt-8 h-2 w-1/3 rounded bg-muted" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}


