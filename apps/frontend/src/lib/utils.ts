import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"
import { ErrorCode, type ErrorObject } from "./graphql"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export interface DomainError {
  code: ErrorCode;
  message: string;
  showContinueAnyway: boolean;
}

export function parseDomainError(error: { graphQLErrors?: ReadonlyArray<{ extensions?: { error?: ErrorObject } }> }): DomainError | null {
  const errorObject = error.graphQLErrors?.[0]?.extensions?.error;
  
  if (!errorObject?.code) {
    return null;
  }
  
  return {
    code: errorObject.code,
    message: errorObject.message,
    showContinueAnyway: errorObject.code === ErrorCode.DOMAIN_VALIDATION_ERROR,
  };
}
