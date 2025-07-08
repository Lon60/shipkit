'use client';

import { forwardRef, useEffect, useState, useCallback } from 'react';
import { CodeEditor } from './code-editor';
import { cn } from '@/lib/utils';

interface FormCodeEditorProps {
  value?: string;
  onChange?: (event: { target: { value: string } }) => void;
  onBlur?: (event: { target: { name?: string; value: string } }) => void;
  name?: string;
  placeholder?: string;
  height?: string | number;
  language?: string;
  className?: string;
  error?: boolean;
}

export const FormCodeEditor = forwardRef<HTMLDivElement, FormCodeEditorProps>(
  ({ value, onChange, placeholder, height = 300, language = 'yaml', className, error }, ref) => {
    const [internalValue, setInternalValue] = useState(value ?? '');

    useEffect(() => {
      setInternalValue(value ?? '');
    }, [value]);

    const handleChange = useCallback((newValue: string | undefined) => {
      const stringValue = newValue ?? '';
      setInternalValue(stringValue);
      onChange?.({ target: { value: stringValue } });
    }, [onChange]);

    return (
      <div ref={ref}>
        <CodeEditor
          value={internalValue}
          onChange={handleChange}
          placeholder={placeholder}
          height={height}
          language={language}
          className={cn(
            error && 'border-destructive',
            className
          )}
          theme="vs-dark"
        />
      </div>
    );
  }
);

FormCodeEditor.displayName = 'FormCodeEditor'; 